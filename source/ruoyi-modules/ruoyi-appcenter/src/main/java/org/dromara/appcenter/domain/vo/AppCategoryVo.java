package org.dromara.appcenter.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
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
