package org.dromara.portal.forum.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.portal.forum.domain.InfoForumReply;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InfoForumReply.class)
public class InfoForumReplyBo extends BaseEntity {
    private Long replyId;
    @NotNull(message = "主题不能为空")
    private Long topicId;
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复不能超过2000字")
    private String content;
    private String status;
    private String remark;
}
