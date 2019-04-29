package org.crypto.communication.internal.server;

import org.crypto.communication.internal.utils.MembersManager;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

public class ServerImpl extends EventBusAbstractServer{
	
	public ServerImpl(Vertx vertx,Handler<AsyncResult<Void>> deploymentHandler){
		super(vertx, MembersManager.getDefaultName(),deploymentHandler);
	}
	
	public ServerImpl(String serverName,Vertx vertx,Handler<AsyncResult<Void>> deploymentHandler) {
		super(vertx,serverName,deploymentHandler);
	}
	
	
}
