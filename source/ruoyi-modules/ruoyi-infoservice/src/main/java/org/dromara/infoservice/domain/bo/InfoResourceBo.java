package org.dromara.infoservice.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.infoservice.domain.InfoResource;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InfoResource.class)
public class InfoResourceBo extends BaseEntity {
    private Long resourceId;
    @NotBlank(message = "资料标题不能为空")
    @Size(max = 160, message = "资料标题不能超过160字")
    private String title;
    @Size(max = 1000, message = "资料简介不能超过1000字")
    private String description;
    @NotNull(message = "资料分类不能为空")
    private Long categoryId;
    @NotNull(message = "文件不能为空")
    private Long ossId;
    private String originalName;
    private String fileSuffix;
    private String mimeType;
    private Long fileSize;
    private String previewType;
    private String status;
    private String remark;
    private String keyword;
    private String categoryCode;
    private String scope;
    private String fileType;
    private String uploadedWithin;
    private String sizeRange;
    private String sort;
}
