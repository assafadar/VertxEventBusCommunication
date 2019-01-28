package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.crypto.communication.internal.notifications.NotificationService;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MembersManager {
	//Available servers map.
	private static Map<String, Map<HttpMethod, String>> eventBusClients;
	private static final String name = "EVENTBUS_RUOTER";
//	private static Function<String,Void> newClientListener;
	private static JsonArray clientNames;
	public static void init() {
		eventBusClients = new HashMap<>();
		clientNames = new JsonArray();
	}
	
	public static void addClient(String serverName, HttpMethod requestMethod,JsonObject handlers) {
		if(!eventBusClients.containsKey(serverName)) {
			eventBusClients.put(serverName, new HashMap<>());
		}
		JsonArray handlerMthods = handlers.getJsonArray("methods") ;
		handlerMthods.forEach(method -> {
			try {
				HttpMethod httpMethod = HttpMethod.valueOf((String)method);
				eventBusClients.get(serverName).put(httpMethod, serverName+handlerMthods);
			}catch (Exception e) {
				//Todo: logger - invalid http method was presented
			}
		});
		clientNames.add(serverName);
		NotificationService.sendNewClientNotification(serverName);
	}
	
	public static String getConsumberAddress(String serverName, HttpMethod requestMethod) {
		return eventBusClients.get(serverName).get(requestMethod);
	}
	
	public static void removeClient(String serverName) {
		eventBusClients.remove(serverName);
		int i;
		
		clientsLoop:
		for(i =0; i<clientNames.size(); i++) {
			if(clientNames.getString(i).equals(serverName)) {
				break clientsLoop;
			}
		}
		
		clientNames.remove(i);
	}
	
	public static JsonArray getAllClients(){
		return clientNames;
	}
	
	public static void clearRouter() {
		eventBusClients.clear();
	}
	
	public static String getDefaultName() {
		return name;
	}
	

	public static Map<String, Map<HttpMethod, String>> getAllRegistrations() {
		return eventBusClients;
	}
}
