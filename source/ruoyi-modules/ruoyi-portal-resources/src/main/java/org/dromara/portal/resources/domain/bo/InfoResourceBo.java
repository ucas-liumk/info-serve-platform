package org.dromara.portal.resources.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.portal.resources.domain.InfoResource;

import java.util.List;

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
    /** 多分类全量（categoryId 为其中的主分类=首个；缺省时回退单值 categoryId） */
    private List<Long> categoryIds;
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
