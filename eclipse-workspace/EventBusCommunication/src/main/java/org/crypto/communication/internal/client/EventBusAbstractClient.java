package org.crypto.communication.internal.client;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.acl.NotOwnerException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.net.EventBusNetworking;
import org.crypto.communication.internal.notifications.IMessageFailedNotification;
import org.crypto.communication.internal.notifications.IMessageSuccessNotification;
import org.crypto.communication.internal.notifications.INewClientNotification;
import org.crypto.communication.internal.notifications.NotificationService;
import org.crypto.communication.internal.utils.EventBusMessageUtils;
import org.crypto.communication.internal.utils.MembersManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
/**
 * 
 * @author asaf
 * @since 12/2018
 * @description - 
 * 		Event bus client template with minimal functionality to be interpreted by inheriting objects
 * 		or to be used as a communication client.
 * 
 * @param eventBus - of the relevant Vertx object.
 * @param cleintName - the name of the sender for outgoing messages.
 */
public abstract class EventBusAbstractClient 
		implements IMessageFailedNotification, IMessageSuccessNotification,INewClientNotification{
	
	private static final Level LOG_LEVEL = Level.SEVERE;
	private EventBus eventBus;
	private Vertx vertx;
	protected String clientName;
	private AtomicLong connectTimerID;
	//Designated for the serverIMPL for saving awaiting response messages - Should be used by the receiving server client.
	private Map<String,EventBusMessage> awatingResponseMessages;
	
	public EventBusAbstractClient(Vertx vertx, String clientName) {
		super();
		this.vertx = vertx;
		this.connectTimerID = new AtomicLong(0);
		this.eventBus = vertx.eventBus();
		this.clientName = clientName;
		this.awatingResponseMessages  = new HashMap<>();
		NotificationService.addNewClientObservers(this);
		NotificationService.addMessageSuccessObservers(this);
		NotificationService.addMessageFailedObservers(this);
		EventBusLogger.createLogger(getClass(),LOG_LEVEL,vertx,clientName);
		//Connecting first to the event bus verticle instance in order to get managed connection list.
		sendConnectRequest();
		
	}
	
	/**
	 * Connects to the Event Bus main router, a verticle that gathers all info
	 * on other verticles running on the same server.
	 */
	
	private void sendConnectRequest() {
		if(!clientName.equals(MembersManager.getDefaultName())) {
			EventBusMessage connectMessage = EventBusMessageUtils.connectMessage();
			connectTimerID.set(vertx.setPeriodic(6000, task -> {
				try {
					sendMessage(MembersManager.getDefaultName(), HttpMethod.CONNECT, connectMessage);
					EventBusLogger.INFO(getClass(), "Sent subscribe request to event bus router", LOG_LEVEL);
				}catch (Exception e) {
					EventBusLogger.ERROR(getClass(), e,"Failed sending connect message to event bus router", LOG_LEVEL);
				}
			}));
			
		}else {
			EventBusLogger.INFO(getClass(), "Event bus router initialized client", LOG_LEVEL);
		}
	}
	
	/**
	 * 
	 * @param serverName - target (relevant target eventbus server.)
	 * @param requestMethod - the request method (PUT,GET..)
	 * @param message - The relevant message.
	 * 
	 */
	public void sendMessage(String serverName, HttpMethod requestMethod,EventBusMessage message) throws Exception{
		WorkerExecutor executor = vertx.createSharedWorkerExecutor("NETWORKING_SEND_MESSAGE_"+message.getMessageID());
		executor.executeBlocking(future ->{
			try {
				message.setSender(this.clientName);
				String consumerAddress = getConsumberAddress(serverName,requestMethod);
				this.eventBus.send(consumerAddress, message,res ->{
					if(res.succeeded()) {
						future.complete();
					}else {
						future.fail(res.cause());
					}
				});
			}catch (Exception e) {
				future.fail(e);
			}finally {
				executor.close();
			}
		}, result ->{
			if(result.succeeded()) {
				EventBusLogger.INFO(getClass(), "Message: "+message.getMessageID()+
						" from: "+message.getSender()+" sent to: "+serverName, LOG_LEVEL);
			}else {
				if(isAwaitingResponse(message.getMessageID())) {
					removeAwaitingResponseMessage(message.getMessageID());
				}
				EventBusLogger.ERROR(getClass(), result.cause(), LOG_LEVEL);
				System.out.println("------------Failed sending message to event bus: "+result.cause().getMessage()
						+"----------------------------------");
				throw new UncheckedIOException(new IOException(result.cause()));
			}
		});
	}
	
	/**
	 * @param targetAddress - server's address.
	 * Sends a connect request to a different verticle.
	 */
	public void connectToServer(String targetAddress) throws Exception{
		EventBusMessage message = EventBusMessageUtils.connectMessage();
		sendMessage(targetAddress, HttpMethod.CONNECT, message);
	}
	
	
	
	
	/**
	 * 
	 * @param serverName - target server
	 * @param requestMethod - HTTP method
	 * @return server specific consumer address - according to the HTTP method. (serverName + httpMethod)
	 */
	private String getConsumberAddress(String serverName, HttpMethod requestMethod)throws Exception {
		return MembersManager.getConsumberAddress(serverName, requestMethod);
	}
	
	@Override
	public void onNewClient(String serverName,Future<Object> future) {
		try {
			EventBusMessage connectResponse = new EventBusMessage("connectResponse",new JsonObject());
			sendMessage(serverName, HttpMethod.OTHER, connectResponse);
		}catch (Exception e) {
			System.out.println("-----------Failed in new client listener in abstract client: "+e.getMessage()+"----------------");
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
		}
	}
	
	@Override
	public void onMessageFailed(EventBusMessage errorMessage,Future<Object> future) {
		try {
			String messageID = errorMessage.getMessageID();
			if(isAwaitingResponse(messageID)) {
				sendResponse(errorMessage);
				EventBusLogger.INFO(getClass(), "Response to message: "+errorMessage.getMessageID()
				+" sent as error message", LOG_LEVEL);
			}else {
				System.out.println("----------Messag Failed with no response hadnler---------------------");
				EventBusLogger.ERROR(getClass(), (Throwable) errorMessage.getData().getValue("error")
						,"Messag Failed with no response hadnler", LOG_LEVEL);
			}
		}catch (Exception e) {
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
			future.fail(e);
		}
	}

	@Override
	public void onMessageSucceeded(EventBusMessage message,Future<Object> future) {
		try {
//			if(isAwaitingResponse(message.getMessageID())) {
//				sendResponse(message);
//				EventBusLogger.INFO(getClass(), 
//						"Success response message to message: "+message.getMessageID(),LOG_LEVEL);
//			}else {
//				EventBusLogger.INFO(getClass(), "Message: "+message.getMessageID()
//				+" succedded with no response handlers", LOG_LEVEL);
//			}
			sendMessage(message.getSender(), HttpMethod.OTHER, message);
		}catch (Exception e) {
			System.out.println("-----------Failed in on message succedded: "+e.getMessage()+"--------------------");
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
			future.fail(e);
		}
	}
	
	private void sendResponse(EventBusMessage message) throws Exception {
		sendMessage(message.getSender(), HttpMethod.OTHER, message);
	}

	public void addAwaitingResponseMessage(String messageID, EventBusMessage resultHandler) {
		awatingResponseMessages.put(messageID, resultHandler);
	}
	
	public void removeAwaitingResponseMessage(String messageID) {awatingResponseMessages.remove(messageID);}
	
	public EventBusMessage getAwaitingResponseMessage(String messageID){
		return awatingResponseMessages.get(messageID);
	}

	public boolean isAwaitingResponse(String messageID) {
		return awatingResponseMessages.containsKey(messageID);
	}

	public void cancelSubscribeTimer() {
		vertx.cancelTimer(connectTimerID.get());
	}
	
}
