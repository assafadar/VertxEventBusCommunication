package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.Map;
import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.notifications.NotificationService;
import org.crypto.communication.internal.verticle.EventBusVerticle;

import java.util.logging.Level;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MembersManager {
	private final static Level LOG_LEVEL = Level.SEVERE;
	//Available servers map.
	private static Map<String, Map<HttpMethod, String>> eventBusClients;
	private static final String name = EventBusVerticle.class.getSimpleName();
	private static JsonArray clientNames;
	public static void init() {
		eventBusClients = new HashMap<>();
		clientNames = new JsonArray();
		registerDefaultClient();
	}
	

	private static void registerDefaultClient() {
		eventBusClients.put(getDefaultName(), new HashMap<>());
		eventBusClients.get(getDefaultName()).put(HttpMethod.CONNECT, getDefaultName()+HttpMethod.CONNECT);
	}


	public static void addClient(String serverName,JsonObject handlers,Vertx vertx) {
		if(!eventBusClients.containsKey(serverName)) {
			eventBusClients.put(serverName, new HashMap<>());
		}
		JsonArray handlerMthods = handlers.getJsonArray("methods") ;
		
		handlerMthods.forEach(method -> {
			try {
				HttpMethod httpMethod = HttpMethod.valueOf((String)method);
				eventBusClients.get(serverName).put(httpMethod, serverName+method);
			}catch (Exception e) {
				EventBusLogger.ERROR(MembersManager.class,e,"Failed loading method: "+method, LOG_LEVEL);
			}
		});
		clientNames.add(serverName);
		NotificationService.sendNewClientNotification(serverName,vertx);
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
