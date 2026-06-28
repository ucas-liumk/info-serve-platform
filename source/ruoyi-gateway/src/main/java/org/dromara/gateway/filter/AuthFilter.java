package org.dromara.gateway.filter;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.httpauth.basic.SaHttpBasicUtil;
import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.dromara.common.core.constant.HttpStatus;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.gateway.config.properties.IgnoreWhiteProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * [Sa-Token 权限认证] 拦截器
 *
 * @author Lion Li
 */
@Configuration
public class AuthFilter {

    private static final String JSON_UTF8_CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";
    private static final String CLIENT_ID_MISMATCH = "-100";

    /**
     * 注册 Sa-Token 全局过滤器
     */
    @Bean
    public SaReactorFilter getSaReactorFilter(IgnoreWhiteProperties ignoreWhite) {
        return new SaReactorFilter()
            // 拦截地址
            .addInclude("/**")
            .addExclude("/favicon.ico", "/actuator", "/actuator/**", "/resource/sse")
            // 鉴权方法：每次访问进入
            .setAuth(obj -> {
                // 登录校验 -- 拦截所有路由
                SaRouter.match("/**")
                    .notMatch(ignoreWhite.getWhites())
                    .check(r -> {
                        ServerHttpRequest request = SaReactorSyncHolder.getExchange().getRequest();
                        // 检查是否登录 是否有token
                        StpUtil.checkLogin();

                        // 检查 header 与 param 里的 clientid 与 token 里的是否一致
                        String headerCid = request.getHeaders().getFirst(LoginHelper.CLIENT_KEY);
                        String paramCid = request.getQueryParams().getFirst(LoginHelper.CLIENT_KEY);
                        String clientId = StpUtil.getExtra(LoginHelper.CLIENT_KEY).toString();
                        if (!StringUtils.equalsAny(clientId, headerCid, paramCid)) {
                            // token 无效
                            throw NotLoginException.newInstance(StpUtil.getLoginType(),
                                "-100", "客户端ID与Token不匹配",
                                StpUtil.getTokenValue());
                        }

                        // 有效率影响 用于临时测试
                        // if (log.isDebugEnabled()) {
                        //     log.debug("剩余有效时间: {}", StpUtil.getTokenTimeout());
                        //     log.debug("临时有效时间: {}", StpUtil.getTokenActivityTimeout());
                        // }
                    });
            }).setError(e -> {
                ServerHttpResponse response = SaReactorSyncHolder.getExchange().getResponse();
                setJsonUtf8Response(response);
                if (e instanceof NotLoginException notLoginException) {
                    return unauthorizedJson(getNotLoginMessage(notLoginException));
                }
                return unauthorizedJson("认证失败，无法访问系统资源");
            });
    }

    /**
     * 对 actuator 健康检查接口 做账号密码鉴权
     */
    @Bean
    public SaReactorFilter actuatorFilter() {
        String username = SpringUtils.getProperty("spring.cloud.nacos.discovery.metadata.username");
        String password = SpringUtils.getProperty("spring.cloud.nacos.discovery.metadata.userpassword");
        return new SaReactorFilter()
            .addInclude("/actuator", "/actuator/**")
            .setAuth(obj -> {
                SaHttpBasicUtil.check(username + ":" + password);
            })
            .setError(e -> {
                ServerHttpResponse response = SaReactorSyncHolder.getExchange().getResponse();
                setJsonUtf8Response(response);
                return unauthorizedJson(messageOrDefault(e.getMessage(), "认证失败，无法访问系统资源"));
            });
    }

    private static void setJsonUtf8Response(ServerHttpResponse response) {
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, JSON_UTF8_CONTENT_TYPE);
    }

    private static String getNotLoginMessage(NotLoginException e) {
        return switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未能读取到有效 token";
            case NotLoginException.INVALID_TOKEN -> "token 无效";
            case NotLoginException.TOKEN_TIMEOUT -> "token 已过期";
            case NotLoginException.BE_REPLACED -> "token 已被顶下线";
            case NotLoginException.KICK_OUT -> "token 已被踢下线";
            case NotLoginException.TOKEN_FREEZE -> "token 已被冻结";
            case NotLoginException.NO_PREFIX -> "未按照指定前缀提交 token";
            case CLIENT_ID_MISMATCH -> "客户端ID与Token不匹配";
            default -> messageOrDefault(e.getMessage(), "当前会话未登录");
        };
    }

    private static String messageOrDefault(String message, String defaultMessage) {
        return message == null || message.isBlank() ? defaultMessage : message;
    }

    private static String unauthorizedJson(String message) {
        return "{\"code\":" + HttpStatus.UNAUTHORIZED + ",\"msg\":\"" + escapeJsonAsAscii(message) + "\",\"data\":null}";
    }

    private static String escapeJsonAsAscii(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20 || ch > 0x7E) {
                        appendUnicodeEscape(sb, ch);
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static void appendUnicodeEscape(StringBuilder sb, char ch) {
        sb.append("\\u");
        String hex = Integer.toHexString(ch);
        for (int i = hex.length(); i < 4; i++) {
            sb.append('0');
        }
        sb.append(hex);
    }

}
