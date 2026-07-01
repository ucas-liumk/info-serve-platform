package org.dromara.appcenter.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.AppMessage;
import org.dromara.appcenter.domain.AppPortalUser;
import org.dromara.appcenter.mapper.AppMessageMapper;
import org.dromara.appcenter.mapper.AppPortalUserMapper;
import org.dromara.appcenter.service.IPortalNotificationService;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PortalNotificationServiceImpl implements IPortalNotificationService {

    private final AppMessageMapper messageMapper;
    private final AppPortalUserMapper userMapper;

    @Override
    public void sendToUser(Long userId, String title, String content, String msgType) {
        if (userId == null || StringUtils.isBlank(title)) {
            return;
        }
        messageMapper.insert(buildMessage(userId, title, content, msgType));
    }

    @Override
    public void sendToAllUsers(String title, String content, String msgType) {
        if (StringUtils.isBlank(title)) {
            return;
        }
        List<AppPortalUser> users = userMapper.selectList(
            Wrappers.<AppPortalUser>lambdaQuery()
                .eq(AppPortalUser::getStatus, "0")
                .eq(AppPortalUser::getDelFlag, "0"));
        users.forEach(user -> sendToUser(user.getUserId(), title, content, msgType));
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
