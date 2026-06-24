package org.dromara.appcenter.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

/** 应用分类 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_category")
public class AppCategory extends BaseEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "category_id")
    private Long categoryId;
    private String categoryName;
    private String categoryCode;
    private String icon;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
}
