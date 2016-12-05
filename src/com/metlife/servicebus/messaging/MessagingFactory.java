package com.metlife.servicebus.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Random;

import javax.jms.JMSException;
import javax.xml.datatype.Duration;

import com.metlife.servicebus.NamespaceManager;
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
	public Thread handleFailureTask;
//	public Thread switchToPrimaryTask;
	public Thread faultBehaviourTask;
	
	private static boolean initialized = false;
	public static boolean primaryDown = false;
	public static boolean secondaryUp = false;
	
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
					System.err.println("Inside Pair namespace");
					if(!initialized) {
						initializePairingTask(pairedNamespaceOptions);
						pairNamespaceTask.notify();
					}
				}
			}

			
		});
		pairNamespaceTask.start();
		
		pingTask = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized (pingTask) {
					while(true) {
						if(primaryDown) {
							// If primary sender is NULL, try to create a new sender
							handleCreatePrimarySender(pairedNamespaceOptions.pingPrimaryInterval);
							handlePing(pairedNamespaceOptions);
						} else {
							try {
								pingTask.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
		
		handleFailureTask = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized (handleFailureTask) {
					while(true) {
						if(primaryDown && !secondaryUp) {
							handleFailure(pairedNamespaceOptions);
							handleFailureTask.notify();
						} else {
							try {
								handleFailureTask.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
		
       /* switchToPrimaryTask = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized (switchToPrimaryTask) {
					while(true) {
						if(!primaryDown && secondaryUp) {
							switchToPrimaryNamespace(pairedNamespaceOptions);
							switchToPrimaryTask.notify();
						} else {
							try {
								switchToPrimaryTask.wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});*/
		
        faultBehaviourTask = new Thread(new Runnable() {
			
			@Override
			public void run() {
				synchronized (faultBehaviourTask) {
					// TODO fault behaviour
				}
			}
		});
        
		return pairNamespaceTask;
	}
	
	protected void handlePing(SendAvailabilityPairedNamespaceOptions pairedNamespaceOptions) {
		boolean pingSuccessful = false;
		while(!pingSuccessful) {
			try {
				pingSuccessful = messageSender.pingMessage();
				if(pingSuccessful) {
					break;
				}
			} catch (Exception ex) {
				if(ex instanceof IllegalStateException) {   // Primary sender is failed
					try {
						messageSender.close();      // Close the connection to release resources
					} catch (JMSException e) {
						e.printStackTrace();
					}
					messageSender = null;
					
					// Try to create new primary sender
					handleCreatePrimarySender(pairedNamespaceOptions.pingPrimaryInterval);
					try {
						pingSuccessful = messageSender.pingMessage();
					} catch (JMSException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				Thread.sleep(pairedNamespaceOptions.pingPrimaryInterval.getTimeInMillis(new Date()));
			} catch (InterruptedException e1) {
				System.err.println(e1.getLocalizedMessage());
			}
		} 
		pairedNamespaceOptions.onNotifyPrimarySendResult(PairedNamespaceConfiguration.PRIMARY_QUEUE, true);
		primaryDown = false; // Mark primary healthy
		pingTask.notify();
	}

	protected void handleCreatePrimarySender(Duration pingPrimaryInterval) {
		while(messageSender == null) {
			System.err.println("Message Sender is null, trying to create before ping...");
			messageSender = createMessageSender(PairedNamespaceConfiguration.PRIMARY_SBCF , PairedNamespaceConfiguration.PRIMARY_QUEUE);
			if(messageSender == null) {
				try {
					Thread.sleep(pingPrimaryInterval.getTimeInMillis(new Date()));
				} catch (InterruptedException e1) {
					System.err.println(e1.getLocalizedMessage());
				}
			}
		}
	}

	public MessageSender createMessageSender(String cf, String queue) {
		try {
			return new MessageSender(cf, queue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public MessageReceiver createMessageReceiver(String queue) {
		try {
			return new MessageReceiver(connectionfactory, queue);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void resetConnection() {
		// TODO Reset connection - Fault behavior
	}
	
	private void initializePairingTask(SendAvailabilityPairedNamespaceOptions pairedNamespaceOptions) {
		int backlogQueueCount = pairedNamespaceOptions.getBacklogQueueCount();
		if (backlogQueueCount < 1)
		{
			// TODO Handle case where no queues were created.
		} else {
			// check if all backlog queues exists
			checkBacklogQueues(pairedNamespaceOptions.secondaryNamespaceManager, pairedNamespaceOptions.backlogQueueCount);
			System.err.println("Backlog queue created");
		}
		
		//  create primary message sender
		messageSender = createMessageSender(PairedNamespaceConfiguration.PRIMARY_SBCF, PairedNamespaceConfiguration.PRIMARY_QUEUE);  
		if(messageSender != null) {
			System.err.println("Message sender created");
			
			// If primary sender creation is successful, start syphon
			SendAvailabilityPairedNamespaceOptions.syphoneTask.start();
		}
		initialized = true;
	}
	
	private void handleFailure(SendAvailabilityPairedNamespaceOptions pairedNamespaceOptions) {
		// Primary is down, start ping task
		System.out.println("PingTask state: " + pingTask.getState());
		if(pingTask.getState() == Thread.State.NEW) {
			pingTask.start();
		} /*else if(pingTask.getState() == Thread.State.WAITING) {
			pingTask.notify();
		}*/
		
		// Stop Syphon process
		if(SendAvailabilityPairedNamespaceOptions.syphons != null) {
			SendAvailabilityPairedNamespaceOptions.stopSyphon();
		}
		
		// Create Secondary Message Sender
		while(pairedNamespaceOptions.secondaryMessagingFactory.messageSender == null) {
			String backlogQueue = chooseRandomBacklogQueue(pairedNamespaceOptions);
			pairedNamespaceOptions.secondaryMessagingFactory.messageSender = 
					pairedNamespaceOptions.secondaryMessagingFactory.createMessageSender(PairedNamespaceConfiguration.SECONDARY_SBCF, backlogQueue);
		}
		secondaryUp = true;
	}
	
	/**
	 * Check if all backlog queues exists in namespace
	 * @param backlogQueueCount 
	 */
	private void checkBacklogQueues(NamespaceManager manager, int backlogQueueCount) {
		queueNames = new ArrayList<String>();
		
		List<String> queueList = manager.getQueueNames();
		
		for (int i = 0; i < backlogQueueCount; i++) {
			String queueName = PairedNamespaceConfiguration.BACKLOG_QUEUE_EXT + i;
			if(queueList.contains(queueName)) {
				queueNames.add(queueName);
			} else {
				// throw error, backlog queue does not exists
				System.err.print("Backlog queue: " + queueName + " does not exists. Please create in portal.");
			    System.exit(-1); // TODO Fault behavior
			}
		}
	}

	private String chooseRandomBacklogQueue(SendAvailabilityPairedNamespaceOptions pairedNamespaceOptions) {
		Random rand = new Random();
		int index = rand.nextInt(pairedNamespaceOptions.backlogQueueCount);	
		return pairedNamespaceOptions.secondaryMessagingFactory.queueNames.get(index);
	}
	
	/*private void switchToPrimaryNamespace(SendAvailabilityPairedNamespaceOptions pairedNamespaceOptions) {
		// Start Syphon process
		if(SendAvailabilityPairedNamespaceOptions.syphons == null) {
			SendAvailabilityPairedNamespaceOptions.stopSyphon();
		}
		secondaryUp = false;
	}*/
	
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
