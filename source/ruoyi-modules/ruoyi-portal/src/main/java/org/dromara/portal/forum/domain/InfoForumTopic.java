package org.dromara.portal.forum.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("info_forum_topic")
public class InfoForumTopic extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "topic_id")
    private Long topicId;
    private Long boardId;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private Long viewCount;
    private Long replyCount;
    private Long likeCount;
    private String isTop;
    private String isClosed;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
