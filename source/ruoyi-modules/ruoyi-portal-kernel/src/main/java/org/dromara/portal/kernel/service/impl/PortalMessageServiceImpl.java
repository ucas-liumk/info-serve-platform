package org.dromara.portal.kernel.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.domain.vo.AppMessageVo;
import org.dromara.portal.kernel.mapper.AppMessageMapper;
import org.dromara.portal.kernel.service.IPortalMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PortalMessageServiceImpl implements IPortalMessageService {

    private final AppMessageMapper messageMapper;

    @Override
    public TableDataInfo<AppMessageVo> pageMyMessages(String isRead, PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        Page<AppMessage> page = messageMapper.selectPage(pageQuery.build(),
            Wrappers.<AppMessage>lambdaQuery()
                .eq(AppMessage::getUserId, userId)
                .eq(StringUtils.isNotBlank(isRead), AppMessage::getIsRead, isRead)
                .orderByDesc(AppMessage::getCreateTime));
        List<AppMessageVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public long countMyUnread() {
        Long userId = LoginHelper.getUserId();
        return messageMapper.selectCount(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getUserId, userId).eq(AppMessage::getIsRead, "0"));
    }

    @Override
    public void markRead(Long messageId) {
        Long userId = LoginHelper.getUserId();
        AppMessage up = new AppMessage();
        up.setMessageId(messageId);
        up.setIsRead("1");
        messageMapper.update(up, Wrappers.<AppMessage>lambdaUpdate()
            .eq(AppMessage::getMessageId, messageId).eq(AppMessage::getUserId, userId));
    }

    @Override
    public void deleteRead(Long messageId) {
        Long userId = LoginHelper.getUserId();
        AppMessage message = messageMapper.selectOne(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getMessageId, messageId)
            .eq(AppMessage::getUserId, userId));
        if (message == null) {
            throw new ServiceException("通知不存在");
        }
        if (!"1".equals(message.getIsRead())) {
            throw new ServiceException("请先标记已读后再删除");
        }
        messageMapper.delete(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getMessageId, messageId)
            .eq(AppMessage::getUserId, userId)
            .eq(AppMessage::getIsRead, "1"));
    }

    @Override
    public void clearRead() {
        Long userId = LoginHelper.getUserId();
        messageMapper.delete(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getUserId, userId)
            .eq(AppMessage::getIsRead, "1"));
    }

    private AppMessageVo toVo(AppMessage message) {
        AppMessageVo vo = new AppMessageVo();
        vo.setMessageId(message.getMessageId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
