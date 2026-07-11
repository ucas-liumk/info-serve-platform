package org.dromara.portal.resources.domain;

import lombok.Data;

import java.util.Date;

/**
 * 资料-分类多对多关联行（info_resource_category_link）。
 * 关联表为分类筛选/计数/删除守卫的唯一事实源；info_resource.category_id 保留为主分类。
 * 仅用于注解 SQL 的结果映射与参数承载，无 MyBatis-Plus 实体语义（复合主键，物理删除）。
 */
@Data
public class InfoResourceCategoryLink {

    private Long resourceId;

    private Long categoryId;

    private String tenantId;

    private Date createTime;
}
