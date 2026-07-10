package org.dromara.portal.resources.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("info_resource_category")
public class InfoResourceCategory extends BaseEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "category_id")
    private Long categoryId;
    /** 父栏目ID（NULL=栏目，非空=分类，两级封顶） */
    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private String description;
    private String icon;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
