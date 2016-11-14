/**
 * 
 */
package com.metlife.servicebus;

import javax.jms.JMSException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import com.metlife.servicebus.messaging.MessageSender;
import com.metlife.servicebus.messaging.MessagingFactory;
import com.metlife.servicebus.messaging.NamespaceException;
import com.metlife.servicebus.messaging.SendAvailabilityPairedNamespaceOptions;

/**
 * @author rprasad017
 *
 */
public class Test {

	/**
	 * @param args
	 * @throws DatatypeConfigurationException 
	 * @throws JMSException 
	 */
	public static void main(String[] args) throws DatatypeConfigurationException, JMSException {
		MessagingFactory primary = MessagingFactory.createFromConnectionSettings(
				PairedNamespaceConfiguration.PRIMARY_SBCF, PairedNamespaceConfiguration.PRIMARY_QUEUE);
		
		MessagingFactory secondaryMessagingFactory =  MessagingFactory.createFromConnectionSettings(
				PairedNamespaceConfiguration.SECONDARY_SBCF, 
				PairedNamespaceConfiguration.SECONDARY_QUEUE1,
				PairedNamespaceConfiguration.SECONDARY_QUEUE2);
		
		NamespaceManager secondaryNamespaceManager = NamespaceManager.createFromConfig(
				PairedNamespaceConfiguration.SECONDARY_NAMESPACE,
				PairedNamespaceConfiguration.SAS_KEY_NAME,
				PairedNamespaceConfiguration.SECONDARY_SAS_KEY,
				PairedNamespaceConfiguration.SECONDARY_ROOT_URI);
		
		Duration failoverInterval = DatatypeFactory.newInstance().newDuration(0);
		
		SendAvailabilityPairedNamespaceOptions sendAvailabilityOptions = 
				new SendAvailabilityPairedNamespaceOptions(
						secondaryNamespaceManager, secondaryMessagingFactory,
						PairedNamespaceConfiguration.BACKLOG_QUEUE_COUNT,
						failoverInterval,
						true);
		
		final Object lock = new Object();
		Thread task = null;
		synchronized (lock) {
			task = primary.pairNamespaceAsync(sendAvailabilityOptions);
			try {
				lock.wait(10000);	//	wait until pairNamespace task completes
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		MessageSender messageSender = primary.getMessageSender();
//		messageSender.pingMessage();
		
		String text = " This is a test message sent from Java";
		for (int i = 0; i < 10; i++) {
			String msg = i + text;
			try {
				messageSender.sendMessage(msg);
			} catch (Exception e) {
//				System.err.println(e.getMessage());
				e.printStackTrace();
				synchronized(task) {
					task.notifyAll();
					try {
						task.wait(10000);	//	wait until backlog queue selected
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				messageSender = sendAvailabilityOptions.getSecondaryMessagingFactory().getMessageSender();
				
				try {
					messageSender.sendMessage(msg);
				} catch (NamespaceException e1) {
					e1.printStackTrace();
				}
			}
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		primary.createMessageReceiver(PairedNamespaceConfiguration.PRIMARY_QUEUE);
	}

}
