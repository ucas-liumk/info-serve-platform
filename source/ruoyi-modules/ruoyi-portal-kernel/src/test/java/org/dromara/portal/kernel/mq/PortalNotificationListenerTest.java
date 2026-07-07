package org.dromara.portal.kernel.mq;

import cn.hutool.extra.spring.SpringUtil;
import com.rabbitmq.client.Channel;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.api.event.PortalNotificationEvent;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PortalNotificationListenerTest {

    /**
     * RedisUtils.CLIENT 在类初始化时经 SpringUtils.getBean(RedissonClient)（继承自 hutool SpringUtil）取 bean，
     * 纯单测无 Spring 上下文会令 mockStatic(RedisUtils) 因类无法初始化而失败。
     * 先在受控的 SpringUtil 静态 mock 下强制完成一次类初始化，之后各用例即可安全 mockStatic(RedisUtils)。
     */
    @BeforeAll
    static void initRedisUtils() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(RedissonClient.class)).thenReturn(mock(RedissonClient.class));
            Class.forName("org.dromara.common.redis.utils.RedisUtils");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Mock
    private IPortalNotificationService notificationService;

    @Mock
    private Channel channel;

    @InjectMocks
    private PortalNotificationListener listener;

    private PortalNotificationEvent event(String targetType) {
        PortalNotificationEvent e = PortalNotificationEvent.toUsers("forum", "标题", "内容", List.of(2L));
        e.setEventId("evt-1");
        e.setTargetType(targetType);
        return e;
    }

    @Test
    void first_delivery_dispatches_and_acks() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
            listener.onNotification(event(PortalNotificationEvent.TARGET_USERS), channel, 7L);
            verify(notificationService).sendToUsers(List.of(2L), "标题", "内容", "forum");
            verify(channel).basicAck(7L, false);
        }
    }

    @Test
    void duplicate_delivery_skips_and_acks() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(false);
            listener.onNotification(event(PortalNotificationEvent.TARGET_USERS), channel, 8L);
            verify(notificationService, never()).sendToUsers(any(), anyString(), anyString(), anyString());
            verify(channel).basicAck(8L, false);
        }
    }

    @Test
    void broadcast_event_uses_sendToAllUsers() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
            listener.onNotification(event(PortalNotificationEvent.TARGET_ALL), channel, 9L);
            verify(notificationService).sendToAllUsers("标题", "内容", "forum");
            verify(channel).basicAck(9L, false);
        }
    }

    @Test
    void failure_nacks_to_dlq() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            redis.when(() -> RedisUtils.setObjectIfAbsent(anyString(), any(), any(Duration.class))).thenReturn(true);
            doThrow(new RuntimeException("db down")).when(notificationService)
                .sendToAllUsers(anyString(), anyString(), anyString());
            listener.onNotification(event(PortalNotificationEvent.TARGET_ALL), channel, 10L);
            verify(channel).basicNack(10L, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
        }
    }
}
