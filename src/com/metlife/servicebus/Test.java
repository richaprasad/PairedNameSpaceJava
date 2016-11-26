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
		
		// Start pair namespace task and wait until it completes successfully.
		Thread task = primary.pairNamespaceAsync(sendAvailabilityOptions);
		synchronized (task) {
			try {
				task.wait();           // wait() should always be in synch block
		    } catch (InterruptedException e) {
			    e.printStackTrace();
		    }
		}
		
		System.out.println("Task isAlive: " + task.isAlive());
		
		// Get Primary message sender
		MessageSender messageSender = primary.getMessageSender();
		
		String text = " This is a test message sent from Java";
		for (int i = 1; i <= 10; i++) {
			String msg = i + text;
			try {
				if(!MessagingFactory.primaryDown && (messageSender != null)) {
					messageSender = primary.getMessageSender();
				}
				
				messageSender.sendMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
				
				// If unable to send to primary, it means primary is down, 
				// start handle failure task and wait until it completes
				MessagingFactory.primaryDown = true;
				primary.handleFailureTask.start();
				
				synchronized(primary.handleFailureTask) {
					try {
						primary.handleFailureTask.wait();   //	wait until backlog queue selected
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
				// Get secondary message sender
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
		System.out.println("Receiver...");
	}

}
