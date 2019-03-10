package org.crypto.communication.internal.utils;

import java.util.HashMap;
import java.util.Map;

public class AppConfig {
	public static Map<String,String> appParams = new HashMap<String, String>();

	public static void setArgs(String[] args){
		for (int i = 0; i < args.length; i++) {
			System.out.println("Parameter: "+args[i]);
			String[] arg = args[i].split("=");
			appParams.put(arg[0], arg[1]);
		}

	}
}
