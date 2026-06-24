package org.dromara.appcenter.service;

import org.dromara.appcenter.domain.bo.AppApplicationBo;
import org.dromara.appcenter.domain.vo.AppApplicationVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IAppApplicationService {
    TableDataInfo<AppApplicationVo> queryPageList(AppApplicationBo bo, PageQuery pageQuery);
    List<AppApplicationVo> queryList(AppApplicationBo bo);
    AppApplicationVo queryById(Long appId);
    Boolean insertByBo(AppApplicationBo bo);
    Boolean updateByBo(AppApplicationBo bo);
    Boolean changeStatus(Long appId, String status);
    Boolean deleteWithValidByIds(Collection<Long> ids);
}
