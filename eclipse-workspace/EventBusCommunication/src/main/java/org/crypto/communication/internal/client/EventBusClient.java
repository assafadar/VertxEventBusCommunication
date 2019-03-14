package org.crypto.communication.internal.client;

import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.utils.MembersManager;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class EventBusClient extends EventBusAbstractClient {

	public EventBusClient(Vertx vertx) {
		super(vertx,MembersManager.getDefaultName());
	}
	
	@Override
	public void onNewClient(String serverName,Future<Object> future) {
		try {
			JsonArray members = MembersManager.getAllClients().copy();
			for(int i=0; i<members.size(); i++) {
				if(members.getString(i).equals(serverName)) {
					members.remove(i);
				}
			}
			EventBusMessage message = new EventBusMessage("connectToAll",new JsonObject().put("members", members));
			sendMessage(serverName, HttpMethod.CONNECT, message);
			future.complete();
		}catch (Exception e) {
			future.fail(e);
		}
	}
}
