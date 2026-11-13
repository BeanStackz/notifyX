package com.beanstack.notifyx.delivery.provider;

import com.beanstack.notifyx.notification.entity.Notification;
import com.beanstack.notifyx.notification.entity.NotificationChannel;

public interface NotificationProvider {
    NotificationChannel getChannel();
    DeliveryResult send(Notification notification);
}
