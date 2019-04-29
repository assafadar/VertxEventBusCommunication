package org.crypto.communication.internal.notifications;

import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.Future;

public interface IMessageFailedNotification {
	void onMessageFailed(EventBusMessage errorMessage, Future<Object> future);
}
