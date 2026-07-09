package org.dromara.portal.kernel.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.mapper.AppMessageMapper;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.system.api.RemoteUserService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 门户统一消息通知（内核）。全部门户消息写入 app_message，由通知铃统一读取；
 * 广播收件人经 RemoteUserService 从系统上下文获取。
 */
@RequiredArgsConstructor
@Service
public class PortalNotificationServiceImpl implements IPortalNotificationService {

    private final AppMessageMapper messageMapper;

    @DubboReference
    private RemoteUserService remoteUserService;

    @Override
    public void sendToUser(Long userId, String title, String content, String msgType) {
        if (userId == null || StringUtils.isBlank(title)) {
            return;
        }
        messageMapper.insert(buildMessage(userId, title, content, msgType));
    }

    @Override
    public void sendToUsers(Iterable<Long> userIds, String title, String content, String msgType) {
        if (userIds == null || StringUtils.isBlank(title)) {
            return;
        }
        userIds.forEach(userId -> sendToUser(userId, title, content, msgType));
    }

    @Override
    public void sendToAllUsers(String title, String content, String msgType) {
        if (StringUtils.isBlank(title)) {
            return;
        }
        List<Long> userIds = remoteUserService.selectActiveUserIds();
        sendToUsers(userIds, title, content, msgType);
    }

    private AppMessage buildMessage(Long userId, String title, String content, String msgType) {
        AppMessage message = new AppMessage();
        message.setUserId(userId);
        message.setTitle(StringUtils.substring(title, 0, 200));
        message.setContent(content);
        message.setMsgType(StringUtils.blankToDefault(msgType, "system"));
        message.setIsRead("0");
        message.setCreateTime(new Date());
        return message;
    }
}
