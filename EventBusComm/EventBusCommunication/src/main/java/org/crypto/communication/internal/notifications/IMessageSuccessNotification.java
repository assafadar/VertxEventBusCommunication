package org.crypto.communication.internal.notifications;

import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.Future;

public interface IMessageSuccessNotification {
	void onMessageSucceeded(EventBusMessage message, Future<Object> future);
}
