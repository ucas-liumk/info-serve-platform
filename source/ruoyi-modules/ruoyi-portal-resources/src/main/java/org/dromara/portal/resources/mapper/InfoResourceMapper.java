package org.dromara.portal.resources.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryCountVo;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;

import java.util.Date;
import java.util.List;

public interface InfoResourceMapper extends BaseMapperPlus<InfoResource, InfoResourceVo> {

    /**
     * 上架资料按分类聚合计数（单条 GROUP BY，禁 N+1；SQL 见 mapper/resources/InfoResourceMapper.xml）。
     * 入参为分面筛选（关键词+工具条，不含分类维度自身），空参表示不筛选；租户条件由拦截器自动追加。
     */
    List<InfoResourceCategoryCountVo> countActiveByCategory(@Param("keyword") String keyword,
                                                            @Param("previewType") String previewType,
                                                            @Param("uploadedSince") Date uploadedSince,
                                                            @Param("minFileSize") Long minFileSize,
                                                            @Param("maxFileSize") Long maxFileSize);

    /** 资料浏览+下载总量（资料 BC 自有口径，供内核统计聚合） */
    @Select("""
        select coalesce(sum(coalesce(view_count, 0) + coalesce(download_count, 0)), 0)
        from info_resource
        where status = '0' and del_flag = '0'
        """)
    Long sumPortalVisits();
}
