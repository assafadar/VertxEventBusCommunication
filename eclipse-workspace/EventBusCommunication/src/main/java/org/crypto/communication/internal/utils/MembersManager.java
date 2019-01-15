package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.crypto.communication.internal.notifications.NotificationService;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;

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
	
	public static void addClient(String serverName, HttpMethod requestMethod,String consumerAddress) {
		if(eventBusClients.containsKey(serverName)) {
			eventBusClients.get(serverName).put(requestMethod, consumerAddress);
		}else {
			Map<HttpMethod,String> newServer = new HashMap<>();
			newServer.put(requestMethod, consumerAddress);
			eventBusClients.put(serverName, newServer);
		}
		clientNames.add(serverName);
//		if(newClientListener != null && requestMethod == HttpMethod.CONNECT) {
//			newClientListener.apply(serverName);
//		}
		
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
