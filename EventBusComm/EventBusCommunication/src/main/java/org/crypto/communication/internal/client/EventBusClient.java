package org.crypto.communication.internal.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
			Set<String> existingMembers =MembersManager.getAllClients(); 
			Set<String> members = new HashSet<>(existingMembers);
			String toRemove = "";
			for(String client : members) {
				if(client.equals(serverName)) {
					toRemove = client;
				}
			}
			members.remove(toRemove);
			EventBusMessage message = new EventBusMessage("connectToAll",new JsonObject().put("members", new ArrayList<>(members)));
			sendMessage(serverName, HttpMethod.CONNECT, message);
			future.complete();
		}catch (Exception e) {
			future.fail(e);
		}
	}
}
