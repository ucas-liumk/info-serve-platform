package org.dromara.appcenter.service;

import org.dromara.appcenter.domain.bo.AppDemandBo;
import org.dromara.appcenter.domain.bo.AppDemandSubmitBo;
import org.dromara.appcenter.domain.vo.AppDemandVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;

public interface IAppDemandService {
    TableDataInfo<AppDemandVo> queryPageList(AppDemandBo bo, PageQuery pageQuery);
    AppDemandVo queryById(Long demandId);
    TableDataInfo<AppDemandVo> queryMyPageList(PageQuery pageQuery);
    Boolean submitFromPortal(AppDemandSubmitBo bo);
    Boolean deleteOwnById(Long demandId);
    Boolean updateHandle(AppDemandBo bo);
    Boolean deleteWithValidByIds(Collection<Long> ids);
}
