package org.dromara.portal.forum.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.portal.forum.domain.InfoForumBoard;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InfoForumBoard.class)
public class InfoForumBoardBo extends BaseEntity {
    private Long boardId;
    @NotBlank(message = "版块名称不能为空")
    private String boardName;
    @NotBlank(message = "版块编码不能为空")
    private String boardCode;
    private String description;
    private Integer orderNum;
    private String status;
    private String remark;
    private String keyword;
}
