package com.metlife.servicebus.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;

import javax.jms.JMSException;

import com.metlife.servicebus.PairedNamespaceConfiguration;

/**
 * @author rprasad017
 *
 */
public class MessagingFactory {
	
	protected String connectionfactory;
	protected List<String> queueNames;
	protected MessageSender messageSender;
	
	protected Thread pingTask;
	protected Thread pairNamespaceTask;
	
	/**
	 * creates MessageFactory from connection factory and queue names
	 * @param cf
	 * @param path
	 * @return
	 */
	public static MessagingFactory createFromConnectionSettings(String cf, String... path) {
		MessagingFactory factory = new MessagingFactory();
		factory.connectionfactory = cf;
		factory.queueNames = new ArrayList<String>();
		for (String entity : path) {
			factory.queueNames.add(entity);
		}
		return factory;
	}
	
	/**
	 * Pairs two namespaces
	 * @param options
	 * @return
	 */
	public Thread pairNamespaceAsync(PairedNamespaceOptions options) {
		final SendAvailabilityPairedNamespaceOptions pairedNamespaceOptions = 
				(SendAvailabilityPairedNamespaceOptions) options;
		
		// Pair namespace task creation
		pairNamespaceTask = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized (pairNamespaceTask) {
					System.err.println("Inside Pair namespace");;
					int backlogQueueCount = pairedNamespaceOptions.getBacklogQueueCount();
					if (backlogQueueCount < 1)
					{
						// TODO Handle case where no queues were created.
					} else {
						// check if all backlog queues exists
						checkBacklogQueues();
					}
					System.err.println("Backlog queue created");
					
					//  create primary message sender
					messageSender = createMessageSender(PairedNamespaceConfiguration.PRIMARY_QUEUE);
					if(messageSender != null) {
						System.err.println("Message sender created");
						
						SendAvailabilityPairedNamespaceOptions.syphoneTask.start();
					}
					
					// Wait until primary becomes down
					try {
						pairNamespaceTask.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// Primary is down, start ping task
					// TODO check primary unavailable
					pingTask.start();
					// TODO stop/suspend syphon
					SendAvailabilityPairedNamespaceOptions.stopSyphon();
					
					String backlogQueue = chooseRandomBacklogQueue();
					pairedNamespaceOptions.secondaryMessagingFactory.messageSender = 
							pairedNamespaceOptions.secondaryMessagingFactory.createMessageSender(backlogQueue);
					
					try {
						pairNamespaceTask.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			/**
			 * Check if all backlog queues exists in namespace
			 */
			private void checkBacklogQueues() {
				queueNames = new ArrayList<String>();
				
				List<String> queueList = pairedNamespaceOptions.secondaryNamespaceManager.getQueueNames();
				
				/*for (int i = 0; i < pairedNamespaceOptions.backlogQueueCount; i++) {
					String queueName = PairedNamespaceConfiguration.PRIMARY_NAMESPACE 
							+ PairedNamespaceConfiguration.BACKLOG_QUEUE_EXT + i;
					if(queueList.contains(queueName)) {
						queueNames.add(queueName);
					} else {
						// throw error, backlog queue does not exists
						System.err.print("Backlog queue: " + queueName + " does not exists. Please create in portal.");
					    System.exit(-1);
					}
				}*/
				
				if(queueList.contains(PairedNamespaceConfiguration.BACKLOG_QUEUE1)) {
					queueNames.add(PairedNamespaceConfiguration.BACKLOG_QUEUE1);
				} else {
					// throw error, backlog queue does not exists
					System.err.print("Backlog queue: " + PairedNamespaceConfiguration.BACKLOG_QUEUE1 + " does not exists. Please create in portal.");
				    System.exit(-1);
				}
				if(queueList.contains(PairedNamespaceConfiguration.BACKLOG_QUEUE2)) {
					queueNames.add(PairedNamespaceConfiguration.BACKLOG_QUEUE2);
				} else {
					// throw error, backlog queue does not exists
					System.err.print("Backlog queue: " + PairedNamespaceConfiguration.BACKLOG_QUEUE2 + " does not exists. Please create in portal.");
				    System.exit(-1);
				}
			}

			private String chooseRandomBacklogQueue() {
				Random rand = new Random();
				int index = rand.nextInt(pairedNamespaceOptions.backlogQueueCount);	
				return pairedNamespaceOptions.secondaryMessagingFactory.queueNames.get(index);
			}
		});
		pairNamespaceTask.start();
		
		
		pingTask = new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					if(messageSender == null) {
						// TODO create sender for primary
					}
					while(!messageSender.pingMessage()) {
						try {
							Thread.sleep(pairedNamespaceOptions.pingPrimaryInterval.getTimeInMillis(new Date()));
						} catch (InterruptedException e1) {
							System.err.println(e1.getLocalizedMessage());
						}
					} 
					pairedNamespaceOptions.markPathHealthy(PairedNamespaceConfiguration.PRIMARY_QUEUE);
					notifyAll();
					pairedNamespaceOptions.onNotifyPrimarySendResult(PairedNamespaceConfiguration.PRIMARY_QUEUE, true);
				} catch (JMSException e) {
//					System.err.println(e.getLocalizedMessage());
					e.printStackTrace();
				}
			}
		});
		
		
		return pairNamespaceTask;
	}
	
	public MessageSender createMessageSender(String queue) {
		try {
			return new MessageSender(connectionfactory, queue);
		} catch (Exception e) {
//			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public MessageReceiver createMessageReceiver(String queue) {
		try {
			return new MessageReceiver(connectionfactory, queue);
		} catch (Exception e) {
//			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public void ResetConnection() {
		// TODO
	}

	/**
	 * @return the messageSender
	 */
	public MessageSender getMessageSender() {
		return messageSender;
	}

	/**
	 * @param messageSender the messageSender to set
	 */
	public void setMessageSender(MessageSender messageSender) {
		this.messageSender = messageSender;
	}
}
