package org.dromara.portal.appcenter.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.portal.appcenter.domain.bo.AppDemandSubmitBo;
import org.dromara.portal.appcenter.domain.vo.AppCategoryVo;
import org.dromara.portal.appcenter.domain.vo.AppDemandVo;
import org.dromara.portal.appcenter.domain.vo.PortalAppVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.io.IOException;
import java.util.List;

public interface IAppPortalService {
    List<AppCategoryVo> categories();
    TableDataInfo<PortalAppVo> apps(String categoryCode, String keyword, String appType, String sort, PageQuery pageQuery);
    String use(Long appId);
    void downloadPackage(Long appId, HttpServletResponse response) throws IOException;
    void favorite(Long appId, boolean add);
    void recommend(Long appId, boolean add);
    TableDataInfo<PortalAppVo> favorites(PageQuery pageQuery);
    Boolean submitDemand(AppDemandSubmitBo bo);
    TableDataInfo<AppDemandVo> myDemands(PageQuery pageQuery);
    Boolean deleteMyDemand(Long demandId);
}
