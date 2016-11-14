/**
 * 
 */
package com.metlife.servicebus.messaging;

/**
 * @author rprasad017
 *
 */
public class NamespaceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public NamespaceException() {
	}

	/**
	 * @param message
	 */
	public NamespaceException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public NamespaceException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NamespaceException(String message, Throwable cause) {
		super(message, cause);
	}

}
