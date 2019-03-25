package org.crypto.communication.internal.server;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	
	public EventBusAbstractServer(Vertx vertx, String serverName,Handler<AsyncResult<Void>> deploymentHandler){
		try {
			MembersManager.init(vertx);
			EventBusLogger.createLogger(getClass(), LOG_LEVEL,vertx,serverName);
			this.vertx = vertx;
			this.eventBus = vertx.eventBus();
			this.serverName = serverName;
			this.consumers = new HashMap<>();
			this.router = IEventBusRouter.create();
			this.responseHandlers  = new HashMap<>();
			registerConsumers();
			registerMandatoryMessageHandlers();
			deploymentHandler.handle(Future.succeededFuture());
		}catch (Exception e) {
			// TODO: handle exception
			deploymentHandler.handle(Future.failedFuture(e));
		}
	}
	
	//Creating handlers for the connect and disconnect calls.
	private void registerMandatoryMessageHandlers() throws Exception{
		try {
			this.router.connect("connect", this::addEventBusMember);
			this.router.connect("disconnect", this::removeEventBusMember);
			this.router.connect("connectToAll", this::subscribeToMembers);
			this.router.responseHandler("connectResponse", this::gotConnectResponse);
			EventBusLogger.INFO(getClass(), "Mandatory handlers registered successfully", LOG_LEVEL);
		}catch (Exception e) {
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
			System.out.println("------------Failed in registerMandatoryMessageHandlers: "+e.getMessage()+"-----------------");
			throw new Exception(e);
		}
	}

	//Register consumers for all HTTP methods.
	protected void registerConsumers() throws Exception{
		try {
			this.eventBus.registerDefaultCodec(EventBusMessage.class, new EventBusMessageCodec());
			
			MessageConsumer<EventBusMessage> connectConsumer = this.eventBus.consumer(serverName+HttpMethod.CONNECT,
					msg ->{
						EventBusMessage message = new EventBusMessage((EventBusMessage)msg.body());
						WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_CONNECT_MESSAGE_"+((EventBusMessage)msg.body()).getMessageID());
						executor.executeBlocking(future -> {
							try {
								readConnectMessage(message, future);
								msg.reply("OK");
							}catch (Exception e) {
								future.fail(e);
							}finally {
								executor.close();
							}
						}, resultHandler -> {
							if(resultHandler.succeeded()) {
								message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
								NotificationService.sendMessageSuccessNotification(message, vertx);
								System.out.println("===============================Message successedded===============================");
							}else {
								resultHandler.cause().printStackTrace();
								message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
								NotificationService.sendMessageFailedNotfication(message, vertx);
								System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! message failed: "+
								resultHandler.cause().getMessage()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
							}
						});
					});
			addConsumer(HttpMethod.CONNECT, connectConsumer);
			
			MessageConsumer<EventBusMessage> postConsumer = this.eventBus.consumer(serverName+HttpMethod.POST.name(), msg ->{
				EventBusMessage message = new EventBusMessage((EventBusMessage)msg.body());
				WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_POST_MESSAGE_"+((EventBusMessage)msg.body()).getMessageID());
				executor.executeBlocking(future -> {
					try {
						readPostMessage(message, future);
						msg.reply("OK");
					}catch (Exception e) {
						future.fail(e);
					}finally {
						executor.close();
					}
				}, resultHandler -> {
					if(resultHandler.succeeded()) {
						message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
						NotificationService.sendMessageSuccessNotification(message, vertx);
						System.out.println("===============================Message successedded===============================");
					}else {
						message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
						NotificationService.sendMessageFailedNotfication(message, vertx);

						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! message failed: "+
						resultHandler.cause().getMessage()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				});
			});
			addConsumer(HttpMethod.POST,postConsumer);
			
			MessageConsumer<EventBusMessage> getConsumer = this.eventBus.consumer(serverName+HttpMethod.GET.name(), msg ->{
				System.out.println(msg.body());
				EventBusMessage message = new EventBusMessage((EventBusMessage)msg.body());
				WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_GET_MESSAGE_"+((EventBusMessage)msg.body()).getMessageID());
				executor.executeBlocking(future -> {
					try {
						readGetMessage(message, future);
						msg.reply("OK");
					}catch (Exception e) {
						future.fail(e);
					}finally {
						executor.close();
					}
				}, resultHandler -> {
					if(resultHandler.succeeded()) {
						message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
						NotificationService.sendMessageSuccessNotification(message, vertx);

						System.out.println("===============================Message successedded===============================");
					}else {
						message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
						NotificationService.sendMessageFailedNotfication(message, vertx);

						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! message failed: "+
						resultHandler.cause().getMessage()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				});
			});
			
			addConsumer(HttpMethod.GET,getConsumer);
			
			MessageConsumer<EventBusMessage> putConsumer =  this.eventBus.consumer(serverName+HttpMethod.PUT.name(), msg ->{
				EventBusMessage message = new EventBusMessage((EventBusMessage)msg.body());
				WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_PUT_MESSAGE_"+((EventBusMessage)msg.body()).getMessageID());
				executor.executeBlocking(future -> {
					try {
						readPutMessage(message, future);
						msg.reply("OK");
					}catch (Exception e) {
						future.fail(e);
					}finally {
						executor.close();
					}
				}, resultHandler -> {
					if(resultHandler.succeeded()) {
						message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
						NotificationService.sendMessageSuccessNotification(message, vertx);

						System.out.println("===============================Message successedded===============================");
					}else {
						message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
						NotificationService.sendMessageFailedNotfication(message, vertx);

						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! message failed: "+
						resultHandler.cause().getMessage()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				});
			});
			addConsumer(HttpMethod.PUT,putConsumer);
			
			MessageConsumer<EventBusMessage> deleteConsumer =  this.eventBus.consumer(serverName+HttpMethod.DELETE.name(), msg ->{
				EventBusMessage message = new EventBusMessage((EventBusMessage)msg.body());
				WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_DELETE_MESSAGE_"+((EventBusMessage)msg.body()).getMessageID());
				executor.executeBlocking(future -> {
					try {
						readDeleteMessage(message, future);
						msg.reply("OK");
					}catch (Exception e) {
						future.fail(e);
					}finally {
						executor.close();
					}
				}, resultHandler -> {
					if(resultHandler.succeeded()) {
						message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
						NotificationService.sendMessageSuccessNotification(message, vertx);

						System.out.println("===============================Message successedded===============================");
					}else {
						message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
						NotificationService.sendMessageFailedNotfication(message, vertx);

						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! message failed: "+
						resultHandler.cause().getMessage()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				});
			});
			addConsumer(HttpMethod.DELETE,deleteConsumer);
			
			MessageConsumer<EventBusMessage> otherConsumer = this.eventBus.consumer(serverName+HttpMethod.OTHER.name(), msg ->{
				EventBusMessage message = new EventBusMessage((EventBusMessage)msg.body());
				WorkerExecutor executor = vertx.createSharedWorkerExecutor("INCOMING_MESSGE_RESPONSE_"+((EventBusMessage)msg.body()).getMessageID());
				executor.executeBlocking(future -> {
					try {
						readMessageResponse(message, future);
						msg.reply("OK");
					}catch (Exception e) {
						future.fail(e);
					}finally {
						executor.close();
					}
				}, resultHandler -> {
					if(resultHandler.succeeded()) {
						message.setData(EventBusMessageUtils.getSuccessMessage(resultHandler.result()));
						NotificationService.sendMessageSuccessNotification(message, vertx);
						System.out.println("===============================Message successedded===============================");
					}else {
						message.setData(EventBusMessageUtils.getErrorMessage(resultHandler.cause()));
						NotificationService.sendMessageFailedNotfication(message, vertx);

						System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!! message failed: "+
						resultHandler.cause().getMessage()+" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
					}
				});
			});
			
			addConsumer(HttpMethod.OTHER, otherConsumer);
			
		}catch (Exception e) {
			System.out.println("---------------------------------- Failed in creating consumers: "+e.getMessage()+"------------------");
			EventBusLogger.ERROR(getClass(), e, LOG_LEVEL);
			System.exit(500);
			throw new Exception(e);
		}
		
	}
	
	private void addConsumer(HttpMethod method, MessageConsumer<EventBusMessage> consumer) throws Exception{
		consumer.completionHandler(res -> {
			if(res.succeeded()) {
				consumers.put(method, consumer);
				EventBusLogger.INFO(getClass(), method+" consumer registered", LOG_LEVEL);
				System.out.println(method+" consumer registered");
			}else {
				System.out.println("---------consumer registration failed: "+res.cause().getMessage());
				EventBusLogger.ERROR(getClass(), res.cause(),method+" registration failed",LOG_LEVEL);
			}
		});
		
		consumer.exceptionHandler(exception -> {
			System.out.println("Exception in: "+consumer.address()+" due to: "+exception.getMessage());
			EventBusLogger.ERROR(getClass(), exception, LOG_LEVEL);
		});
		
	}
	
	
	/**
	 * @publish - send the same message to many recipients */
	private void publishMessage(Set<String> addresses, HttpMethod method,EventBusMessage message)throws Exception {
		EventBusNetworking.getNetworking().sendMultipleMessages(addresses, method, message);
	}
	
	public void removeEventBusMember(EventBusMessage message) {
		try {
			MembersManager.removeClient(message.getSender());
			System.out.println(message.getSender()+" disconnected!!!!!!!");
			EventBusLogger.INFO(getClass(), message.getSender()+" disconnected", LOG_LEVEL);
			message.finishMessageReading();
		}catch (Exception e) {
			message.messageReadingFailed(e);
		}
	}
	/**
	 * @send - sends connect message to many targets, targets will be available from the connectResponse
	 * data. 
	 * @param reponseMessage
	 * @param future
	 */
	private void subscribeToMembers(EventBusMessage reponseMessage) {
		try {
			Set<String> clients = new HashSet<>();
			JsonArray clientsArray = reponseMessage.getData().getJsonArray("members");
			JsonObject methods = new JsonObject().put("methods", new JsonArray().add(HttpMethod.CONNECT)
					.add(HttpMethod.GET).add(HttpMethod.POST).add(HttpMethod.PUT).add(HttpMethod.DELETE).add(HttpMethod.OTHER));
			MembersManager.addClient(reponseMessage.getSender(), methods, vertx);
			for(int i = 0; i<clientsArray.size(); i++) {
				MembersManager.addClient(clientsArray.getString(i), methods, vertx);
				clients.add(clientsArray.getString(i));
			}
			if(clientsArray.size()>0) {
				EventBusMessage connectMessage = EventBusMessageUtils.connectMessage();
				
				EventBusNetworking.getNetworking().sendMultipleMessages(clients, HttpMethod.CONNECT, connectMessage);
			}
			EventBusNetworking.getNetworking().markAsConnected();
			reponseMessage.finishMessageReading();
		}catch (Exception e) {
			reponseMessage.messageReadingFailed(e);
		}
	}
	
	public void gotConnectResponse(EventBusMessage messag) {
		try {
			EventBusLogger.INFO(getClass(),messag.getSender()+ " responded properly to subscribe request", LOG_LEVEL);
			EventBusNetworking.getNetworking().markAsConnected();
			messag.finishMessageReading();
		}catch (Exception e) {
			messag.messageReadingFailed(e);
		}
	}
	public void addEventBusMember(EventBusMessage message) {
		try {
			MembersManager.addClient(message.getSender(),  message.getData(),vertx);
			message.finishMessageReading();
		}catch (Exception e) {
			message.messageReadingFailed(e);
		}
	}
	
	protected void close() {
		EventBusMessage message = new EventBusMessage("disconnect",null);
		Set<String> clients = MembersManager.getAllClients();
		
		try {
			publishMessage(clients, HttpMethod.CONNECT, message);
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
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.CONNECT, message.getPath());
			message.setReadFuture(future);
			handler.handle(message);
		}catch (Exception e) {
			System.out.println("-----------Reading connect message failed: "+e.getMessage()+"------------");
			future.fail(e);
		}
	}
	protected void readPostMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.POST, message.getPath());
			message.setReadFuture(future);
			handler.handle(message);
		}catch (Exception e) {
			System.out.println("-----------Reading post message failed: "+e.getMessage()+"------------");
			future.fail(e);
		}
	}
	
	protected void readPutMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.PUT, message.getPath());
			message.setReadFuture(future);
			handler.handle(message);
		}catch (Exception e) {
			System.out.println("-----------Reading put message failed: "+e.getMessage()+"------------");
			future.fail(e);
		}
	}

	protected void readGetMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.GET, message.getPath());
			message.setReadFuture(future);
			handler.handle(message);
		}catch (Exception e) {
			System.out.println("-----------Reading get message failed: "+e.getMessage()+"------------");
			future.fail(e);
		}
	}

	protected void readDeleteMessage(EventBusMessage message,Future<Object> future) {
		try {
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.DELETE, message.getPath());
			message.setReadFuture(future);
			handler.handle(message);
		}catch (Exception e) {
			System.out.println("-----------Reading delete message failed: "+e.getMessage()+"------------");
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
			EventBusLogger.INFO(getClass(), "Message: "+message.getMessageID()
			+" got valid response from event bus", LOG_LEVEL);
			
		}
		catch (Exception e) {
			EventBusLogger.ERROR(getClass(), e,"Failed handling message reponse: "+message.getMessageID(), LOG_LEVEL);
			System.out.println("-----------Reading message response failed: "+e.getMessage()+"------------");
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
