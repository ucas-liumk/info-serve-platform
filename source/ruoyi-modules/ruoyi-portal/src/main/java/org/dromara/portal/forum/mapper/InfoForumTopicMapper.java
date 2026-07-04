package org.dromara.portal.forum.mapper;

import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.portal.forum.domain.InfoForumTopic;
import org.dromara.portal.forum.domain.vo.InfoForumTopicVo;

public interface InfoForumTopicMapper extends BaseMapperPlus<InfoForumTopic, InfoForumTopicVo> {

    /** 论坛活跃用户数：发帖或回帖作者去重（论坛 BC 自有口径，供内核统计聚合） */
    @Select("""
        select count(distinct author_id)
        from (
            select author_id from info_forum_topic where status = '0' and del_flag = '0'
            union
            select author_id from info_forum_reply where status = '0' and del_flag = '0'
        ) t
        where author_id is not null
        """)
    Long countActiveAuthors();
}
