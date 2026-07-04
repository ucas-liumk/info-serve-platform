package org.dromara.portal.forum.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("info_forum_board")
public class InfoForumBoard extends BaseEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "board_id")
    private Long boardId;
    private String boardName;
    private String boardCode;
    private String description;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
