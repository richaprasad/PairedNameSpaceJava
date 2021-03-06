/**
 * 
 */
package com.servicebus;


/**
 * @author rprasad017
 * Constants used in Pairing namespace
 */
public class PairedNamespaceConfiguration {
	
	public static String SAS_KEY_NAME = "RootManageSharedAccessKey";
	
	public static String SECONDARY_NAMESPACE = "mljsbpocjw";
	public static String SECONDARY_SAS_KEY = "gumNzJ2JpwbULPpJoQl8VbeO8JZU+jTROhVOtkXbL2o=";
	public static String SECONDARY_ROOT_URI = ".servicebus.windows.net"; 
	
	public static final String BACKLOG_QUEUE_EXT = "x-servicebus-transfer-";
	public static final int BACKLOG_QUEUE_COUNT = 2;
	public static int FAILOVER_INTERVAL_SECONDS = 0;
	public static final long PING_INTERVAL = 60000;		// 1 minute
	
	/* ----   Servicebus properties       --------*/
	public static final String PRIMARY_SBCF = "SBCF";
	public static final String SECONDARY_SBCF = "SBCF2";
	
	public static final String PRIMARY_QUEUE = "QUEUE";
	public static final String SECONDARY_QUEUE1 = "QUEUE1";
	public static final String SECONDARY_QUEUE2 = "QUEUE2";
	
	public static final String PRIMARY_DEADQUEUE = "DEADQUEUE";
	public static final String SECONDARY_DEADQUEUE1 = "DEADQUEUE1";
	public static final String SECONDARY_DEADQUEUE2 = "DEADQUEUE2";
	/* ----   End of Servicebus properties       --------*/
	
}
