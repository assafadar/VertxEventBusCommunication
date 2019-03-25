package org.crypto.communication.internal.router;

import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.net.EventBusNetworking;
import org.crypto.communication.internal.verticle.IEventBusUser;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public abstract class CommunicationAbstractRouter implements IEventBusUser {
	private RouterType type;
	protected Vertx vertx;
	private IEventBusRouter router;
	protected CommunicationAbstractRouter(RouterType type,Vertx vertx, Future<Void> deploymentFuture) {
		super();
		this.type = type;
		this.vertx = vertx;
		EventBusNetworking.init(vertx, this, deploymentFuture);
		initRouter();
	}
	
	private void initRouter() {
		this.router = EventBusNetworking.getNetworking().getRouter();
		this.router.get("message", this::routGetMessage);
		this.router.put("message", this::routPutMessage);
		this.router.post("message", this::routPostMessage);
		this.router.delete("message", this::routDeleteMessage);
	}
	
	protected abstract void routGetMessage(EventBusMessage message);
	protected abstract void routPostMessage(EventBusMessage message);
	protected abstract void routPutMessage(EventBusMessage message);
	protected abstract void routDeleteMessage(EventBusMessage message);
	
	public abstract void addClient();
	
	public abstract void removeClient();
	
	public abstract void disableClient(String clientAddress);
	
	public abstract void enableClient(String clientAddress);
}
