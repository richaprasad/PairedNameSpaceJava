/**
 * 
 */
package com.metlife.servicebus;

import java.io.IOException;

import javax.jms.JMSException;
import javax.naming.NamingException;

import com.metlife.servicebus.messaging.MessageReceiver;

/**
 * @author rprasad017
 *
 */
public class CustomReceiver {
	
	public static void main(String[] args) throws IOException, NamingException, JMSException {
		new MessageReceiver(PairedNamespaceConfiguration.PRIMARY_SBCF, 
				PairedNamespaceConfiguration.PRIMARY_QUEUE);
		System.out.println("Receiver...");
	}
}
