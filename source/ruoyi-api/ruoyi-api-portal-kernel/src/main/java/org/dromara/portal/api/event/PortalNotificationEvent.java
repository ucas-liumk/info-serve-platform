package org.dromara.portal.api.event;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 门户通知事件（BC → RabbitMQ → kernel 落 app_message）。
 * scene 即历史 msgType 取值：demand / app / forum / resource / system。
 */
@Data
public class PortalNotificationEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TARGET_ALL = "ALL";
    public static final String TARGET_USERS = "USERS";

    private String eventId;
    private Long occurredAt;
    private String scene;
    private String title;
    private String content;
    private String targetType;
    private List<Long> targetUserIds;

    public static PortalNotificationEvent toAll(String scene, String title, String content) {
        PortalNotificationEvent e = new PortalNotificationEvent();
        e.setScene(scene);
        e.setTitle(title);
        e.setContent(content);
        e.setTargetType(TARGET_ALL);
        return e;
    }

    public static PortalNotificationEvent toUsers(String scene, String title, String content, List<Long> userIds) {
        PortalNotificationEvent e = toAll(scene, title, content);
        e.setTargetType(TARGET_USERS);
        e.setTargetUserIds(userIds);
        return e;
    }
}
