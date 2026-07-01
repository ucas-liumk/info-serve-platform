package org.dromara.infoservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.infoservice.domain.InfoPortalMessage;
import org.dromara.infoservice.domain.InfoPortalUser;
import org.dromara.infoservice.mapper.InfoPortalMessageMapper;
import org.dromara.infoservice.mapper.InfoPortalUserMapper;
import org.dromara.infoservice.service.IInfoPortalNotificationService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class InfoPortalNotificationServiceImpl implements IInfoPortalNotificationService {

    private final InfoPortalMessageMapper messageMapper;
    private final InfoPortalUserMapper userMapper;

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
        List<InfoPortalUser> users = userMapper.selectList(
            Wrappers.<InfoPortalUser>lambdaQuery()
                .eq(InfoPortalUser::getStatus, "0")
                .eq(InfoPortalUser::getDelFlag, "0"));
        users.forEach(user -> sendToUser(user.getUserId(), title, content, msgType));
    }

    private InfoPortalMessage buildMessage(Long userId, String title, String content, String msgType) {
        InfoPortalMessage message = new InfoPortalMessage();
        message.setUserId(userId);
        message.setTitle(StringUtils.substring(title, 0, 200));
        message.setContent(content);
        message.setMsgType(StringUtils.blankToDefault(msgType, "system"));
        message.setIsRead("0");
        message.setCreateTime(new Date());
        return message;
    }
}
