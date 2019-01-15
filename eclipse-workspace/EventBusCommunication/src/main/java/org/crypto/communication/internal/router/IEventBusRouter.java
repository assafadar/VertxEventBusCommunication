package org.crypto.communication.internal.router;

import org.crypto.communication.internal.handler.IEventBusHandler;
import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.Handler;

public interface IEventBusRouter {
	
	static EventBusAbstractRouter create() {
		return new EventBusRouterImpl();
	}
	
	
	void get(String path,IEventBusHandler<EventBusMessage>handler);
//	void get(String path,Handler<EventBusMessage> handler);
	void post(String path,IEventBusHandler<EventBusMessage> handler);
//	void post(String path,Handler<EventBusMessage> handler);
	void put(String path,IEventBusHandler<EventBusMessage> handler);
//	void put(String path,Handler<EventBusMessage> handler);
	void delete(String path,IEventBusHandler<EventBusMessage> handler);
//	void delete(String path,Handler<EventBusMessage> handler);
	void connect(String path, IEventBusHandler<EventBusMessage> handler);
//	void connect(String path, Handler<EventBusMessage> handler);
	
	
}
