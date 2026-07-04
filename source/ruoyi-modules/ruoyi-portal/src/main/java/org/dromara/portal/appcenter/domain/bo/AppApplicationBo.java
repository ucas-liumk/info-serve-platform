package org.dromara.portal.appcenter.domain.bo;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppApplicationBo extends BaseEntity {
    private Long appId;
    @NotBlank(message = "应用名称不能为空")
    private String appName;
    @NotBlank(message = "应用编码不能为空")
    private String appCode;
    private String version;
    @NotNull(message = "分类不能为空")
    private Long categoryId;
    private String icon;
    private String accent;
    @Size(max = 255, message = "描述不能超过255字")
    private String description;
    private String tags;
    @Pattern(regexp = "^$|^https?://.*", message = "访问地址必须以 http(s):// 开头")
    private String accessUrl;
    private String appType;
    private Long packageOssId;
    private String packageName;
    private Long packageSize;
    private String packageUrl;
    private String status;
    private String isSecurity;
    private Integer orderNum;
    private String remark;
    // 列表查询条件
    private String keyword;
}
