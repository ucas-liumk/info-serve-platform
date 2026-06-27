package org.dromara.infoservice.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("info_forum_reply")
public class InfoForumReply extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "reply_id")
    private Long replyId;
    private Long topicId;
    private String content;
    private Long authorId;
    private String authorName;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
