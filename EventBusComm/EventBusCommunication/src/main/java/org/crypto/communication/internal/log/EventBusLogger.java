package org.crypto.communication.internal.log;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import io.vertx.core.Vertx;
/*
 * Custom logger for EventBus 
 * @author: Asaf.
 * @Date: 24/3/19
 */

public class EventBusLogger {
	private static Map<Class<? extends Object>,Logger> loggers = new HashMap<Class<? extends Object>, Logger>();
	
	public static void createLogger(Class<? extends Object> clzz, String logFilePath, Level logLevel
			,Vertx vertx,String verticleName) {
		if(!loggers.containsKey(clzz)) {
			initializeLogger(clzz, logFilePath,logLevel,verticleName);
		}
		
	}
	public static void createLogger(Class<? extends Object> clzz, Level logLevel,Vertx vertx,String verticleName) {
		if(!loggers.containsKey(clzz)) {
			initializeLogger(clzz, "",logLevel,verticleName);
		}
	}
	
	//Todo: need to find a way to stop duplicating log docs.
	private static void initializeLogger(Class<? extends Object> clzz, String logFilePath, Level logLevel,String verticleName) {
		Logger logger = Logger.getLogger(clzz.getSimpleName());
		FileHandler fileHandler;
		try {
//			fileHandler = new FileHandler(logFilePath+verticleName+"-"+clzz+".log",false);
//			fileHandler.setFormatter(new SimpleFormatter());
//			fileHandler.flush();
//			logger.addHandler(fileHandler);
			logger.setLevel(logLevel);
		}catch (Exception e) {
			throw new IllegalStateException("Log creating failed: "+e.getMessage(),e);
		}
		loggers.put(clzz, logger);
		logger.severe("Logger for: "+clzz.getSimpleName()+" started on level: "+logLevel.getName());
	}
	
	private static Logger getLogger(Class<? extends Object> clzz) {
		return loggers.get(clzz);
	}
	
	public static void ERROR(Class<? extends Object> clzz, Throwable error, Level classLogLevel) {
		getLogger(clzz).log(classLogLevel,error.getMessage(),error);
	}
	
	public static void ERROR(Class<? extends Object> clzz, Throwable error,String message, Level classLogLevel) {
//		INFO(clzz, message, classLogLevel);
		ERROR(clzz, error, classLogLevel);
	}
	
	public static void INFO(Class<? extends Object> clzz, String message, Level classLogLevel) {
		getLogger(clzz).log(classLogLevel,message);
	}
	
	public static void close() {
		//Should close all the file handlers.
		for(Class<?> cl : loggers.keySet()) {
			Handler[] handlers = loggers.get(cl).getHandlers();
			for(Handler h : handlers) {
				h.close();
			}
		}
		//Should stop the duplication of files by reseting the connections.
		LogManager.getLogManager().reset();
	}
	
}