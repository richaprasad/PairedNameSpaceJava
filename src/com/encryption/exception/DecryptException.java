/**
 * 
 */
package com.encryption.exception;

/**
 * @author rprasad017
 *
 */
public class DecryptException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public DecryptException() {
	}

	/**
	 * @param message
	 */
	public DecryptException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public DecryptException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DecryptException(String message, Throwable cause) {
		super(message, cause);
	}

}
