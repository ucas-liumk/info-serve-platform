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

    /** 门户上架应用累计使用次数（应用态势聚合用） */
    Long sumPortalUseCount();

    /** 门户上架应用使用排行（应用态势聚合用） */
    List<AppUsageRank> listPortalUsageTop(int limit);

    /** 低活跃上架应用数（应用态势聚合用） */
    Long countPortalLowUsage(Long maxUseCount);

    /** 按创建部门聚合的应用使用量（弱口径，应用态势聚合用） */
    List<DeptUsageStat> listDeptUsageStats();

    record AppUsageRank(String name, String categoryName, Long value) {
    }

    record DeptUsageStat(Long deptId, Long value) {
    }
}
