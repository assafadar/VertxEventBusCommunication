package org.crypto.communication.internal.server;

import org.crypto.communication.internal.utils.MembersManager;
import io.vertx.core.Vertx;

public class ServerImpl extends EventBusAbstractServer{
	
	public ServerImpl(Vertx vertx){
		super(vertx, MembersManager.getDefaultName());
	}
	
	public ServerImpl(String serverName,Vertx vertx) {
		super(vertx,serverName);
	}
	
	
}
