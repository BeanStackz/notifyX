package com.beanstack.notifyx.delivery.provider;

import com.beanstack.notifyx.delivery.service.DeliveryContext;
import com.beanstack.notifyx.notification.entity.NotificationChannel;

public interface NotificationProvider {
    NotificationChannel getChannel();
    DeliveryResult send(DeliveryContext context);
}
