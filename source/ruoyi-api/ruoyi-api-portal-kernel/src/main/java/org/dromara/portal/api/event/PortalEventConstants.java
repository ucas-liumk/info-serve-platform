package org.dromara.portal.api.event;

/** 门户事件契约常量（exchange / routing key / 队列名 / Dubbo group），跨服务勿改字面值 */
public final class PortalEventConstants {

    public static final String EXCHANGE = "portal.topic";
    public static final String DLX = "portal.dlx";

    public static final String QUEUE_KERNEL_NOTIFICATIONS = "portal-kernel.notifications";
    public static final String QUEUE_KERNEL_NOTIFICATIONS_DLQ = "portal-kernel.notifications.dlq";
    public static final String QUEUE_RESOURCES_CONVERT = "portal-resources.convert";
    public static final String QUEUE_RESOURCES_CONVERT_DLQ = "portal-resources.convert.dlq";

    public static final String RK_APPCENTER_DEMAND_REPLIED = "appcenter.demand.replied";
    public static final String RK_APPCENTER_APP_PUBLISHED = "appcenter.application.published";
    public static final String RK_FORUM_REPLY_CREATED = "forum.reply.created";
    public static final String RK_RESOURCES_ITEM_PUBLISHED = "resources.item.published";

    public static final String STATS_GROUP_APPCENTER = "appcenter";
    public static final String STATS_GROUP_FORUM = "forum";
    public static final String STATS_GROUP_RESOURCES = "resources";

    private PortalEventConstants() {
    }
}
