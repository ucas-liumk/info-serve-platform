package org.dromara.infoservice.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.infoservice.domain.InfoForumTopic;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InfoForumTopic.class)
public class InfoForumTopicBo extends BaseEntity {
    private Long topicId;
    @NotNull(message = "版块不能为空")
    private Long boardId;
    @NotBlank(message = "标题不能为空")
    @Size(min = 2, max = 160, message = "标题长度需在2到160字之间")
    private String title;
    @NotBlank(message = "正文不能为空")
    @Size(max = 5000, message = "正文不能超过5000字")
    private String content;
    private String isTop;
    private String isClosed;
    private String status;
    private String remark;
    private String keyword;
    private String boardCode;
}
