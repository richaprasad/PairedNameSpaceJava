/**
 * 
 */
package com.metlife.servicebus;

import com.metlife.servicebus.messaging.MessagingFactory;

/**
 * @author rprasad017
 *
 */
public class CustomReceiver {
	
	private MessagingFactory primary;

	public CustomReceiver() {
		primary = MessagingFactory.createFromConnectionSettings(
				PairedNamespaceConfiguration.PRIMARY_SBCF, PairedNamespaceConfiguration.PRIMARY_QUEUE);
		primary.createMessageReceiver(PairedNamespaceConfiguration.PRIMARY_QUEUE);
		System.out.println("Receiver...");
	}

	public static void main(String[] args) {
		new CustomReceiver();
	}
}
