package org.crypto.communication.internal.server;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.crypto.communication.internal.handler.IEventBusHandler;
import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.messages.EventBusMessageCodec;
import org.crypto.communication.internal.net.EventBusNetworking;
import org.crypto.communication.internal.notifications.NotificationService;
import org.crypto.communication.internal.router.EventBusAbstractRouter;
import org.crypto.communication.internal.router.IEventBusRouter;
import org.crypto.communication.internal.utils.EventBusMessageUtils;
import org.crypto.communication.internal.utils.MembersManager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.FutureFactoryImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class EventBusAbstractServer{
	private static final Level LOG_LEVEL = Level.SEVERE;
	protected Vertx vertx;
	protected EventBus eventBus;
	protected String serverName;
	//Holds all the registered consumers of a server
	protected Map<HttpMethod,MessageConsumer<EventBusMessage>> consumers;
	private EventBusAbstractRouter router;
	//Designated for the client in order to save result handlers for sent messages - Should be used by the OTHER method handler of the server. 
	private Map<String,Handler<AsyncResult<JsonObject>>> responseHandlers;
	
	public EventBusAbstractServer(Vertx vertx, String serverName){
		super();
		EventBusLogger.createLogger(getClass(), LOG_LEVEL);
		this.vertx = vertx;
		this.eventBus = vertx.eventBus();
		this.serverName = serverName;
		this.consumers = new HashMap<>();
		this.router = IEventBusRouter.create();
		this.responseHandlers  = new HashMap<>();
		registerConsumers();
		registerMandatoryMessageHandlers();
	}
	
	//Creating handlers for the connect and disconnect calls.
	private void registerMandatoryMessageHandlers(){
		try {
			this.router.connect("connect", this::addEventBusMember);
			this.router.connect("disconnect", this::removeEventBusMember);
			this.router.responseHandler("connectResponse", this::subscribeToMembers);
			EventBusLogger.INFO(getClass(), "Mandatory handlers registered successfully", LOG_LEVEL);
		}catch (Exception e) {
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
		}
	}

	//Register consumers for all HTTP methods.
	protected void registerConsumers() {
		try {
			this.eventBus.registerCodec(new EventBusMessageCodec()); 
			
			createConsumer(HttpMethod.CONNECT, this.getClass().getMethod("readConnectMessage"));
			createConsumer(HttpMethod.POST, this.getClass().getMethod("readPostMessage"));
			createConsumer(HttpMethod.GET, this.getClass().getMethod("readGetMessage"));
			createConsumer(HttpMethod.PUT, this.getClass().getMethod("readPutMessage"));
			createConsumer(HttpMethod.DELETE, this.getClass().getMethod("readDeleteMessage"));
			createConsumer(HttpMethod.OTHER, this.getClass().getMethod("readMessageResponse"));
			EventBusLogger.INFO(getClass(), "Consumers registered successfully", LOG_LEVEL);
		}catch (Exception e) {
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
			System.exit(500);
		}
		
	}
	//Creating a message consumer and invoking the relevant consumer in case of a message event.
	private void createConsumer(HttpMethod method,Method handlerMethod) throws Exception{
		
			MessageConsumer<EventBusMessage> consumer  = this.eventBus.consumer(serverName+method.name(),msg ->{
				//Launching new WorkerExecutor here is intended to clear the messages queue to a message handler executor 
				WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_MESSAGE_"+msg.body().getMessageID());
				EventBusMessage message = msg.body();
				executor.executeBlocking(future -> {
					try {
						EventBusLogger.INFO(getClass(), "New Incoming message", LOG_LEVEL);
						handlerMethod.invoke(this, (EventBusMessage) msg.body(),future);
					}catch (Exception e) {
						EventBusLogger.ERROR(getClass(), e,"Consumer invocation failed: "+e.getMessage(),LOG_LEVEL);
						future.fail(e);
					}finally {
						executor.close();
					}
				}, resultHandler -> {
					if(resultHandler.succeeded()) {
						message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
						NotificationService.sendMessageSuccessNotification(message, vertx);
					}else {
						message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
						NotificationService.sendMessageFailedNotfication(message, vertx);
					}
				});
			});
			addConsumer(method, consumer);
		
		
	}
	private void addConsumer(HttpMethod method, MessageConsumer<EventBusMessage> consumer) {
		consumers.put(method, consumer);
	}
	
	
	/**
	 * @publish - send the same message to many recipients */
	private void publishMessage(JsonArray addresses, HttpMethod method,EventBusMessage message)throws Exception {
		EventBusNetworking.getNetworking().sendMultipleMessages(addresses, method, message);
	}
	/**
	 * @send - send message to one recipient */
	private void sendMessage(String address,HttpMethod method,EventBusMessage message) throws Exception{
		EventBusNetworking.getNetworking().sendMessage(address, method, message);
	}
	public void removeEventBusMember(EventBusMessage message, Future<Object> future) {
		try {
			MembersManager.removeClient(message.getSender());
			future.complete();
		}catch (Exception e) {
			future.fail(e);
		}
	}
	/**
	 * @send - sends connect message to many targets, targets will be available from the connectResponse
	 * data. 
	 * @param reponseMessage
	 * @param future
	 */
	private void subscribeToMembers(EventBusMessage reponseMessage, Future<Object> future) {
		try {
			JsonArray members = reponseMessage.getData().getJsonArray("members");
			EventBusMessage connectMessage = EventBusMessageUtils.connectMessage(serverName); 
			EventBusNetworking.getNetworking().sendMultipleMessages(members, HttpMethod.CONNECT, connectMessage);
			EventBusNetworking.getNetworking().markAsConnected();
			future.complete();
		}catch (Exception e) {
			future.fail(e);
		}
	}
	public void addEventBusMember(EventBusMessage message, Future<Object> future) {
		try {
			MembersManager.addClient(message.getSender(),HttpMethod.CONNECT,  message.getData());
			future.complete();
		}catch (Exception e) {
			future.fail(e);
		}
	}
	
	protected void close() {
		EventBusMessage message = new EventBusMessage(serverName,"disconnect",null);
		JsonArray jsonArray = MembersManager.getAllClients();
		
		try {
			publishMessage(jsonArray, HttpMethod.CONNECT, message);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		this.eventBus.close(rs -> {
			if(rs.succeeded()) {
				System.out.println("Event bus closed");
			}
			else {
				System.out.println("Event bus failed closing");
			}
			
		});
	}
	
	
	public Map<HttpMethod,MessageConsumer<EventBusMessage>> getConsumers() {
		return this.consumers;
	}
	
	public void addRoute(HttpMethod method,String path,IEventBusHandler<EventBusMessage>handler ) {
		switch (method) {
		case GET:
		{
			this.router.get(path, handler);
			break;
		}
		case PUT:
		{
			this.router.put(path, handler);
			break;
		}
		case POST:
		{
			this.router.post(path, handler);
			break;
		}
		case DELETE:
		{
			this.router.delete(path, handler);
			break;
		}
		case CONNECT:
		{
			this.router.connect(path, handler);
			break;
		}

		default:
			break;
		}
	}
	
	protected IEventBusHandler<EventBusMessage> getHandler(HttpMethod httpMethod,String path){
		return this.router.getHandler(httpMethod, path);
	}
	
	private void readConnectMessage(EventBusMessage message,Future<Object> future)throws Exception{
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.POST, message.getPath());
			handler.handle(message, future);
		}catch (Exception e) {
			future.fail(e);
		}
	}
	protected void readPostMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.POST, message.getPath());
			handler.handle(message,future);
		}catch (Exception e) {
			future.fail(e);
		}
	}
	
	protected void readPutMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.PUT, message.getPath());
			handler.handle(message,future);
		}catch (Exception e) {
			future.fail(e);
		}
	}

	protected void readGetMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.GET, message.getPath());
			handler.handle(message,future);
		}catch (Exception e) {
			future.fail(e);
		}
	}

	protected void readDeleteMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.DELETE, message.getPath());
			handler.handle(message, future);
		}catch (Exception e) {
			future.fail(e);
		}
	}
	
	protected void readMessageResponse(EventBusMessage message,Future<Object> future) {
		try {
			if(isErrorMessage(message)) {
				responseHandlers.get(message.getMessageID()).handle(
						Future.failedFuture((Throwable) message.getData().getValue("error")));
			}else {
				responseHandlers.get(message.getMessageID()).handle(Future.succeededFuture(message.getData()));
			}
		}catch (NullPointerException e) {
			// TODO: nothing, means that message sent with no result handler.
		}
		catch (Exception e) {
			EventBusLogger.ERROR(getClass(), e,"Failed handling message reponse: "+message.getMessageID(), LOG_LEVEL);
		}
	}
	
	private boolean isErrorMessage(EventBusMessage message) {
		return message.getData().containsKey("error");
	}

	public IEventBusRouter getRouter() {
		return this.router;
	}
	
	
	public void addMessageResponseHandler(String messagID, Handler<AsyncResult<JsonObject>> resultHandler) {
		responseHandlers.put(messagID, resultHandler);
	}
	
	public void removeMessageResponseHandler(String messageID) {
		responseHandlers.remove(messageID);
	}
	
	public Handler<AsyncResult<JsonObject>> getResponseHandler(String messageID){
		return responseHandlers.get(messageID);
	}
	
	
	
	
}
