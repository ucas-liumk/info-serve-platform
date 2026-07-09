package org.dromara.portal.resources.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.RemoteUserService;
import org.dromara.system.api.domain.vo.RemoteUserVo;
import org.dromara.system.api.model.LoginUser;
import org.springframework.stereotype.Component;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ResourceUserDisplayNameResolver {

    @DubboReference
    private RemoteUserService remoteUserService;

    public String currentUserDisplayName(String fallback) {
        LoginUser loginUser = null;
        try {
            loginUser = LoginHelper.getLoginUser();
        } catch (Exception ignored) {
            return StringUtils.defaultIfBlank(LoginHelper.getUsername(), fallback);
        }
        if (loginUser == null) {
            return StringUtils.defaultIfBlank(LoginHelper.getUsername(), fallback);
        }
        return StringUtils.defaultIfBlank(
            loginUser.getNickname(),
            StringUtils.defaultIfBlank(loginUser.getUsername(), fallback)
        );
    }

    public Map<Long, String> resolveDisplayNames(Collection<Long> userIds) {
        List<Long> ids = userIds == null
            ? Collections.emptyList()
            : userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<RemoteUserVo> users = remoteUserService.selectListByIds(ids);
            if (users == null || users.isEmpty()) {
                return Collections.emptyMap();
            }
            return users.stream()
                .filter(user -> user.getUserId() != null)
                .map(user -> new AbstractMap.SimpleEntry<>(user.getUserId(), resolveDisplayName(user)))
                .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        } catch (Exception e) {
            log.warn("解析用户昵称失败: {}", ids, e);
            return Collections.emptyMap();
        }
    }

    private String resolveDisplayName(RemoteUserVo user) {
        return StringUtils.defaultIfBlank(user.getNickName(), user.getUserName());
    }
}
