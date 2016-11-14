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

import com.metlife.servicebus.PairedNamespaceConfiguration;
import com.metlife.servicebus.messaging.util.AppConstants;

/**
 * @author rprasad017
 *
 */
public class SyphonProcess implements MessageListener, ExceptionListener {
	
	private ConnectionFactory cf;
	private Connection connection;
    private Session receiveSession;
    private MessageConsumer receiver;
    private Destination queue;
    
    private String connectionfactory;
    private String queueName;
    
    private MessageSender sender;
	
	/**
	 * @throws IOException 
	 * @throws NamingException 
	 * @throws JMSException 
	 * @throws SecretKeyInitException 
	 * 
	 */
	public SyphonProcess(String connectionfactory, String queueName) throws IOException, NamingException, JMSException {
		this.connectionfactory = connectionfactory;
		this.queueName = queueName;
		
		sender = new MessageSender(PairedNamespaceConfiguration.PRIMARY_SBCF, 
				PairedNamespaceConfiguration.PRIMARY_QUEUE); 	// TODO, created new sender. retieve orginal sender/ one sender for backlog queues
		
		initReceiver();
		System.err.println("Syphon process is ready");
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
				TextMessage msg = restoreProperties((TextMessage) message);
				
				sender.sendMessage(msg);
				
				System.out.println("Syphon message: "+ msg.getText());
				message.acknowledge();
				msg = null;
			}
        } catch (Exception e) {
//        	System.err.println(e.getLocalizedMessage());
        	e.printStackTrace();
        }
	}

	@Override
	public void onException(JMSException exception) {
		System.err.println("Error in connection, Retrying to connect...");
		try {
			initializeConnection();
		} catch (JMSException e) {
			System.err.println(e.getLocalizedMessage());
		}
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
	
	/**
	 * Modify message property to sit in the backlog queue
	 * @param message
	 * @throws JMSException
	 */
	public TextMessage restoreProperties(TextMessage message) throws JMSException {
		TextMessage newMsg = sender.createPlainTextMessage();
		newMsg.setJMSMessageID(message.getJMSMessageID());
		newMsg.setJMSCorrelationID(message.getJMSMessageID());
		newMsg.setText(message.getText());
		if(message.propertyExists(AppConstants.TIME_TO_LIVE_PROP)) {
			newMsg.setJMSExpiration(message.getLongProperty(AppConstants.TIME_TO_LIVE_PROP));
		}
		return newMsg;
	}
	
	/**
	 * Close syphon process
	 * @throws JMSException
	 */
	public void closeSyphon() throws JMSException {
		if(connection != null) {
			connection.stop();
		}
		if(receiver != null) {
			receiver.close();
			receiver = null;
		}
		if(receiveSession != null) {
			receiveSession.close();
			receiveSession = null;
		}
		if(connection != null) {
			connection.close();
			connection = null;
		}
		if(sender != null) {
			sender.close();
			sender = null;
		}
	}
	
	/*public static void main(String[] args) throws IOException, NamingException, JMSException {
		new SyphonProcess("SBCF2", "QUEUE2");
	}*/

}