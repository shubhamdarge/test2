package com.neebal.notification.service;

import com.neebal.email.integration.transferobject.EmailContent;
import com.neebal.ivr.integration.transferobject.IVRContent;
import com.neebal.sms.integration.transferobject.SmsContent;

public interface NotificationConsumerService {

	public boolean sendIvrNotification(IVRContent ivrContent);
	public boolean sendSmsNotification(SmsContent smsContent);
	public boolean sendEmailNotification(EmailContent emailContent);
}
