package org.dromara.portal.kernel.service;

public interface IPortalNotificationService {

    void sendToUser(Long userId, String title, String content, String msgType);

    void sendToUsers(Iterable<Long> userIds, String title, String content, String msgType);

    void sendToAllUsers(String title, String content, String msgType);
}
