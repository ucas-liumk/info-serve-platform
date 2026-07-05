package org.dromara.portal.resources.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InfoResourceNoteBo {
    @NotBlank(message = "笔记内容不能为空")
    @Size(max = 2000, message = "笔记内容不能超过 2000 个字符")
    private String content;

    private String visibility;
}
