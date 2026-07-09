package org.dromara.portal.kernel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.domain.vo.AppMessageVo;
import org.dromara.portal.kernel.mapper.AppMessageMapper;
import org.dromara.portal.kernel.service.impl.PortalMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalMessageServiceTest {

    @Mock
    private AppMessageMapper messageMapper;

    @InjectMocks
    private PortalMessageServiceImpl service;

    @Test
    void deleteRead_missing_message_throws() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            when(messageMapper.selectOne(any())).thenReturn(null);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.deleteRead(9L));
            assertEquals("通知不存在", ex.getMessage());
        }
    }

    @Test
    void deleteRead_unread_message_rejected() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            AppMessage msg = new AppMessage();
            msg.setIsRead("0");
            when(messageMapper.selectOne(any())).thenReturn(msg);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.deleteRead(9L));
            assertEquals("请先标记已读后再删除", ex.getMessage());
        }
    }

    @Test
    void deleteRead_read_message_deleted() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            AppMessage msg = new AppMessage();
            msg.setIsRead("1");
            when(messageMapper.selectOne(any())).thenReturn(msg);
            service.deleteRead(9L);
            verify(messageMapper).delete(any());
        }
    }

    @Test
    void pageMyMessages_withIsReadFilter_returnsPage() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            Page<AppMessage> msgPage = new Page<>(1, 10);
            AppMessage msg = new AppMessage();
            msg.setMessageId(1L);
            msg.setTitle("Hello");
            msgPage.setRecords(List.of(msg));
            msgPage.setTotal(1);
            when(messageMapper.selectPage(any(), any())).thenReturn(msgPage);

            TableDataInfo<AppMessageVo> result = service.pageMyMessages("0", pageQuery());

            assertEquals(1, result.getRows().size());
            assertEquals(1L, result.getTotal());
            verify(messageMapper).selectPage(any(), any());
        }
    }

    @Test
    void pageMyMessages_withNoFilter_returnsAllMessages() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            Page<AppMessage> msgPage = new Page<>(1, 10);
            msgPage.setRecords(List.of());
            msgPage.setTotal(0);
            when(messageMapper.selectPage(any(), any())).thenReturn(msgPage);

            TableDataInfo<AppMessageVo> result = service.pageMyMessages(null, pageQuery());

            assertEquals(0, result.getRows().size());
            verify(messageMapper).selectPage(any(), any());
        }
    }

    @Test
    void countMyUnread_returnsCountForCurrentUser() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            when(messageMapper.selectCount(any())).thenReturn(5L);

            assertEquals(5L, service.countMyUnread());
            verify(messageMapper).selectCount(any());
        }
    }

    @Test
    void countMyUnread_returnsZeroWhenNoUnread() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            when(messageMapper.selectCount(any())).thenReturn(0L);

            assertEquals(0L, service.countMyUnread());
        }
    }

    @Test
    void markRead_updatesMessageScopedToCurrentUser() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            when(messageMapper.update(any(AppMessage.class), any())).thenReturn(1);

            service.markRead(10L);

            verify(messageMapper).update(any(AppMessage.class), any());
        }
    }

    @Test
    void markRead_setsIsReadTo1() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            ArgumentCaptor<AppMessage> captor = ArgumentCaptor.forClass(AppMessage.class);
            when(messageMapper.update(captor.capture(), any())).thenReturn(1);

            service.markRead(7L);

            AppMessage captured = captor.getValue();
            assertEquals(7L, captured.getMessageId());
            assertEquals("1", captured.getIsRead());
        }
    }

    private static PageQuery pageQuery() {
        PageQuery pageQuery = new PageQuery();
        pageQuery.setPageNum(1);
        pageQuery.setPageSize(10);
        return pageQuery;
    }
}
