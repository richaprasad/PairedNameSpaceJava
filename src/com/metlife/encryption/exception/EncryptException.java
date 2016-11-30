/**
 * 
 */
package com.metlife.encryption.exception;

/**
 * @author rprasad017
 *
 */
public class EncryptException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public EncryptException() {
	}

	/**
	 * @param message
	 */
	public EncryptException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public EncryptException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EncryptException(String message, Throwable cause) {
		super(message, cause);
	}

}
