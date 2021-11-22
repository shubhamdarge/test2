package com.neebal.notification.factory;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.neebal.notification.connection.QueueConnection;
import com.neebal.notification.connection.impl.ActiveMQQueueConnection;
import com.neebal.notification.enums.QueueConnectionType;


@Component
public class QueueConnectionFactory {
	
	@Autowired
	ActiveMQQueueConnection activeMq;

	@Autowired
	QueueConnection queueConnection;

	/**
	 * 
	 * @param connectionType eg:ActiveMq, SQS
	 * @return a queueConnection based on the connection type that was passed
	 */
	public QueueConnection getQueueConnection(String connectionType) {
		try {
			QueueConnectionType conType = QueueConnectionType.valueOf(connectionType);
			switch (conType) {
			case ACTIVE_MQ:
				queueConnection = activeMq;
				break;
			case SQS:
				queueConnection = activeMq;
				break;
			}
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
		}
		queueConnection.setUpQueue();
		return queueConnection;

	}

}
