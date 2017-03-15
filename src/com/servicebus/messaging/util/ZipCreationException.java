/**
 * 
 */
package com.servicebus.messaging.util;

/**
 * @author rprasad017
 *
 */
public class ZipCreationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ZipCreationException() {
	}

	/**
	 * @param message
	 */
	public ZipCreationException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public ZipCreationException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public ZipCreationException(String message, Throwable cause) {
		super(message, cause);
	}

}
