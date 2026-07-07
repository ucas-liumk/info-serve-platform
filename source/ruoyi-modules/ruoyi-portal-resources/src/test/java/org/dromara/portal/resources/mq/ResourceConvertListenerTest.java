package org.dromara.portal.resources.mq;

import cn.hutool.extra.spring.SpringUtil;
import com.rabbitmq.client.Channel;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResourceConvertListenerTest {

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
    private IInfoResourceService resourceService;

    @Mock
    private Channel channel;

    @InjectMocks
    private ResourceConvertListener listener;

    private ResourceConvertMessage msg() {
        ResourceConvertMessage m = new ResourceConvertMessage();
        m.setMessageId("m-1");
        m.setResourceId(42L);
        return m;
    }

    @Test
    void success_converts_acks_and_clears_pending_flag() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            listener.onConvert(msg(), channel, 5L);
            verify(resourceService).ensurePreviewConverted(42L);
            verify(channel).basicAck(5L, false);
            redis.verify(() -> RedisUtils.deleteObject("resources:convert:pending:42"));
        }
    }

    @Test
    void failure_nacks_to_dlq_and_clears_pending_flag() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            doThrow(new RuntimeException("soffice missing")).when(resourceService).ensurePreviewConverted(42L);
            listener.onConvert(msg(), channel, 6L);
            verify(channel).basicNack(6L, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
            redis.verify(() -> RedisUtils.deleteObject("resources:convert:pending:42"));
        }
    }
}
