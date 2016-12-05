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
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		CustomSender sender = new CustomSender();
		try {
			sender.startPairing();
			
			String text = " This is a test message sent from Java";
			for (int i = 1; i <= 20; i++) {
				String msg = i + text;
				sender.send(msg);
				
				Thread.sleep(1000);
			}
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		
	}

}
