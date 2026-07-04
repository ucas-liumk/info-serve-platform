package org.dromara.portal.kernel.mapper;

import org.apache.ibatis.annotations.Select;

public interface InfoPortalMapper {

    @Select("""
        select count(distinct author_id)
        from (
            select author_id from info_forum_topic where status = '0' and del_flag = '0'
            union
            select author_id from info_forum_reply where status = '0' and del_flag = '0'
        ) t
        where author_id is not null
        """)
    Long countForumActiveUsers();

    @Select("""
        select coalesce(sum(coalesce(view_count, 0) + coalesce(download_count, 0)), 0)
        from info_resource
        where status = '0' and del_flag = '0'
        """)
    Long countResourceVisits();
}
