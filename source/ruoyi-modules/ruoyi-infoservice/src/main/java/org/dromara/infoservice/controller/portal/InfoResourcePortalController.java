package org.dromara.infoservice.controller.portal;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.infoservice.domain.bo.InfoResourceBo;
import org.dromara.infoservice.domain.vo.InfoResourceCategoryVo;
import org.dromara.infoservice.domain.vo.InfoResourceVo;
import org.dromara.infoservice.domain.vo.ResourceUploadVo;
import org.dromara.infoservice.service.IInfoResourceCategoryService;
import org.dromara.infoservice.service.IInfoResourceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources")
public class InfoResourcePortalController {

    private static final int MINIO_EXTERNAL_PORT = 19000;

    private final IInfoResourceCategoryService categoryService;
    private final IInfoResourceService resourceService;

    @GetMapping("/categories")
    public R<List<InfoResourceCategoryVo>> categories() {
        return R.ok(categoryService.portalCategories());
    }

    @GetMapping
    public TableDataInfo<InfoResourceVo> list(InfoResourceBo bo, PageQuery pageQuery) {
        return resourceService.portalPage(bo, pageQuery);
    }

    @GetMapping("/{resourceId}")
    public R<InfoResourceVo> detail(@PathVariable Long resourceId) {
        return R.ok(resourceService.queryPortalDetail(resourceId));
    }

    @Log(title = "门户资料上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public R<ResourceUploadVo> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(resourceService.portalUpload(file));
    }

    @Log(title = "门户资料发布", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody InfoResourceBo bo) {
        return Boolean.TRUE.equals(resourceService.insertPortalByBo(bo)) ? R.ok() : R.fail("发布资料失败");
    }

    @Log(title = "门户资料编辑", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{resourceId}")
    public R<Void> edit(@PathVariable Long resourceId, @Validated @RequestBody InfoResourceBo bo) {
        bo.setResourceId(resourceId);
        return Boolean.TRUE.equals(resourceService.updateOwnByBo(bo)) ? R.ok() : R.fail("保存资料失败");
    }

    @Log(title = "门户资料状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{resourceId}/status")
    public R<Void> changeStatus(@PathVariable Long resourceId, @RequestBody InfoResourceBo bo) {
        return Boolean.TRUE.equals(resourceService.changeOwnStatus(resourceId, bo.getStatus())) ? R.ok() : R.fail("更新状态失败");
    }

    @Log(title = "门户资料删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resourceId}")
    public R<Void> remove(@PathVariable Long resourceId) {
        return Boolean.TRUE.equals(resourceService.deleteOwnById(resourceId)) ? R.ok() : R.fail("删除资料失败");
    }

    @GetMapping("/{resourceId}/preview")
    public void preview(@PathVariable Long resourceId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(toBrowserReachableUrl(resourceService.resolveFileUrl(resourceId, false), request));
    }

    @GetMapping("/{resourceId}/download")
    public void download(@PathVariable Long resourceId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(toBrowserReachableUrl(resourceService.resolveFileUrl(resourceId, true), request));
    }

    private String toBrowserReachableUrl(String url, HttpServletRequest request) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            boolean internalMinio = "minio".equalsIgnoreCase(host) || ("127.0.0.1".equals(host) && uri.getPort() == 9000);
            if (internalMinio) {
                return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    resolveExternalHost(request),
                    MINIO_EXTERNAL_PORT,
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
                ).toString();
            }
        } catch (Exception ignored) {
            return url;
        }
        return url;
    }

    private String resolveExternalHost(HttpServletRequest request) {
        String host = StringUtils.defaultIfBlank(request.getHeader("X-External-Host"), request.getHeader("X-Forwarded-Host"));
        host = StringUtils.defaultIfBlank(host, request.getHeader("Host"));
        if (StringUtils.isBlank(host)) {
            return request.getServerName();
        }
        host = host.split(",")[0].trim();
        if (host.startsWith("[")) {
            int end = host.indexOf(']');
            return end > 0 ? host.substring(1, end) : request.getServerName();
        }
        int portIndex = host.lastIndexOf(':');
        return portIndex > 0 ? host.substring(0, portIndex) : host;
    }
}
