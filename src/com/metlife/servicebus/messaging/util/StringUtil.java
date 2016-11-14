package com.metlife.servicebus.messaging.util;


/**
 * @author rprasad017
 *
 */
public class StringUtil {

	public static boolean isNotNullOrEmpty(String value) {
		if(value == null) {
			return false;
		}
		if(value.trim().isEmpty()) {
			return false;
		}
		return true;
	}
}
