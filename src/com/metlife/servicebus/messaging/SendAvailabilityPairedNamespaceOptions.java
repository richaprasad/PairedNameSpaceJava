/**
 * 
 */
package com.metlife.servicebus.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import com.metlife.servicebus.NamespaceManager;
import com.metlife.servicebus.PairedNamespaceConfiguration;
import com.metlife.servicebus.messaging.util.StringUtil;

/**
 * @author rprasad017
 *
 */
public class SendAvailabilityPairedNamespaceOptions extends	PairedNamespaceOptions {
	
	protected int backlogQueueCount;
	protected boolean enableSyphon;
	protected Duration pingPrimaryInterval;

	public SendAvailabilityPairedNamespaceOptions(
			NamespaceManager namespaceManager, MessagingFactory messagingFactory) {
		super(namespaceManager, messagingFactory);
		try {
			pingPrimaryInterval = DatatypeFactory.newInstance().newDuration(60000);	// 1 minute
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	public SendAvailabilityPairedNamespaceOptions(
		    NamespaceManager secondaryNamespaceManager,
		    MessagingFactory messagingFactory,
		    int backlogQueueCount,
		    Duration failoverInterval,
		    boolean enableSyphon) {
		super(secondaryNamespaceManager, messagingFactory, failoverInterval);
		this.backlogQueueCount = backlogQueueCount;
		this.enableSyphon = enableSyphon;
		try {
			pingPrimaryInterval = DatatypeFactory.newInstance().newDuration(60000);	// 1 minute
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see com.metlife.servicebus.messaging.PairedNamespaceOptions#onNotifyPrimarySendResult(java.lang.String, boolean)
	 */
	@Override
	protected void onNotifyPrimarySendResult(String path, boolean success) {
		if(success && syphons == null) {
			syphoneTask.start();
		}
	}
	
	public void markPathHealthy(String path) {
		if(StringUtil.isNotNullOrEmpty(path)) {
			if(path.equalsIgnoreCase(PairedNamespaceConfiguration.PRIMARY_QUEUE)) {
				// TODO mark path healthy
			}
		}
	}
	
	/**
	 * @return the backlogQueueCount
	 */
	public int getBacklogQueueCount() {
		return backlogQueueCount;
	}

	/**
	 * @param backlogQueueCount the backlogQueueCount to set
	 */
	public void setBacklogQueueCount(int backlogQueueCount) {
		this.backlogQueueCount = backlogQueueCount;
	}

	/**
	 * @return the enableSyphon
	 */
	public boolean isEnableSyphon() {
		return enableSyphon;
	}

	/**
	 * @param enableSyphon the enableSyphon to set
	 */
	public void setEnableSyphon(boolean enableSyphon) {
		this.enableSyphon = enableSyphon;
	}

	/**
	 * @return the pingPrimaryInterval
	 */
	public Duration getPingPrimaryInterval() {
		return pingPrimaryInterval;
	}

	/**
	 * @param pingPrimaryInterval the pingPrimaryInterval to set
	 */
	public void setPingPrimaryInterval(Duration pingPrimaryInterval) {
		this.pingPrimaryInterval = pingPrimaryInterval;
	}
	public static List<SyphonProcess> syphons = null;
	
	public static Thread syphoneTask = new Thread(new Runnable() {
		
		@Override
		public void run() {
			syphons = createSyphon();
		}
	});
	
    public static Thread stopSyphoneTask = new Thread(new Runnable() {
		
		@Override
		public void run() {
			if(syphons != null) {
				stopSyphon();
			}
		}
	});
	
	/**
	 * Creates syphon process
	 * @return
	 */
	private static List<SyphonProcess> createSyphon() {
		List<SyphonProcess> processes = new ArrayList<SyphonProcess>();
		
		try {
			SyphonProcess syphonProcess1 = new SyphonProcess(
					PairedNamespaceConfiguration.SECONDARY_SBCF, 
					PairedNamespaceConfiguration.SECONDARY_QUEUE1);
			
			SyphonProcess syphonProcess2 = new SyphonProcess(
					PairedNamespaceConfiguration.SECONDARY_SBCF, 
					PairedNamespaceConfiguration.SECONDARY_QUEUE2);
			
			processes.add(syphonProcess1);
			processes.add(syphonProcess2);
			return processes;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Stop syphon process
	 */
	public static void stopSyphon() {
		if(syphons != null) {
			System.out.println("Stopping Syphon...");
			for ( SyphonProcess syphon : syphons) {
				try {
					syphon.closeSyphon();
					syphon = null;
				} catch (JMSException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
