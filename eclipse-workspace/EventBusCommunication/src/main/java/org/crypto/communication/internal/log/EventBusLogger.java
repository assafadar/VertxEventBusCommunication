package org.crypto.communication.internal.log;

import java.util.logging.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;

import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class EventBusLogger {
	private static Map<Class<? extends Object>,Logger> loggers = new HashMap<Class<? extends Object>, Logger>();
	
	public static void createLogger(Class<? extends Object> clzz, String logFilePath, Level logLevel) {
		if(!loggers.containsKey(clzz)) {
			initializeLogger(clzz, logFilePath,logLevel);
		}
		
	}
	public static void createLogger(Class<? extends Object> clzz, Level logLevel) {
		if(!loggers.containsKey(clzz)) {
			initializeLogger(clzz, "",logLevel);
		}
	}
	
	private static void initializeLogger(Class<? extends Object> clzz, String logFilePath, Level logLevel) {
		Logger logger = Logger.getLogger(clzz.getSimpleName());
		FileHandler fileHandler;
		try {
			fileHandler = new FileHandler(logFilePath+clzz+".log",true);
			fileHandler.setFormatter(new SimpleFormatter());
			logger.addHandler(fileHandler);
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
		INFO(clzz, message, classLogLevel);
		ERROR(clzz, error, classLogLevel);
	}
	
	public static void INFO(Class<? extends Object> clzz, String message, Level classLogLevel) {
		getLogger(clzz).log(classLogLevel,message);
	}
	
	
	
	

}