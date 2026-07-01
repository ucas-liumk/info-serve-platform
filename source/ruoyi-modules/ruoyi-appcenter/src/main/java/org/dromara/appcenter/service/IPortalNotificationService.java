package org.dromara.appcenter.service;

public interface IPortalNotificationService {

    void sendToUser(Long userId, String title, String content, String msgType);

    void sendToAllUsers(String title, String content, String msgType);
}
