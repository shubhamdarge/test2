package com.neebal.notification.service.impl;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.neebal.email.integration.processor.EmailProcessorImpl;
import com.neebal.email.integration.transferobject.EmailContent;
import com.neebal.ivr.integration.processor.IVRProcessor;
import com.neebal.ivr.integration.processor.IVRProcessorImpl;
import com.neebal.ivr.integration.transferobject.IVRContent;
import com.neebal.notification.service.NotificationConsumerService;
import com.neebal.sms.integration.processor.Processor;
import com.neebal.sms.integration.processor.SmsProcessorImpl;
import com.neebal.sms.integration.transferobject.SmsContent;

@Service
public class NotificationConsumerServiceImpl implements NotificationConsumerService{
	
	public static final Logger logger = Logger.getLogger(NotificationConsumerServiceImpl.class.getName());

	@Override
	public boolean sendIvrNotification(IVRContent ivrContent) {
		IVRProcessor ivrProcessor = IVRProcessorImpl.getInstance();
        boolean callSuccessfull = ivrProcessor.processIvrRequest(ivrContent);
        if(callSuccessfull) {
        	logger.info("IVR SENT");
        	return true;
        }
        return false;
	}

	@Override
	public boolean sendSmsNotification(SmsContent smsContent) {
		Processor smsProcessor = SmsProcessorImpl.getInstance();
		boolean isProcessed = smsProcessor.processRequest(smsContent);
		if(isProcessed) {
			logger.info("SMS SENT");
			return true;
		}
		return false;
	}

	@Override
	public boolean sendEmailNotification(EmailContent emailContent) {
		com.neebal.email.integration.processor.Processor emailProcessor = EmailProcessorImpl.getInstance();
        boolean isProcessed = emailProcessor.processRequest(emailContent);
        if(isProcessed) {
        	logger.info("EMAIL SENT");
        	return true;
        }
        return false;
	}

}
