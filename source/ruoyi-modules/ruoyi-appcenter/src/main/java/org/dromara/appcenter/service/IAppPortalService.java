package org.dromara.appcenter.service;

import org.dromara.appcenter.domain.bo.AppDemandSubmitBo;
import org.dromara.appcenter.domain.vo.AppCategoryVo;
import org.dromara.appcenter.domain.vo.AppDemandVo;
import org.dromara.appcenter.domain.vo.AppMessageVo;
import org.dromara.appcenter.domain.vo.PortalAppVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IAppPortalService {
    List<AppCategoryVo> categories();
    TableDataInfo<PortalAppVo> apps(String categoryCode, String keyword, String sort, PageQuery pageQuery);
    String use(Long appId);
    void favorite(Long appId, boolean add);
    void recommend(Long appId, boolean add);
    TableDataInfo<PortalAppVo> favorites(PageQuery pageQuery);
    TableDataInfo<AppMessageVo> messages(String isRead, PageQuery pageQuery);
    long unreadCount();
    void readMessage(Long messageId);
    void deleteReadMessage(Long messageId);
    void clearReadMessages();
    Boolean submitDemand(AppDemandSubmitBo bo);
    TableDataInfo<AppDemandVo> myDemands(PageQuery pageQuery);
    Boolean deleteMyDemand(Long demandId);
}
