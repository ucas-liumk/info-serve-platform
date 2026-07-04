package org.dromara.portal.resources.mapper;

import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;

public interface InfoResourceMapper extends BaseMapperPlus<InfoResource, InfoResourceVo> {

    /** 资料浏览+下载总量（资料 BC 自有口径，供内核统计聚合） */
    @Select("""
        select coalesce(sum(coalesce(view_count, 0) + coalesce(download_count, 0)), 0)
        from info_resource
        where status = '0' and del_flag = '0'
        """)
    Long sumPortalVisits();
}
