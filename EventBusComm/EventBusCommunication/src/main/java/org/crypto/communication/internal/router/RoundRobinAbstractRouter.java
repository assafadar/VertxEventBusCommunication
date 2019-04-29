package org.crypto.communication.internal.router;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.net.EventBusNetworking;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;

public abstract class RoundRobinAbstractRouter extends CommunicationAbstractRouter {
	List<String> instancesList;
	int instanceIndex;
	protected RoundRobinAbstractRouter(Vertx vertx, Future<Void> deploymentFuture) {
		super(RouterType.ROUND_ROBIN, vertx, deploymentFuture);
		this.instancesList = new ArrayList<>();
		this.instanceIndex = 0;
	}

	@Override
	protected void routGetMessage(EventBusMessage message) {
		try {
			String clientAddress = getAddress();
			sendMesasge(clientAddress, HttpMethod.GET, message);
			message.finishMessageReading();
		}catch (Exception e) {
			System.out.println("--------------------------- Faild in routGetMessage----------------"
					+" "+e.getMessage());
			message.messageReadingFailed(e);
		}
	}
	
	
	@Override
	protected void routPostMessage(EventBusMessage message) {
		try {
			String clientAddress = getAddress();
			sendMesasge(clientAddress, HttpMethod.POST, message);
			message.finishMessageReading();
		}catch (Exception e) {
			System.out.println("--------------------------- Faild in routPostMessage----------------"
					+" "+e.getMessage());
			message.messageReadingFailed(e);
		}
	}
	
	@Override
	protected void routPutMessage(EventBusMessage message) {
		try {
			String clientAddress = getAddress();
			sendMesasge(clientAddress, HttpMethod.PUT, message);
			message.finishMessageReading();
		}catch (Exception e) {
			System.out.println("--------------------------- Faild in routPutMessage----------------"
					+" "+e.getMessage());
			message.messageReadingFailed(e);
		}
	}
	
	@Override
	protected void routDeleteMessage(EventBusMessage message) {
		try {
			String clientAddress = getAddress();
			sendMesasge(clientAddress, HttpMethod.DELETE, message);
			message.finishMessageReading();
		}catch (Exception e) {
			System.out.println("--------------------------- Faild in routDeleteMessage----------------"
					+" "+e.getMessage());
			message.messageReadingFailed(e);
		}
	}
	
	private void sendMesasge(String address, HttpMethod method, EventBusMessage message)throws Exception{
		EventBusNetworking.getNetworking().sendMessage(address,method, message);
	}

	private String getAddress() {
		if(this.instanceIndex <= instancesList.size()) {
			this.instanceIndex = -1;
		}
		return this.instancesList.get((this.instanceIndex++));
	}

}
