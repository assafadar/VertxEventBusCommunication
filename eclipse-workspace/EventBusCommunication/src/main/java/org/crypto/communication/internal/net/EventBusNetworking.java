package org.crypto.communication.internal.net;

import org.crypto.communication.internal.client.ClientImpl;
import org.crypto.communication.internal.client.EventBusAbstractClient;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.router.EventBusAbstractRouter;
import org.crypto.communication.internal.router.IEventBusRouter;
import org.crypto.communication.internal.server.EventBusAbstractServer;
import org.crypto.communication.internal.server.ServerImpl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
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
	private EventBusAbstractRouter router;
	private static Vertx vertx;
	private static EventBusNetworking INSTANCE;
	private static EventBusAbstractClient eventBusClientImpl;
	private static EventBusAbstractServer eventBusServerImpl;
	
	
	/**
	 * Event bus networking instantiation
	 */
	private EventBusNetworking() {
		if(vertx == null) {
			throw new IllegalArgumentException("Vertx instance is needed");
		}
		if(eventBusClientImpl == null ) {
			eventBusClientImpl = new ClientImpl(vertx);
		}
		
		if(eventBusServerImpl == null) {
			eventBusServerImpl = new ServerImpl(vertx);
		}
		this.router = IEventBusRouter.create();
	}
	
	/**
	 * @Fluent
	 * @param vertx
	 * sets vertx instance and returns networking instance.
	 */
	public static EventBusNetworking init(Vertx vertx,EventBusAbstractClient client, EventBusAbstractServer server) {
		EventBusNetworking.vertx = vertx;
		return getNetworking(client,server);
	}
	
	/**
	 * 
	 * @returns the singleton @param INSTANCE
	 * or creates a new one if does not exists.
	 */
	public static EventBusNetworking getNetworking() {
		if(INSTANCE == null) {
			INSTANCE = new EventBusNetworking();
		}
		return INSTANCE;
	}
	
	private static EventBusNetworking getNetworking(EventBusAbstractClient client, EventBusAbstractServer server) {
		eventBusClientImpl = client;
		eventBusServerImpl = server;
		if(INSTANCE == null) {
			INSTANCE = new EventBusNetworking();
		}
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
	public void sendMessage(String targetAddress,HttpMethod requestMethod,EventBusMessage message) {
		eventBusClientImpl.sendMessage(targetAddress, requestMethod, message);
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
		eventBusClientImpl.sendMessageWithResponse(targetAddress, requestMethod, message, resultHandler);
	}
	
	
	
	
	/**
	 * 
	 * @param addresses - destination addresses
	 * @param requestMethod - HttpMethod for the handler of the request
	 * @param message		- The eventbus message codec object, custom messages.
	 * 
	 * sends the same message trough the event bus client to the event bus for the registered consumers.
	 */
	public void sendMultipleMessages(JsonArray addresses, HttpMethod requestMethod, EventBusMessage message) {
		addresses.forEach( address -> {
			eventBusClientImpl.sendMessage((String)address, requestMethod, message);
		});
	}
	
	//Returns the router instance.
	public IEventBusRouter getRouter() {
		return this.router;
	}
}
