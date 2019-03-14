package org.crypto.communication.internal.utils;

import java.util.List;

import org.crypto.communication.internal.messages.EventBusMessage;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
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

	public static EventBusMessage connectMessage() {
		JsonArray methods = new JsonArray()
				.add(HttpMethod.CONNECT).add(HttpMethod.POST).add(HttpMethod.GET)
				.add(HttpMethod.PUT).add(HttpMethod.DELETE).add(HttpMethod.OTHER);
		return new EventBusMessage("connect",new JsonObject().put("methods", methods));
	}
	
	public static JsonObject createJson(List<String>keys,List<Object>values) {
		JsonObject jsonObject = new JsonObject();
		for(int i=0; i<keys.size(); i++) {
			jsonObject.put(keys.get(i), values.get(i));
		}
		return jsonObject;
	}
	
	
}
