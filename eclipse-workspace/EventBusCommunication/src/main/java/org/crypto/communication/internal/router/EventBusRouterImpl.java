package org.crypto.communication.internal.router;

import org.crypto.communication.internal.handler.IEventBusHandler;
import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.Handler;

public class EventBusRouterImpl extends EventBusAbstractRouter {

	
	@Override
	public void get(String path, IEventBusHandler<EventBusMessage> handler) {
		addGetHandler(path, handler);
	}

	
	@Override
	public void post(String path, IEventBusHandler<EventBusMessage> handler) {
		addPostHandler(path, handler);
	}

	
	@Override
	public void put(String path, IEventBusHandler<EventBusMessage> handler) {
		addPutHandler(path, handler);
	}

	
	@Override
	public void delete(String path, IEventBusHandler<EventBusMessage> handler) {
		addDeleteHandler(path, handler);
	}

	
	@Override
	public void connect(String path, IEventBusHandler<EventBusMessage> handler) {
		addConnectHandler(path,handler);
	}

	@Override
	public void responseHandler(String path, IEventBusHandler<EventBusMessage> handler) {
		addResponseHandler(path, handler);
	}

	

	

}
