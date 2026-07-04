package org.dromara.portal.forum.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.forum.domain.InfoForumReply;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoForumReply.class)
public class InfoForumReplyVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long replyId;
    private Long topicId;
    private String content;
    private Long authorId;
    private String authorName;
    private String status;
    private String remark;
    private Date createTime;
}
