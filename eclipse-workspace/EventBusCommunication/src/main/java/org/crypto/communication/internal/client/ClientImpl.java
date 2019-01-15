package org.crypto.communication.internal.client;


import org.crypto.communication.internal.utils.MembersManager;
import io.vertx.core.Vertx;

 public class ClientImpl extends EventBusAbstractClient {
	
	public ClientImpl(Vertx vertx) {
		super(vertx,MembersManager.getDefaultName());
	}
	
	public ClientImpl(String clientName,Vertx vertx) {
		super(vertx,clientName);
	}

	

	

	

}
