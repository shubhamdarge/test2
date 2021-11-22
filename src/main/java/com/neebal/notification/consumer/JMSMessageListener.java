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
import com.neebal.email.integration.transferobject.EmailContent;
import com.neebal.ivr.integration.transferobject.IVRContent;
import com.neebal.notification.connection.QueueConnection;
import com.neebal.notification.dto.ConsumerDto;
import com.neebal.notification.enums.NotificationType;
import com.neebal.notification.factory.QueueConnectionFactory;
import com.neebal.notification.service.NotificationConsumerService;
import com.neebal.notification.util.Config;
import com.neebal.notification.util.Constants;
import com.neebal.sms.integration.transferobject.SmsContent;


@Component
public class JMSMessageListener implements MessageListener {
	
	private static final Logger logger = Logger.getLogger(JMSMessageListener.class.getName());
	
	@Autowired
	Config config;
	
	@Autowired
	NotificationConsumerService notificationConsumerService;
	
	@Autowired
	QueueConnectionFactory queueConnectionFactory;
	
	QueueConnection queueConnection;
	String queueName;
    public JMSMessageListener() {
    	
    }
    
    /**
     * Sets up consumer of a given type,
     * For eg: currently consumers of ActiveMQ and SQS are available
     */
    @PostConstruct
    public void setupMessageQueueConsumer() {
    	try{
	        String queueType = config.getProperty(Constants.QUEUE_TYPE);
	        queueName = config.getProperty(Constants.QUEUE_NAME);
	    	queueConnection = queueConnectionFactory.getQueueConnection(queueType);
	    	MessageConsumer consumer=queueConnection.setupMessageQueueConsumer(queueName);
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

                boolean isNotificationSent = false;
                if(content.getApplication().equalsIgnoreCase(NotificationType.SMS.name())) {
                	SmsContent smsContent = new SmsContent();
                	smsContent.setApplication(content.getApplication());
                	smsContent.setContent(content.getContent());
                	smsContent.setMobileNo(content.getMobile());
                	
                	logger.info("Calling SMS service");
                	isNotificationSent = notificationConsumerService.sendSmsNotification(smsContent);
                }
                if(content.getApplication().equalsIgnoreCase(NotificationType.EMAIL.name())) {
                	EmailContent emailContent = new EmailContent();
                	emailContent.setApplication(content.getApplication());
                	emailContent.setBody(content.getBody());
                	emailContent.setCcEmails(content.getCcEmails());
                	emailContent.setSubject(content.getSubject());
                	emailContent.setToEmails(content.getToEmails());
                	
                	logger.info("Calling Email service");
                	isNotificationSent = notificationConsumerService.sendEmailNotification(emailContent);
                }
                if(content.getApplication().equalsIgnoreCase(NotificationType.IVR.name())) {
                	IVRContent ivrContent = new IVRContent();
                	ivrContent.setApplication(content.getApplication());
                	ivrContent.setContent(content.getContent());
                	ivrContent.setMobile(content.getMobile());
                	
                	logger.info("Calling IVR service");
                	isNotificationSent = notificationConsumerService.sendIvrNotification(ivrContent);
                }
                    		                
                if (!isNotificationSent) {
                	logger.severe("Call Unsuccessful");
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
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "In JMS send msg to retry Q");
			queueConnection.sendMessageToRetryQueue(message);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}
	}
}
