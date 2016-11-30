/**
 * 
 */
package com.metlife.servicebus;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * @author rprasad017
 *
 */
public class Test {


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CustomSender sender = new CustomSender();
		try {
			sender.startPairing();
			
			String text = " This is a test message sent from Java";
			for (int i = 1; i <= 10; i++) {
				String msg = i + text;
				sender.send(msg);
			}
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
