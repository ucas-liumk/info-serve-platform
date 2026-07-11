package org.dromara.portal.resources.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.portal.resources.domain.InfoResourceCategoryLink;

import java.util.Collection;
import java.util.List;

/**
 * 资料-分类关联表访问。tenant_id 由租户拦截器自动追加（INSERT 补列、SELECT/DELETE 补条件）；
 * 跨租户守卫计数须由调用方用 TenantHelper.ignore 包裹。
 */
public interface InfoResourceCategoryLinkMapper {

    @Insert("""
        <script>
        insert into info_resource_category_link (resource_id, category_id, create_time)
        values
        <foreach collection="categoryIds" item="cid" separator=",">(#{resourceId}, #{cid}, now())</foreach>
        on conflict (resource_id, category_id) do nothing
        </script>
        """)
    int insertLinks(@Param("resourceId") Long resourceId, @Param("categoryIds") Collection<Long> categoryIds);

    @Delete("delete from info_resource_category_link where resource_id = #{resourceId}")
    int deleteByResourceId(@Param("resourceId") Long resourceId);

    @Select("""
        <script>
        select resource_id, category_id from info_resource_category_link
        where resource_id in
        <foreach collection="resourceIds" item="rid" open="(" separator="," close=")">#{rid}</foreach>
        </script>
        """)
    List<InfoResourceCategoryLink> selectByResourceIds(@Param("resourceIds") Collection<Long> resourceIds);

    @Select("""
        <script>
        select count(distinct l.resource_id)
        from info_resource_category_link l
        join info_resource r on r.resource_id = l.resource_id and r.del_flag = '0'
        where l.category_id in
        <foreach collection="categoryIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
        </script>
        """)
    Long countUndeletedResourcesByCategoryIds(@Param("categoryIds") Collection<Long> categoryIds);
}
