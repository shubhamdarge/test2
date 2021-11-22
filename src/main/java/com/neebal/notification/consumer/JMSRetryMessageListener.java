/**
 * 
 */

package com.neebal.notification.consumer;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.neebal.notification.connection.QueueConnection;
import com.neebal.notification.dto.ConsumerDto;
import com.neebal.notification.factory.QueueConnectionFactory;
import com.neebal.notification.service.NotificationConsumerService;
import com.neebal.notification.util.Config;


/**
 * @author ChiragShah
 * This listener will listen to the retry queue
 *
 */

public class JMSRetryMessageListener implements MessageListener {
	
	@Autowired
	Config config;
	
	@Autowired
	NotificationConsumerService notificationConsumerService;
	
	@Autowired
	QueueConnectionFactory queueConnectionFactory;

	QueueConnection queueConnection;
	String retryQueueName;
	public JMSRetryMessageListener() {
//    	setupMessageQueueConsumer();
    }

	/**
     * Sets up consumer of a given type,
     * For eg: currently consumers of ActiveMQ and SQS are available
     */
	@PostConstruct
	public void setupMessageQueueConsumer() {
    	try{
    		String queueType = config.getProperty("queueType");
    		retryQueueName = config.getProperty("queueName");
	    	queueConnection = queueConnectionFactory.getQueueConnection(queueType);
	    	MessageConsumer consumer=queueConnection.setupMessageQueueConsumer(retryQueueName);
	    	consumer.setMessageListener(this);
    	}
    	catch(Exception e){
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
    	}

	}

    /**
     * Listens for a message consumed,
     * Converts message body into JSON form and sends for further processing
     */
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof TextMessage) {
                Gson gson = new Gson();
                TextMessage textMessage = (TextMessage) message;
                
                String jsonBody = textMessage.getText();
                ConsumerDto content = gson.fromJson(jsonBody, ConsumerDto.class);
                
                System.out.println(content);
                
                boolean callSuccessfull = true;

             
                if (!callSuccessfull) {
                    sendMessageToRetryQueue(message);
                    message.acknowledge();
                }
                else {
                    message.acknowledge();
                }
            }
        }
        catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Message getting sent to a retry queue if request is not processed successfully
     * @param message
     */
	private void sendMessageToRetryQueue(Message message) {
		try {
			queueConnection.sendMessageToRetryQueueAgain(message);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
