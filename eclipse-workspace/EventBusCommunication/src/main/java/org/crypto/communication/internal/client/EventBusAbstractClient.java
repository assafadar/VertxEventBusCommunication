package org.crypto.communication.internal.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 * 		Event bus client template with minimal functionality to be interpreted by inheriting objects.
 * 
 * @param eventBus - of the relevant Vertx object.
 * @param cleintName - the name of the sender for outgoing messages.
 */
public abstract class EventBusAbstractClient 
		implements IMessageFailedNotification, IMessageSuccessNotification,INewClientNotification{
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private EventBus eventBus;
	private Vertx vertx;
	private String clientName;
	
	public EventBusAbstractClient(Vertx vertx, String clientName) {
		super();
		this.vertx = vertx;
		this.eventBus = vertx.eventBus();
		this.clientName = clientName;
		NotificationService.addNewClientObservers(this);
	}
	
	/**
	 * 
	 * @param serverName - target (relevant target eventbus server.)
	 * @param requestMethod - the request method (PUT,GET..)
	 * @param message - The relevant message.
	 * 
	 * Saves the particular message in awatingReponse map - waiting for response.
	 */
	public void sendMessageWithResponse(String serverName, HttpMethod requestMethod, EventBusMessage message
			,Handler<AsyncResult<JsonObject>> resultHandler) {
		
		EventBusMessageUtils.addMessageHandler(message.getMessageID(), resultHandler);
		sendMessage(serverName, requestMethod, message);
	}
	
	/**
	 * 
	 * @param serverName - target (relevant target eventbus server.)
	 * @param requestMethod - the request method (PUT,GET..)
	 * @param message - The relevant message.
	 * 
	 */
	public void sendMessage(String serverName, HttpMethod requestMethod,EventBusMessage message) {
		WorkerExecutor executor = vertx.createSharedWorkerExecutor("NETWORKING_SEND_MESSAGE_"+message.getMessageID());
		executor.executeBlocking(future ->{
			try {
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
				logger.info("Message "+message.getMessageID()+" sent successfully");
			}else {
				if(EventBusMessageUtils.isAwaitingResponse(message.getMessageID())) {
					EventBusMessageUtils.getMessageHandler(message.getMessageID())
													.handle(Future.failedFuture(result.cause()));
				}else {
					logger.error(message.getMessageID()+" was not sent due to: "+result.cause().getMessage());
				}
			}
		});
	}
	
	/**
	 * @param targetAddress - server's address.
	 * Sends a connect request to a different verticle.
	 */
	public void connectToServer(String targetAddress) {
		EventBusMessage message = new EventBusMessage(this.clientName,"connect",null);
		sendMessage(targetAddress, HttpMethod.CONNECT, message);
	}
	
	/**
	 * Connects to the Event Bus main router, a verticle that gathers all info
	 * on other verticles running on the same server.
	 */
	public void connectToEventBus() {
		sendMessage("EVENT_BUS_ROUTER",HttpMethod.POST,new EventBusMessage(clientName,"connect",null));
	}
	
	
	/**
	 * 
	 * @param serverName - target server
	 * @param requestMethod - HTTP method
	 * @return server specific consumer address - according to the HTTP method. (serverName + httpMethod)
	 */
	private String getConsumberAddress(String serverName, HttpMethod requestMethod) {
		return MembersManager.getConsumberAddress(serverName, requestMethod);
	}
	
	@Override
	public void onNewClient(String serverName) {
		Map<String,Map<HttpMethod,String>> clients = MembersManager.getAllRegistrations();
		JsonArray jsonArray = new JsonArray();
		for(String client : clients.keySet()) {
			if(!client.equals(serverName)) {
				Set<String> addresses = new HashSet<>();
				JsonObject data = new JsonObject();
				addresses.addAll(clients.get(client).values());
				data.put(client, new ArrayList<>(addresses));
				jsonArray.add(data);
			}
		}
		
		sendMessage(serverName, HttpMethod.CONNECT, new EventBusMessage(MembersManager.getDefaultName(), 
				"connect", new JsonObject().put("members", jsonArray)));
		
	}
	
	@Override
	public void onMessageFailed(EventBusMessage errorMessage,Future<Object> future) {
		
	}

	@Override
	public void onMessageSucceeded(EventBusMessage message,Future<Object> future) {
		
	}
	
}
