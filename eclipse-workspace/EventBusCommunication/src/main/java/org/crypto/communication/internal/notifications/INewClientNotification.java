package org.crypto.communication.internal.notifications;

import io.vertx.core.Future;

public interface INewClientNotification {

	void onNewClient(String serverName, Future<Object> future);
}
