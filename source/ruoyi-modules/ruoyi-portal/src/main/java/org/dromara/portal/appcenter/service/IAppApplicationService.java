package org.dromara.portal.appcenter.service;

import org.dromara.portal.appcenter.domain.bo.AppApplicationBo;
import org.dromara.portal.appcenter.domain.vo.AppApplicationVo;
import org.dromara.portal.appcenter.domain.vo.AppPackageUploadVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

public interface IAppApplicationService {
    TableDataInfo<AppApplicationVo> queryPageList(AppApplicationBo bo, PageQuery pageQuery);
    List<AppApplicationVo> queryList(AppApplicationBo bo);
    AppApplicationVo queryById(Long appId);
    Boolean insertByBo(AppApplicationBo bo);
    Boolean updateByBo(AppApplicationBo bo);
    Boolean changeStatus(Long appId, String status);
    AppPackageUploadVo uploadPackage(MultipartFile file);
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /**
     * 门户可见（上架）应用数，供内核首页统计聚合调用
     */
    Long countPortalVisible();
}
