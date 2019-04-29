package org.crypto.communication.internal.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.crypto.communication.internal.log.EventBusLogger;
import org.crypto.communication.internal.messages.EventBusMessage;
import org.crypto.communication.internal.utils.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NotificationService {
	private final static Logger logger = LoggerFactory.getLogger(NotificationService.class);
	private static List<IMessageFailedNotification> messageFailedObservers = new ArrayList<>();
	private static List<IMessageSuccessNotification> messageSuccessObservers = new ArrayList<>();
	private static List<INewClientNotification> newClientsObservers = new ArrayList<>();
	
	
	public static void addMessageFailedObservers(IMessageFailedNotification observer) {
		messageFailedObservers.add(observer);
	}
	
	public static void addNewClientObservers(INewClientNotification observer) {
		newClientsObservers.add(observer);
	}
	
	
	public static void addMessageSuccessObservers(IMessageSuccessNotification observer) {
		messageSuccessObservers.add(observer);
	}
	
	
	public static void sendMessageFailedNotfication(EventBusMessage errorMessage, Vertx vertx) {
		messageFailedObservers.forEach(observer -> {
			WorkerExecutor executor = vertx.createSharedWorkerExecutor
					("MESSAGE_FAILED_NOTIF_"+observer.getClass().getName()+StringUtils.getRandomUUID());
			executor.executeBlocking(future -> {
				try {
					observer.onMessageFailed(errorMessage,future);
				}catch (Exception e) {
					future.fail(e);
				}finally {
					executor.close();
				}
			}, resultHandler -> {
				if(resultHandler.succeeded()) {
					logger.info("Message failure notification success: "+observer.getClass().getName());
				}else {
					logger.error("Message failure notification failed: "+observer.getClass().getName()
							+" due to: "+resultHandler.cause().getMessage(),resultHandler.cause());
				}
			});
		});
	}
	
	public static void sendMessageSuccessNotification(EventBusMessage successMessage,Vertx vertx) {
		messageSuccessObservers.forEach(observer -> {
			WorkerExecutor executor = vertx.createSharedWorkerExecutor
					("MESSAGE_SUCCESS_NOTIF_"+observer.getClass().getName()+StringUtils.getRandomUUID());
			executor.executeBlocking(future -> {
				try {
					observer.onMessageSucceeded(successMessage,future);
				}catch (Exception e) {
					future.fail(e);
				}finally {
					executor.close();
				}
			}, resultHandler -> {
				if(resultHandler.succeeded()) {
					logger.info("Message success notification success: "+observer.getClass().getName());
				}else {
					logger.error("Message success notification failed: "+observer.getClass().getName()
							+" due to: "+resultHandler.cause().getMessage(),resultHandler.cause());
				}
			});
		});
	}
	
	public static void sendNewClientNotification(String serverName, Vertx vertx) {
		newClientsObservers.forEach(observer -> {
			WorkerExecutor executor = 
					vertx.createSharedWorkerExecutor("NEW_CLIENT_NOTIF_"+observer.getClass().getSimpleName());
			executor.executeBlocking(future -> {
				System.out.println("---------------------------------Sent new client notif to: "+observer.getClass().getSimpleName()+"----------------------");
				observer.onNewClient(serverName,future);
			}, resultHandler -> {
				if(resultHandler.succeeded()) {
					EventBusLogger.INFO(observer.getClass(), "New client added", Level.SEVERE);
				}
			});
		});
	}
}
