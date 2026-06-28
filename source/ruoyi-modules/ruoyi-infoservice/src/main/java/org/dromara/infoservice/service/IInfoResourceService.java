package org.dromara.infoservice.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.infoservice.domain.bo.InfoResourceBo;
import org.dromara.infoservice.domain.vo.InfoResourceVo;
import org.dromara.infoservice.domain.vo.ResourceUploadVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

public interface IInfoResourceService {

    TableDataInfo<InfoResourceVo> queryPageList(InfoResourceBo bo, PageQuery pageQuery);

    TableDataInfo<InfoResourceVo> portalPage(InfoResourceBo bo, PageQuery pageQuery);

    InfoResourceVo queryById(Long resourceId);

    InfoResourceVo queryPortalDetail(Long resourceId);

    ResourceUploadVo upload(MultipartFile file);

    ResourceUploadVo portalUpload(MultipartFile file);

    String resolveFileUrl(Long resourceId, boolean download);

    Boolean insertByBo(InfoResourceBo bo);

    Boolean insertPortalByBo(InfoResourceBo bo);

    Boolean updateByBo(InfoResourceBo bo);

    Boolean updateOwnByBo(InfoResourceBo bo);

    Boolean changeStatus(Long resourceId, String status);

    Boolean changeOwnStatus(Long resourceId, String status);

    Boolean deleteOwnById(Long resourceId);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
