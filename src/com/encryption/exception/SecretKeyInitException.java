/**
 * 
 */
package com.encryption.exception;

/**
 * @author rprasad017
 *
 */
public class SecretKeyInitException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SecretKeyInitException() {
	}

	/**
	 * @param message
	 */
	public SecretKeyInitException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SecretKeyInitException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SecretKeyInitException(String message, Throwable cause) {
		super(message, cause);
	}

}
