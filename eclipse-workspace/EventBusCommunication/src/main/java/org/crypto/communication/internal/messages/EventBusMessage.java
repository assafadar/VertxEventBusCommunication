package org.crypto.communication.internal.messages;

import org.crypto.communication.internal.utils.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
/**
 * 
 * @author asaf
 * @description - 
 * 		Structure of custom event bus message.
 */
public class EventBusMessage {
	private String messageID;
	private String sender;
	private JsonObject data;
	private String path;
	private Future<Object> readFuture;
	
	public EventBusMessage(String path, JsonObject data) {
		super();
		this.path = path;
		this.data = data;
		this.messageID = StringUtils.getRandomUUID(path);
	}
	
	public EventBusMessage(String path, JsonObject data,String sender) {
		super();
		this.sender = sender;
		this.path = path;
		this.data = data;
		this.messageID = StringUtils.getRandomUUID(path);
	}
	
	public EventBusMessage(String messageID, String path, JsonObject data) {
		super();
		this.path = path;
		this.data = data;
		this.messageID = messageID;
	}
	
	public EventBusMessage(String messageID, String path, JsonObject data,String sender) {
		super();
		this.sender = sender;
		this.path = path;
		this.data = data;
		this.messageID = messageID;
	}
	
	public void setReadFuture(Future<Object> future) {
		this.readFuture = future;
	}
	
	public Future<Object> getReadFuture() {
		return this.readFuture;
	}
	
	public void finishMessageReading(JsonObject result) {
		this.readFuture.complete(result);
	}
	
	public void finishMessageReading() {
		this.readFuture.complete();
	}
	
	public void messageReadingFailed(Throwable error) {
		this.readFuture.fail(error);
	}
	
	public void messageReadingFailed(String errorMessage) {
		this.readFuture.fail(errorMessage);
	}
	
	
	public EventBusMessage(EventBusMessage eventBusMessage) {
		this.sender = eventBusMessage.sender;
		this.path = eventBusMessage.path;
		this.data = eventBusMessage.data;
		this.messageID = eventBusMessage.messageID;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public JsonObject getData() {
		return data;
	}

	public void setData(JsonObject data) {
		this.data = data;
	}

	public String getMessageID() {
		return messageID;
	}
	
	
	
}
