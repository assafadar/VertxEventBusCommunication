package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.Map;

import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class EventBusMessageUtils {
	
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

	public static EventBusMessage connectMessage(String sender) {
		// TODO Auto-generated method stub
		return new EventBusMessage(sender,"connect",new JsonObject());
	}
	
	
	
	
}
