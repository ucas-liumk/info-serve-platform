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

    void downloadFile(Long resourceId, HttpServletResponse response) throws IOException;

    void favorite(Long resourceId, boolean add);

    Boolean insertByBo(InfoResourceBo bo);

    Boolean insertPortalByBo(InfoResourceBo bo);

    Boolean updateByBo(InfoResourceBo bo);

    Boolean updateOwnByBo(InfoResourceBo bo);

    Boolean changeStatus(Long resourceId, String status);

    Boolean changeOwnStatus(Long resourceId, String status);

    Boolean deleteOwnById(Long resourceId);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
