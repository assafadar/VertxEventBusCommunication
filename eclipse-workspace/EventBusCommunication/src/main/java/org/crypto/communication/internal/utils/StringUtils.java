package org.crypto.communication.internal.utils;

import java.util.UUID;

public class StringUtils {
	public static String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
	
	public static String getRandomUUID(String uniqeIdentifeir) {
		return	uniqeIdentifeir + getRandomUUID();
	}
}
