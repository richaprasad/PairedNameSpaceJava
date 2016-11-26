/**
 * 
 */
package com.metlife.servicebus.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.metlife.servicebus.PairedNamespaceConfiguration;
import com.metlife.servicebus.messaging.util.AppConstants;


/**
 * @author rprasad017
 * <p>Class to send message to Azure Service Bus using AMQP 1.0</p>
 */
public class MessageSender implements ExceptionListener {
	
	private ConnectionFactory cf;
	private Destination queue;
	private Connection connection;
    private Session sendSession;
    private MessageProducer sender;
    private String connectionfactory;
    private String queueName;
    
    private static Random randomGenerator = new Random();
    private static final int MAX_RETRY = 5;
	
	public MessageSender(String connectionfactory, String queueName) throws IOException, NamingException, JMSException {
		this.connectionfactory = connectionfactory;
		this.queueName = queueName;
		
		initSender();
	}

	private void initSender() throws IOException, NamingException, JMSException {
		// Configure JNDI environment
    	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    	InputStream input = classLoader.getResourceAsStream("servicebus.properties");
    	
    	Properties properties = new Properties();
    	properties.load(input);
        Context context = new InitialContext(properties);

        // Lookup ConnectionFactory and Queue
        cf = (ConnectionFactory) context.lookup(connectionfactory);
        queue = (Destination) context.lookup(queueName);
        
        initializeConnection();
	}

	/**
	 * Initialize connection
	 * @throws JMSException
	 */
	private void initializeConnection() throws JMSException {
		// Create Connection
        connection = cf.createConnection();
        connection.setExceptionListener(this);
        
        // Create sender-side Session and MessageProducer
        sendSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        sender = sendSession.createProducer(queue);
	}

	/**
	 * Send messages to Azure Service Bus
	 * @param msg
	 * @throws JMSException
	 * @throws NamespaceException 
	 */
	public void sendMessage(String msg) throws JMSException, NamespaceException {
		if(msg.trim().length() > 0 ) {
			// Create message
			TextMessage message = sendSession.createTextMessage();
			message.setJMSMessageID("ID:" + generateMessageId());
			message.setText(msg);
			sendMessage(message);
		} 
    }
	

	/**
	 * Send messages to Azure Service Bus
	 * @param msg
	 * @throws JMSException
	 * @throws NamespaceException 
	 */
	public void sendMessage(TextMessage message) throws JMSException, NamespaceException {
		try {
			sender.send(message);				// Send message to service bus
			System.out.println("Sent message with JMSMessageID = " + message.getJMSMessageID());
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			handleException(e, message);
		} finally {
			copyProperties(message);
		}
		message = null;
    }

	private void handleException(Exception e, TextMessage message) throws NamespaceException {
		int retryCount = 1;
		if((e instanceof TimeoutException) || (e instanceof JMSException)) {
			retrySendMessage(message, retryCount);	// Retry 
		} 
//		else if(e instanceof InvalidDestinationException) {}	
		else {
			// recreate JMS Connection, Session, and MessageProducer
			try {
				initializeConnection();
				retrySendMessage(message, retryCount);	// Retry 
			} catch (JMSException ex) {
				System.err.println(ex.getLocalizedMessage());
				// TODO paired name space
				throw new NamespaceException("Namespace is down");
			}
		}
	}

	/**
	 * Retry send message until retry limit exhausts
	 * @param message
	 * @param retry
	 * @throws NamespaceException 
	 */
	private void retrySendMessage(TextMessage message, int retry) throws NamespaceException {
		try {
			retry++;
			sender.send(message);
			System.out.println("Sent message with JMSMessageID = " + message.getJMSMessageID());
		} catch (Exception e) {
			if((e instanceof TimeoutException) || (e instanceof JMSException)) {
				// Retry logic
				if(retry < MAX_RETRY) {
					retrySendMessage(message, retry);
				} else {
					// TODO paired name space
					throw new NamespaceException("Retry limit exhausted, Namespace is down");
/*					try {
						MessageSender.class.wait();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}*/
				}
			}
		}
	}

	/**
	 * Generates MessageId
	 */
    public long generateMessageId() {
		return randomGenerator.nextLong() >>>1;
	}

    /**
     * Close all active connections
     * @throws JMSException
     */
	public void close() throws JMSException {
        if(sender != null) {
        	sender.close();
        }
        if(sendSession != null) {
        	sendSession.close();
        }
    	if(connection!= null) {
    		connection.close();
    	}
    	connection = null;
    	sendSession = null;
    	sender = null;
    }
    

	@Override
	public void onException(JMSException exception) {
		System.err.println("Error in sender connection, Retrying to connect...");

		try {
			close();
		} catch (JMSException e1) {	
			// We will get an Exception anyway, since the connection to the server is
            // broken, but close() frees up resources associated with the connection
		}
		
		try {
			initializeConnection();
		} catch (JMSException e) {
			System.err.println(e.getLocalizedMessage());
		}
	}
	
	/**
	 * Modify message property to sit in the backlog queue
	 * @param message
	 * @throws JMSException
	 */
	public void copyProperties(TextMessage message) throws JMSException {
		message.setLongProperty(AppConstants.TIME_TO_LIVE_PROP, message.getJMSExpiration());
		message.setJMSExpiration(0);	//JMSExpiration is set to zero to indicate that the message does not expire.
		message.setStringProperty(AppConstants.ORIGINAL_PATH_PROP, PairedNamespaceConfiguration.PRIMARY_QUEUE);
	}
	
	/**
	 * Sends ping messages to the entity in an attempt 
	 * to detect when that entity becomes available again.
	 * @return 
	 * @throws JMSException
	 */
	public boolean pingMessage() throws JMSException {
		Message message = sendSession.createMessage();
		message.setJMSExpiration(System.currentTimeMillis() + 1000);		// 1 sec
		message.setJMSType("application/vnd.ms-servicebus-ping");
		try {
			sender.send(message);
			System.err.println("Ping successfull");
			return true;
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
		}
		message = null;
		return false;
	}
	
	public TextMessage createPlainTextMessage() throws JMSException {
		return sendSession.createTextMessage();
	}
	
	/*public static void main(String[] args) throws IOException, NamingException, JMSException, NamespaceException {
		MessageSender sender = new MessageSender("SBCF2", "QUEUE1");
		String msg = "This is test message 1234";
		TextMessage message = sender.createPlainTextMessage();
		message.setJMSMessageID("ID:" + sender.generateMessageId());
		message.setText(msg);
		message.setLongProperty(AppConstants.TIME_TO_LIVE_PROP, 0);
		sender.sendMessage(message);
		
	}*/

}
