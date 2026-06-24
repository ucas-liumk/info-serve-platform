package org.dromara.appcenter.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.appcenter.domain.AppCategory;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = AppCategory.class)
public class AppCategoryVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private String icon;
    private Integer orderNum;
    private String status;
    private Long appCount;
}
