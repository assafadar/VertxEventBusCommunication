package org.crypto.communication.internal.server;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.crypto.communication.internal.handler.IEventBusHandler;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.messages.EventBusMessageCodec;
import org.crypto.communication.internal.net.EventBusNetworking;
import org.crypto.communication.internal.router.EventBusAbstractRouter;
import org.crypto.communication.internal.router.IEventBusRouter;
import org.crypto.communication.internal.utils.MembersManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public abstract class EventBusAbstractServer{
	protected Vertx vertx;
	protected EventBus eventBus;
	protected String serverName;
	protected Map<HttpMethod,MessageConsumer<EventBusMessage>> consumers;
	private EventBusAbstractRouter router;
	
	public EventBusAbstractServer(Vertx vertx, String serverName) {
		super();
		this.vertx = vertx;
		this.eventBus = vertx.eventBus();
		this.serverName = serverName;
		this.consumers = new HashMap<>();
		this.router = IEventBusRouter.create();
		registerConsumers();
	}
	

	protected void registerConsumers() {
		try {
			this.eventBus.registerCodec(new EventBusMessageCodec()); 
			
			createConsumer(HttpMethod.CONNECT, this.getClass().getMethod("readConnectMessage"));
			createConsumer(HttpMethod.POST, this.getClass().getMethod("readPostMessage"));
			createConsumer(HttpMethod.GET, this.getClass().getMethod("readGetMessage"));
			createConsumer(HttpMethod.PUT, this.getClass().getMethod("readPutMessage"));
			createConsumer(HttpMethod.DELETE, this.getClass().getMethod("readDeleteMessage"));
			createConsumer(HttpMethod.OTHER, this.getClass().getMethod("readMessageResponse"));
			
		}catch (Exception e) {
			System.err.println("Event bus consumers not registered properly, EXIT");
			System.exit(500);
		}
		
	}
	
	private void createConsumer(HttpMethod method,Method handlerMethod) throws Exception{
		MessageConsumer<EventBusMessage> consumer  = this.eventBus.consumer(serverName+method.name(),msg ->{
			try {
				handlerMethod.invoke(this, (EventBusMessage) msg.body());
			}catch (Exception e) {
				throw new UncheckedIOException(new IOException(e));
			}
		});
		addConsumer(method, consumer);
		
	}
	private void addConsumer(HttpMethod method, MessageConsumer<EventBusMessage> consumer) {
		consumers.put(method, consumer);
	}
	
	private void readConnectMessage(EventBusMessage message)throws Exception{
		switch (message.getPath()) {
		case "connect":
			addEventBusMember(message.getSender());
			break;
		case "conenctAll":
			connectToSubscriptions(message.getData());
			break;
		default:
			removeEventBusMember(message.getSender());
			break;
		}
		
	}
	private void connectToSubscriptions(JsonObject data) {
		JsonArray addresses = data.getJsonArray("members");
		JsonObject messageData = new JsonObject();
		
		for(HttpMethod method : consumers.keySet()) {
			messageData.put(method.name(),consumers.get(method).address());
		}
		
		EventBusMessage message = new EventBusMessage(serverName,"conenct",messageData);
		publishMessage(addresses,HttpMethod.CONNECT,message);
	}
	/**
	 * @publish - send the same message to many recipients */
	private void publishMessage(JsonArray addresses, HttpMethod method,EventBusMessage message) {
		EventBusNetworking.getNetworking().sendMultipleMessages(addresses, method, message);
	}
	/**
	 * @send - send message to one recipient */
	private void sendMessage(String address,HttpMethod method,EventBusMessage message){
		EventBusNetworking.getNetworking().sendMessage(address, method, message);
	}
	private void removeEventBusMember(String sender) {
		MembersManager.removeClient(sender);
	}


	private void addEventBusMember(String sender) {
		MembersManager.addClient(sender, HttpMethod.CONNECT, sender+HttpMethod.CONNECT);
	}
	
	protected void close() {
		EventBusMessage message = new EventBusMessage(serverName,"disconnect",null);
		JsonArray jsonArray = MembersManager.getAllClients();
		
		publishMessage(jsonArray, HttpMethod.CONNECT, message);
		
		this.eventBus.close(rs -> {
			if(rs.succeeded()) {
				System.out.println("Event bus closed");
			}
			else {
				System.out.println("Event bus failed closing");
			}
			
		});
	}
	
	protected void readPostMessage(EventBusMessage message) {
		WorkerExecutor postExecutor = vertx.createSharedWorkerExecutor
				("NETWORKING_READ_POST_MESSAGE_"+message.getMessageID());
		postExecutor.executeBlocking(future ->{
			try {
				IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.POST, message.getPath());
				handler.handle(message,future);
			}catch (Exception e) {
				future.fail(e);
			}finally {
				postExecutor.close();
			}
		}, result -> {
			if(result.succeeded()) {
				
			}else {
				
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
	
	protected void readPutMessage(EventBusMessage message) {
		WorkerExecutor putExecutor = vertx.createSharedWorkerExecutor
				("NETWORKING_READ_PUT_MESSAGE_"+message.getMessageID());
		putExecutor.executeBlocking(future ->{
			try {
				IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.PUT, message.getPath());
				handler.handle(message,future);
			}catch (Exception e) {
				future.fail(e);
			}finally {
				putExecutor.close();
			}
		}, result -> {
			if(result.succeeded()) {
				
			}else {
				
			}
		});
	}

	protected void readGetMessage(EventBusMessage message) {
		WorkerExecutor getExecutor = vertx.createSharedWorkerExecutor
				("NETWORKING_READ_POST_MESSAGE_"+message.getMessageID());
		getExecutor.executeBlocking(future ->{
			try {
				IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.GET, message.getPath());
				handler.handle(message,future);
			}catch (Exception e) {
				future.fail(e);
			}finally {
				getExecutor.close();
			}
		}, result -> {
			if(result.succeeded()) {
				
			}else {
				
			}
		});
	}

	protected void readDeleteMessage(EventBusMessage message) {
		WorkerExecutor deleteExecutor = vertx.createSharedWorkerExecutor
				("NETWORKING_READ_POST_MESSAGE"+message.getMessageID());
		deleteExecutor.executeBlocking(future ->{
			IEventBusHandler<EventBusMessage> handler = getHandler(HttpMethod.DELETE, message.getPath());
			handler.handle(message, future);
		}, resultHandler ->{
			if(resultHandler.succeeded()) {
			}else {
				
			}
		});
	}
	
	
	
	
}
