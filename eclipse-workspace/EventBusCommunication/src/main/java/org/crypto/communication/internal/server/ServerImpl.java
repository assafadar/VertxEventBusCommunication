package org.crypto.communication.internal.server;

import org.crypto.communication.internal.utils.MembersManager;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ServerImpl extends EventBusAbstractServer{
	
	public ServerImpl(Vertx vertx,Future<Void> deploymentFuture){
		super(vertx, MembersManager.getDefaultName(),deploymentFuture);
	}
	
	public ServerImpl(String serverName,Vertx vertx,Future<Void> deploymentFuture) {
		super(vertx,serverName,deploymentFuture);
	}
	
	
}
