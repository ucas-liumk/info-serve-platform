package org.dromara.portal.forum.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.forum.domain.InfoForumTopic;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoForumTopic.class)
public class InfoForumTopicVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long topicId;
    private Long boardId;
    private String boardName;
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
    private String remark;
    private Date createTime;
    private Boolean liked;
}
