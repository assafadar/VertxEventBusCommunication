package org.crypto.communication.internal.handler;

import io.vertx.core.Future;

public interface IEventBusHandler<E> {
//	void handle(E event, Future<Object> future);
	void handle(E event);
}
