package com.neebal.notification.connection;

import javax.jms.Message;
import javax.jms.MessageConsumer;

public interface QueueConnection {
	
	public void setUpQueue();

	public void sendMessageToRetryQueue(Message message);

	public MessageConsumer setupMessageQueueConsumer(String queueName);

	public void sendMessageToRetryQueueAgain(Message message);

	public void sendMessageToDeadLetterQueue(Message message);
}
