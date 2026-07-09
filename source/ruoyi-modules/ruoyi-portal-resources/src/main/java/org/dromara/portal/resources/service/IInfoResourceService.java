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

    /** 确保资料的 PDF 预览缓存已生成（MQ 消费线程调用，幂等） */
    void ensurePreviewConverted(Long resourceId);

    void downloadFile(Long resourceId, HttpServletResponse response) throws IOException;

    void favorite(Long resourceId, boolean add);

    /** 门户可见资料数（内核统计聚合用） */
    Long countPortalVisible();

    /** 资料浏览+下载总量（内核统计聚合用） */
    Long sumPortalVisits();

    Boolean insertByBo(InfoResourceBo bo);

    Boolean insertPortalByBo(InfoResourceBo bo);

    Boolean updateByBo(InfoResourceBo bo);

    Boolean updateOwnByBo(InfoResourceBo bo);

    Boolean changeStatus(Long resourceId, String status);

    Boolean changeOwnStatus(Long resourceId, String status);

    Boolean deleteOwnById(Long resourceId);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
