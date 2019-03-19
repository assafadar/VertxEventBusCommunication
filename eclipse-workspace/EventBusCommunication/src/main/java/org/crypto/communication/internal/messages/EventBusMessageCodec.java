package org.crypto.communication.internal.messages;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;

public class EventBusMessageCodec implements MessageCodec<EventBusMessage, EventBusMessage> {

	@Override
	public void encodeToWire(Buffer buffer, EventBusMessage message) {
		JsonObject encodeJSON = new JsonObject().put("sender", message.getSender()).put("data", message.getData())
				.put("path", message.getPath()).put("messageID", message.getMessageID());

		String jsonAsString = encodeJSON.encode();

		int length = jsonAsString.getBytes().length;

		buffer.appendInt(length);
		buffer.appendString(jsonAsString);
	}

	@Override
	public EventBusMessage decodeFromWire(int pos, Buffer buffer) {
		int length = buffer.getInt(pos);
		String jsonSTR = buffer.getString(pos += 4, pos += length);
		JsonObject content = new JsonObject(jsonSTR);

		return new EventBusMessage(content.getString("messageID"), content.getString("path"),
				content.getJsonObject("data"));
	}

	@Override
	public EventBusMessage transform(EventBusMessage message) {
		// TODO Auto-generated method stub
		return message;
	}

	@Override
	public String name() {
		return this.getClass().getSimpleName();
	}

	@Override
	public byte systemCodecID() {
		// TODO Auto-generated method stub
		return -1;
	}

}
