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
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author rprasad017
 *
 */
public class MessageReceiver implements MessageListener, ExceptionListener {
	
	private ConnectionFactory cf;
	private Connection connection;
    private Session receiveSession;
    private MessageConsumer receiver;
    private Destination queue;
    
    private String connectionfactory;
    private String queueName;
	
	/**
	 * @throws IOException 
	 * @throws NamingException 
	 * @throws JMSException 
	 * @throws SecretKeyInitException 
	 * 
	 */
	public MessageReceiver(String connectionfactory, String queueName) throws IOException, NamingException, JMSException {
		this.connectionfactory = connectionfactory;
		this.queueName = queueName;
		
		initReceiver();
	}
	
	private void initReceiver() throws IOException, NamingException, JMSException {
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

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage) {
				TextMessage msg = (TextMessage) message;
				System.out.println("Received: "+ msg.getText());
				message.acknowledge();
			}
        } catch (Exception e) {
//        	System.err.println(e.getLocalizedMessage());
        	e.printStackTrace();
        }
	}

	@Override
	public void onException(JMSException exception) {
		System.err.println("Error in receiver connection, Retrying to connect...");
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
     * Close all active connections
     * @throws JMSException
     */
	public void close() throws JMSException {
        if(receiver != null) {
        	receiver.setMessageListener(null);
        	receiver.close();
        }
        if(receiveSession != null) {
        	receiveSession.close();
        }
    	if(connection!= null) {
    		connection.close();
    	}
    	connection = null;
    	receiveSession = null;
    	receiver = null;
    }

	private void initializeConnection() throws JMSException {
		 // Create Connection
        connection = cf.createConnection();
        
		// Create receiver-side Session, MessageConsumer,and MessageListener
        receiveSession = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        receiver = receiveSession.createConsumer(queue);
        receiver.setMessageListener(this);
        connection.start();
	}
	
	public static void main(String[] args) throws IOException, NamingException, JMSException {
		new MessageReceiver("SBCF", "QUEUE");
	}

}
