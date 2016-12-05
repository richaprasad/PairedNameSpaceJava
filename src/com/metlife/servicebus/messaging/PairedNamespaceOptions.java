/**
 * 
 */
package com.metlife.servicebus.messaging;

import javax.xml.datatype.Duration;

import com.metlife.servicebus.NamespaceManager;

/**
 * @author rprasad017
 * Pairing options
 */
public abstract class PairedNamespaceOptions {

	protected Duration failoverInterval;
	protected MessagingFactory secondaryMessagingFactory;
	protected NamespaceManager secondaryNamespaceManager;	
	
	public PairedNamespaceOptions(NamespaceManager namespaceManager, 
					MessagingFactory messagingFactory,
					Duration failoverInterval) {
		this.secondaryNamespaceManager = namespaceManager;
		this.secondaryMessagingFactory = messagingFactory;
		this.failoverInterval = failoverInterval;
	}
	
	protected void ClearPairing() {
		secondaryNamespaceManager = null;
		secondaryMessagingFactory = null;
	}
	
	protected abstract void onNotifyPrimarySendResult(String path, boolean success);

	/**
	 * @return the failoverInterval
	 */
	public Duration getFailoverInterval() {
		return failoverInterval;
	}

	/**
	 * @param failoverInterval the failoverInterval to set
	 */
	public void setFailoverInterval(Duration failoverInterval) {
		this.failoverInterval = failoverInterval;
	}

	/**
	 * @return the secondaryMessagingFactory
	 */
	public MessagingFactory getSecondaryMessagingFactory() {
		return secondaryMessagingFactory;
	}

	/**
	 * @param secondaryMessagingFactory the secondaryMessagingFactory to set
	 */
	public void setSecondaryMessagingFactory(
			MessagingFactory secondaryMessagingFactory) {
		this.secondaryMessagingFactory = secondaryMessagingFactory;
	}

	/**
	 * @return the secondaryNamespaceManager
	 */
	public NamespaceManager getSecondaryNamespaceManager() {
		return secondaryNamespaceManager;
	}

	/**
	 * @param secondaryNamespaceManager the secondaryNamespaceManager to set
	 */
	public void setSecondaryNamespaceManager(
			NamespaceManager secondaryNamespaceManager) {
		this.secondaryNamespaceManager = secondaryNamespaceManager;
	}
	
	
}
