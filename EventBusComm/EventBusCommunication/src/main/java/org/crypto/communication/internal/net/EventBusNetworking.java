package org.crypto.communication.internal.net;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.logging.Level;

import org.crypto.communication.internal.client.ClientImpl;
import org.crypto.communication.internal.client.EventBusAbstractClient;
import org.crypto.communication.internal.client.EventBusClient;
import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.router.IEventBusRouter;
import org.crypto.communication.internal.server.EventBusAbstractServer;
import org.crypto.communication.internal.server.ServerImpl;
import org.crypto.communication.internal.utils.MembersManager;
import org.crypto.communication.internal.verticle.IEventBusUser;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

/**
 * @author asaf
   @since 12/2018
 * 
 * @description:
 * 	EventBus networking singleton instance that provides internal communication
 * 	between verticles based on Hazlecast cluster manager. 
 * 	
 * @param INSTANCE - The eventbus networing instance, one per process.
 * @param eventBusClientImpl - event bus client implementation for sending messages. 
 * @param eventBusServerImpl - event bus client implementation for receiving messages. 
 * @param vertx - in order to access the process that the verticle is running on and his EventBus object. 
	
 */
public class EventBusNetworking{
	private static final Level LOG_LEVEL = Level.SEVERE;
	private static Vertx vertx;
	private static EventBusNetworking INSTANCE;
	private static EventBusAbstractClient eventBusClientImpl;
	private static EventBusAbstractServer eventBusServerImpl;
	
	
	/**
	 * Event bus networking instantiation
	 */
	private EventBusNetworking(String verticleName,Handler<AsyncResult<Void>> deploymentHandler) {
		if(vertx == null) {
			throw new IllegalArgumentException("Vertx instance is needed");
		}
		String name = (verticleName == null)?MembersManager.getDefaultName():verticleName;
		
		eventBusServerImpl = new ServerImpl(name,vertx,deploymentHandler);
		
		eventBusClientImpl = (verticleName == null || verticleName.equals("") )? 
				new EventBusClient(vertx) : new ClientImpl(verticleName,vertx);
				
		EventBusLogger.createLogger(getClass(),LOG_LEVEL,vertx,name);
	
	}
	
	/**
	 * @Fluent
	 * @param vertx
	 * sets vertx instance and returns networking instance.
	 */
	public static EventBusNetworking init(Vertx vertx,IEventBusUser verticle,Handler<AsyncResult<Void>> deploymentHandler) {
		EventBusNetworking.vertx = vertx;
		return getNetworking((verticle == null)?null:verticle.verticleName(),deploymentHandler);
	}
	
	/**
	 * 
	 * @returns the singleton @param INSTANCE
	 * or creates a new one if does not exists.
	 */
	public static EventBusNetworking getNetworking() {
		if(INSTANCE == null) {
			throw new IllegalStateException("Please call INIT method first");
		}
		return INSTANCE;
	}
	
	private static EventBusNetworking getNetworking(String verticleName,Handler<AsyncResult<Void>> deploymentHandler) {
		if(INSTANCE == null) {
			INSTANCE = new EventBusNetworking(verticleName,deploymentHandler);
		}
		
		EventBusLogger.INFO(EventBusNetworking.class, "Client & Server replaced", LOG_LEVEL);
		return INSTANCE;
	}

	// Prevent double instantiation by serialization
	protected Object readResolve() {
		return INSTANCE;
	}
	
	/**
	 * 
	 * @param targetAddress - destination address
	 * @param requestMethod - HttpMethod for the handler of the request
	 * @param message		- The eventbus message codec object, custom messages.
	 * 
	 * sends a message trough the event bus client to the event bus for the registered consumer.
	 */
	public void sendMessage(String targetAddress,HttpMethod requestMethod,EventBusMessage message) throws Exception{
		eventBusClientImpl.sendMessage(targetAddress, requestMethod, message);
		EventBusLogger.INFO(EventBusNetworking.class, "Message ID: "+message.getMessageID()+" sent to: "
				+targetAddress, LOG_LEVEL);
	}
	
	/**
	 * 
	 * @param targetAddress - destination address
	 * @param requestMethod - HttpMethod for the handler of the request
	 * @param message		- The eventbus message codec object, custom messages.
	 * @param resultHandler - The handler that will handle the response from the external verticle.
	 * sends a message trough the event bus client to the event bus for the registered consumer.
	 */
	public void sendMessageWithResponse(String targetAddress,HttpMethod requestMethod,EventBusMessage message
											,Handler<AsyncResult<JsonObject>> resultHandler) {
		try {
			eventBusServerImpl.addMessageResponseHandler(message.getMessageID(), resultHandler);
			EventBusLogger.INFO(EventBusNetworking.class, 
					"Response listener registered for message: "+message.getMessageID(), LOG_LEVEL);
			eventBusClientImpl.sendMessage(targetAddress, requestMethod, message);
		}catch (Exception e) {
			eventBusServerImpl.removeMessageResponseHandler(message.getMessageID());
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
			resultHandler.handle(Future.failedFuture(e));
		}
		
	}
	
	/**
	 * 
	 * @param addresses - destination addresses
	 * @param requestMethod - HttpMethod for the handler of the request
	 * @param message		- The eventbus message codec object, custom messages.
	 * 
	 * sends the same message trough the event bus client to the event bus for the registered consumers.
	 */
	public void sendMultipleMessages(Set<String> addresses, HttpMethod requestMethod, EventBusMessage message) throws Exception{
		addresses.forEach( address -> {
			try {
				eventBusClientImpl.sendMessage((String)address, requestMethod, message);
			}catch (Exception e) {
				EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
				throw new UncheckedIOException(new IOException(e));
			}
		});
	}
	
	//Returns the router instance.
	public IEventBusRouter getRouter() {
		return this.eventBusServerImpl.getRouter();
	}

	public void markAsConnected() {
		eventBusClientImpl.cancelSubscribeTimer();
		EventBusLogger.INFO(getClass(), "Connected to event bus router", LOG_LEVEL);
	}
	
	public void failedSendingMessage(String messageID,Throwable e) {
		Handler<AsyncResult<JsonObject>> resultHandler = eventBusServerImpl.getResponseHandler(messageID);
		if(resultHandler!=null) {
			System.out.println("--------------------------- Result handler will fail now.... ------------------------------");
			resultHandler.handle(Future.failedFuture(e));
		}else {
			System.out.println("----------------------- no result handlers found ----------------------------------------");
		}
	}
	
}
