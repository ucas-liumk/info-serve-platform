package org.dromara.infoservice.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ForumTopicDetailVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private InfoForumTopicVo topic;
    private List<InfoForumReplyVo> replies;
}
