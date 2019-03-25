package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.notifications.NotificationService;
import org.crypto.communication.internal.verticle.EventBusVerticle;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MembersManager {
	private final static Level LOG_LEVEL = Level.SEVERE;
	// Available servers map.
	private static Map<String, Map<HttpMethod, String>> eventBusClients;
	private static final String name = EventBusVerticle.class.getSimpleName();
	private static Set<String> clientNames;

	public static void init(Vertx vertx) {
		eventBusClients = new HashMap<>();
		clientNames = new HashSet<>();
		registerDefaultClient();
		EventBusLogger.createLogger(MembersManager.class, LOG_LEVEL, vertx,"Members Manager");
	}

	private static void registerDefaultClient() {
		eventBusClients.put(getDefaultName(), new HashMap<>());
		eventBusClients.get(getDefaultName()).put(HttpMethod.CONNECT, getDefaultName() + HttpMethod.CONNECT);
		eventBusClients.get(getDefaultName()).put(HttpMethod.GET, getDefaultName() + HttpMethod.GET);

	}

	public static void addClient(String serverName, JsonObject handlers, Vertx vertx) {
		if (!eventBusClients.containsKey(serverName)) {
			eventBusClients.put(serverName, new HashMap<>());
		}
		JsonArray handlerMthods = handlers.getJsonArray("methods");

		handlerMthods.forEach(method -> {
			try {
				HttpMethod httpMethod = HttpMethod.valueOf((String) method);
				eventBusClients.get(serverName).put(httpMethod, serverName + method);
			} catch (Exception e) {
				EventBusLogger.ERROR(MembersManager.class, e, "Failed loading method: " + method, LOG_LEVEL);
			}
		});
		clientNames.add(serverName);
		EventBusLogger.INFO(MembersManager.class,
				serverName + " client registered consumers " + handlerMthods.toString(), LOG_LEVEL);
		NotificationService.sendNewClientNotification(serverName, vertx);
		System.out.println("Client added: " + serverName);
	}

	public static String getConsumberAddress(String serverName, HttpMethod requestMethod) throws Exception {
		isAddressExists(serverName, requestMethod);
		return eventBusClients.get(serverName).get(requestMethod);
	}

	private static void isAddressExists(String serverName, HttpMethod requestMethod) {
		if (!eventBusClients.containsKey(serverName) || !eventBusClients.get(serverName).containsKey(requestMethod)) {
			EventBusLogger.ERROR(MembersManager.class, new Exception("Address does not exists"), LOG_LEVEL);
			System.out.println("==================Address: " + serverName + requestMethod
					+ " does not exists================================");
			throw new IllegalStateException("Address does not exists");
		}
	}

	public static void removeClient(String serverName) {
		eventBusClients.remove(serverName);
		int i;

		clientsLoop: for (String client : clientNames) {
			if (clientNames.equals(serverName)) {
				break clientsLoop;
			}

			clientNames.remove(client);
		}

	}

	public static Set<String> getAllClients() {
		System.out.println(clientNames.toString());
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
