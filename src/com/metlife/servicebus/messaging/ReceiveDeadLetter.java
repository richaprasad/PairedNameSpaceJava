package com.metlife.servicebus.messaging;
/**
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


/**
 * @author rprasad017
 * <p>Class to receive message from Azure Service Bus using AMQP 1.0</p>
 */
public class ReceiveDeadLetter {

	private Connection connection;
    private Session receiveSession;
    private MessageConsumer receiver;
    
    private ConnectionFactory cf;
    private Destination queue;
    
	/**
	 * @throws IOException 
	 * @throws NamingException 
	 * @throws JMSException 
	 * @throws SecretKeyInitException 
	 * 
	 */
	public ReceiveDeadLetter() throws IOException, NamingException, JMSException {
		// Configure JNDI environment
    	ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    	InputStream input = classLoader.getResourceAsStream("servicebus.properties");
    	
    	Properties properties = new Properties();
    	properties.load(input);
        Context context = new InitialContext(properties);

        // Lookup ConnectionFactory and Queue
        cf = (ConnectionFactory) context.lookup("SBCF2");
        queue = (Destination) context.lookup("DEADQUEUE1");
        initializeConnection();
	}
	
	/**
	 * Initialize connection
	 * @throws JMSException
	 */
	private void initializeConnection() throws JMSException {
		 // Create Connection
       connection = cf.createConnection();
		
		// Create receiver-side Session, MessageConsumer,and MessageListener
       receiveSession = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
       receiver = receiveSession.createConsumer(queue);
       connection.start();
	}
	
	/**
	 * Retrieve message from queue
	 * @return 
	 * @throws IOException 
	 * @throws UnZipException 
	 * @throws DecryptException 
	 * @throws JMSException 
	 */
	public String getMessageFromQueue() throws IOException, JMSException {
		String contents = null;
		Message message = null;
		synchronized (this) {
			int count = 0;
			while (count < 5) {			// Try until message is received
				try {
					Thread.sleep(1000);
					message = receiver.receive(5000);	// Receives the next message arrives
													//	within the 5 seconds timeout interval.
					if (message != null) {
						contents = processMsg(message);		// start processing message
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				count++;
			}
		}
		return contents;
    }

	/**
	 * Process message body
	 * @param message
	 * @return 
	 * @throws JMSException
	 * @throws DecryptException
	 * @throws UnZipException
	 * @throws IOException 
	 */
	public synchronized String processMsg(Message message) throws JMSException, 
									IOException {
        System.out.println("Received message with JMSMessageID = " + message.getJMSMessageID());
        
        String reason = message.getStringProperty("DeadLetterReason");
    	String errDesc = message.getStringProperty("DeadLetterErrorDescription");
    	System.out.println("Dead Letter Reason: " + reason);
    	System.out.println("Dead Letter Error Description: " + errDesc);
        
        String contents = null;
        if (message instanceof TextMessage) {
        	TextMessage msg = (TextMessage) message;
        	contents = msg.getText();
        	message.acknowledge();
        } 
        return contents;
	}
	
	/**
	 * Close all active connections
	 * @throws JMSException
	 */
	public void close() {
        try {
        	if(receiver != null) {
        		receiver.close();
        	}
        	if(receiveSession != null) {
        		receiveSession.close();
        	}
        	if(connection != null) {
        		connection.stop();
        		connection.close();
        	}
		} catch (JMSException e) {
			e.printStackTrace();
		}
        receiver = null;
        receiveSession = null;
        connection = null;
    }
	
	public static void main(String[] args) throws IOException, NamingException, JMSException {
		ReceiveDeadLetter deadLetter =  new ReceiveDeadLetter();
		while(true) {
			deadLetter.getMessageFromQueue();
		}
//		deadLetter.close();
	}
	
}
