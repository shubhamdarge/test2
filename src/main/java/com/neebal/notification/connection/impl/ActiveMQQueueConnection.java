package com.neebal.notification.connection.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.neebal.notification.connection.QueueConnection;
import com.neebal.notification.util.Config;
import com.neebal.notification.util.Constants;

@Service("activemq")
public class ActiveMQQueueConnection implements QueueConnection {
	
	@Autowired
	Config config;

	private long redeliveryDelayMultiplier;
	private String messageBrokerUrl;
	private String retryQueueName;
	private String username;
	private String password;
	private boolean transacted;
	private String messageDLQName;
	private long initialDelay;
	private long maxRetryCount;

		
	@Override
	public void setUpQueue() {
		messageBrokerUrl = config.getProperty(Constants.BROKER_URL);
		username = config.getProperty(Constants.QUEUE_ACCESS_USERNAME);
		password = config.getProperty(Constants.QUEUE_ACCESS_PASSWORD);
		transacted = false;
		redeliveryDelayMultiplier = Long.parseLong(config.getProperty(Constants.REDELIVERY_DELAY_MULTIPLIER));
		initialDelay = Long.parseLong(config.getProperty(Constants.INTIAL_DELAY_IN_SECONDS));
		maxRetryCount = Long.parseLong(config.getProperty(Constants.MAX_RETRY_COUNT));
		retryQueueName = config.getProperty(Constants.RETRY_QUEUE_NAME);
		messageDLQName = config.getProperty(Constants.DEAD_LETTER_QUEUE_NAME);
	}
	

	@Override
	public MessageConsumer setupMessageQueueConsumer(String queueName) {
		MessageConsumer consumer = null;
		try {
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);
			/*
			 * white listing all packages for whose class are the type of classes which are
			 * acceptable in message of type object message
			 */
			// String[] packages = { Constants.TRANSFEROBJECT_PACKAGE_STRUCTURE };
			// connectionFactory.setTrustedPackages(Arrays.asList(packages));
			ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection(username, password);
			connection.start();

			Session session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
			Destination adminQueue = session.createQueue(queueName);

			// Set up a consumer to consume messages off of the admin queue
			consumer = session.createConsumer(adminQueue);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}
		return consumer;

	}

	@Override
	public void sendMessageToRetryQueue(Message message) {
		try {
			// Create a ConnectionFactory
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);

			// Create a Connection
			Connection connection = connectionFactory.createConnection(username, password);

			// Create a Session
			Session session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);

			// Create the destination (Topic or Queue)
			Destination destination = session.createQueue(retryQueueName);

			// Create a MessageProducer from the Session to the Topic or Queue
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			connection.start();
			if (maxRetryCount > 0) {
				connection.start();
				message.clearProperties();
				message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, initialDelay * 1000l);
				message.setStringProperty(Constants.RETRY_COUNT, Long.toString(maxRetryCount));
				producer.send(message);
			} else {
				// I am not sending this to any retry queue or might send to DLQ
			}

			// Clean up
			session.close();
			connection.close();
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}

	}

	@Override
	public void sendMessageToRetryQueueAgain(Message message) {
		try {
			// Create a ConnectionFactory
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);

			// Create a Connection
			Connection connection = connectionFactory.createConnection(username, password);

			// Create a Session
			Session session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);

			// Create the destination (Topic or Queue)
			Destination destination = session.createQueue(retryQueueName);

			// Create a MessageProducer from the Session to the Topic or Queue
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			connection.start();
			boolean isRetryFinished = false;
			String retryCount = message.getStringProperty(Constants.RETRY_COUNT);
			if (retryCount.equals(Constants.ONE)) {
				isRetryFinished = true;
			} else {
				retryCount = Long.toString(Long.parseLong(retryCount) - 1);
			}
			if (!isRetryFinished) {
				long period = getScheduleDelayTime(retryCount);
				message.clearProperties();
				message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD, period);
				message.setStringProperty(Constants.RETRY_COUNT, retryCount);
				producer.send(message);
			} else {
				sendMessageToDeadLetterQueue(message);
			}

			// Clean up
			session.close();
			connection.close();
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}

	}

	@Override
	public void sendMessageToDeadLetterQueue(Message message) {
		try {
			// Create a ConnectionFactory
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);

			// Create a Connection
			Connection connection = connectionFactory.createConnection(username, password);

			// Create a Session
			Session session = connection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);

			// Create the destination (Topic or Queue)
			Destination destination = session.createQueue(messageDLQName);

			// Create a MessageProducer from the Session to the Topic or Queue
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			connection.start();

			producer.send(message);

			// Clean up
			session.close();
			connection.close();
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @param retryCount
	 * @return
	 */
	private Long getScheduleDelayTime(String retryCount) {
		if (retryCount == null) {
			return redeliveryDelayMultiplier * 1000l;
		} else {
			return (long) (Math.pow(redeliveryDelayMultiplier, (maxRetryCount - Long.parseLong(retryCount) + 1))
					* 1000l);
		}
	}

}
