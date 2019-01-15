package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.Map;

import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class EventBusMessageUtils {
	private static Map<String,Handler<AsyncResult<JsonObject>>> awatingResponseMessages = new HashMap<>();
	
	public static JsonObject getErrorMessage(Throwable e) {
		return new JsonObject().put("error", e)
								.put("msg", e.getMessage());
	}
	
	public static JsonObject getSuccessMessage(Object object) {
		return new JsonObject().put("data", object);
	}
	
	public static boolean isSuccessMessage(EventBusMessage message) {
		if(message == null || message.getData().containsKey("error")) {
			return false;
		}
		return true;
	}
	
	public static void addMessageHandler(String messageID, Handler<AsyncResult<JsonObject>> resultHandler) {
		awatingResponseMessages.put(messageID, resultHandler);
	}
	
	public static void removeMessageHandler(String messageID) {awatingResponseMessages.remove(messageID);}
	
	public static Handler<AsyncResult<JsonObject>> getMessageHandler(String messageID){
		return awatingResponseMessages.get(messageID);
	}

	public static boolean isAwaitingResponse(String messageID) {
		return awatingResponseMessages.containsKey(messageID);
	}
	
}
