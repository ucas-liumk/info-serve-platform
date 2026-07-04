package org.dromara.portal.forum.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.forum.domain.InfoForumBoard;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoForumBoard.class)
public class InfoForumBoardVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long boardId;
    private String boardName;
    private String boardCode;
    private String description;
    private Integer orderNum;
    private String status;
    private String remark;
    private Date createTime;
    private Long topicCount;
}
