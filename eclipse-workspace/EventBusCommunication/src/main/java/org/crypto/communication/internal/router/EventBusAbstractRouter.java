package org.crypto.communication.internal.router;

import java.util.HashMap;
import java.util.Map;

import org.crypto.communication.internal.handler.IEventBusHandler;
import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
/**
 * 
 * @author Asaf
 * @description: 
 *			Abstract router that maintains all the registered handlers for an instance.
 *			The handlers consumes EventBusMessage object.
 */
public abstract class EventBusAbstractRouter implements IEventBusRouter {
//	private Map<HttpMethod,Map<String,Handler<EventBusMessage>>> handlers;
	private Map<HttpMethod,Map<String,IEventBusHandler<EventBusMessage>>> handlers;

	public EventBusAbstractRouter() {
		initRouterMap();
	}
	private void initRouterMap() {
		this.handlers = new HashMap<>();
		this.handlers.put(HttpMethod.GET, new HashMap<>());
		this.handlers.put(HttpMethod.PUT, new HashMap<>());
		this.handlers.put(HttpMethod.POST, new HashMap<>());
		this.handlers.put(HttpMethod.DELETE, new HashMap<>());
		this.handlers.put(HttpMethod.CONNECT, new HashMap<>());
		this.handlers.put(HttpMethod.OTHER, new HashMap<>());
	}
	
	protected void addResponseHandler(String path, IEventBusHandler<EventBusMessage> handler) {
		addHandler(HttpMethod.OTHER, path, handler);
	}
	protected void addGetHandler(String path,IEventBusHandler<EventBusMessage> handler) {
		addHandler(HttpMethod.GET, path, handler);
	}
	
	protected void addPutHandler(String path,IEventBusHandler<EventBusMessage> handler) {
		addHandler(HttpMethod.PUT, path, handler);
	}
	
	protected void addPostHandler(String path,IEventBusHandler<EventBusMessage> handler) {
		addHandler(HttpMethod.POST, path, handler);
	}

	protected void addDeleteHandler(String path,IEventBusHandler<EventBusMessage> handler) {
		addHandler(HttpMethod.DELETE, path, handler);
	}

	
	protected void addConnectHandler(String path, IEventBusHandler<EventBusMessage> handler) {
		addHandler(HttpMethod.CONNECT, path, handler);
	}

	
	private Map<String,IEventBusHandler<EventBusMessage>> getHandlers(HttpMethod httpMethod){
		return this.handlers.get(httpMethod);
	}

	
	public Map<HttpMethod,Map<String,IEventBusHandler<EventBusMessage>>> getAllHandlers(){
		return this.handlers;
	}

	
	public IEventBusHandler<EventBusMessage> getHandler(HttpMethod httpMethod,String path){
		return this.getHandlers(httpMethod).get(path);
	}

	
	private void addHandler(HttpMethod httpMethod,String path,IEventBusHandler<EventBusMessage> handler) {
		Map<String,IEventBusHandler<EventBusMessage>> handlersMap = this.handlers.get(httpMethod);
		handlersMap.put(path, handler);
		this.handlers.put(httpMethod, handlersMap);
	}

}
