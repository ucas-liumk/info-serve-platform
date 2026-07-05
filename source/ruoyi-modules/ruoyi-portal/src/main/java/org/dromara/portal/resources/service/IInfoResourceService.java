package org.dromara.portal.resources.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;
import org.dromara.portal.resources.domain.vo.ResourceUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface IInfoResourceService {

    TableDataInfo<InfoResourceVo> queryPageList(InfoResourceBo bo, PageQuery pageQuery);

    TableDataInfo<InfoResourceVo> portalPage(InfoResourceBo bo, PageQuery pageQuery);

    InfoResourceVo queryById(Long resourceId);

    InfoResourceVo queryPortalDetail(Long resourceId);

    InfoResourceVo queryPortalReadableDetail(Long resourceId);

    ResourceUploadVo upload(MultipartFile file);

    ResourceUploadVo portalUpload(MultipartFile file);

    void previewFile(Long resourceId, HttpServletResponse response) throws IOException;

    void previewPdf(Long resourceId, HttpServletResponse response) throws IOException;

    void downloadFile(Long resourceId, HttpServletResponse response) throws IOException;

    void favorite(Long resourceId, boolean add);

    /** 门户可见资料数（内核统计聚合用） */
    Long countPortalVisible();

    /** 资料浏览+下载总量（内核统计聚合用） */
    Long sumPortalVisits();

    /** 门户资料流动排行（应用态势聚合用） */
    List<ResourceUsageRank> listPortalUsageTop(int limit);

    /** 门户资料参与用户（应用态势聚合用） */
    List<Long> listPortalActorIds();

    /** 按创建部门聚合的资料流动量（应用态势聚合用） */
    List<DeptResourceStat> listDeptResourceStats();

    Boolean insertByBo(InfoResourceBo bo);

    Boolean insertPortalByBo(InfoResourceBo bo);

    Boolean updateByBo(InfoResourceBo bo);

    Boolean updateOwnByBo(InfoResourceBo bo);

    Boolean changeStatus(Long resourceId, String status);

    Boolean changeOwnStatus(Long resourceId, String status);

    Boolean deleteOwnById(Long resourceId);

    Boolean deleteWithValidByIds(Collection<Long> ids);

    record ResourceUsageRank(String name, String categoryName, Long value) {
    }

    record DeptResourceStat(Long deptId, Long value) {
    }
}
