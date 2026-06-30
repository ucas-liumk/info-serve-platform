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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources")
public class InfoResourcePortalController {

    private static final int MINIO_INTERNAL_PORT = 9000;
    private static final int MINIO_EXTERNAL_PORT = 8160;
    private static final int KKFILEVIEW_EXTERNAL_PORT = 8012;

    private final IInfoResourceCategoryService categoryService;
    private final IInfoResourceService resourceService;

    @Value("${infoservice.kkfileview.base-url:}")
    private String kkFileViewBaseUrl;

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

    @GetMapping("/{resourceId}/kk-preview-url")
    public R<String> kkPreviewUrl(@PathVariable Long resourceId, HttpServletRequest request) {
        InfoResourceVo resource = resourceService.queryPortalReadableDetail(resourceId);
        String fileUrl = toBrowserReachableUrl(resourceService.resolveFileUrl(resourceId, false), request);
        String previewFileUrl = appendFullFileName(
            fileUrl,
            StringUtils.defaultIfBlank(resource.getOriginalName(), resource.getTitle())
        );
        String encodedUrl = URLEncoder.encode(
            Base64.getEncoder().encodeToString(previewFileUrl.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
        );
        return R.ok(resolveKkFileViewBaseUrl(request) + "/onlinePreview?url=" + encodedUrl);
    }

    @GetMapping("/{resourceId}/download")
    public void download(@PathVariable Long resourceId, HttpServletResponse response) throws IOException {
        resourceService.downloadFile(resourceId, response);
    }

    private String toBrowserReachableUrl(String url, HttpServletRequest request) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            int port = uri.getPort();
            boolean loopbackHost = "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host);
            boolean internalMinio = "minio".equalsIgnoreCase(host) || (loopbackHost && port == MINIO_INTERNAL_PORT);
            boolean mappedLocalMinio = loopbackHost && port == MINIO_EXTERNAL_PORT;
            if (internalMinio || mappedLocalMinio) {
                return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    resolveExternalHost(request),
                    mappedLocalMinio ? port : MINIO_EXTERNAL_PORT,
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

    private String appendFullFileName(String url, String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "fullfilename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
    }

    private String resolveKkFileViewBaseUrl(HttpServletRequest request) {
        String baseUrl = kkFileViewBaseUrl;
        if (StringUtils.isBlank(baseUrl)) {
            baseUrl = resolveExternalScheme(request) + "://" + resolveExternalHost(request) + ":" + KKFILEVIEW_EXTERNAL_PORT;
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String resolveExternalScheme(HttpServletRequest request) {
        String scheme = StringUtils.defaultIfBlank(request.getHeader("X-Forwarded-Proto"), request.getHeader("X-Forwarded-Protocol"));
        scheme = StringUtils.defaultIfBlank(scheme, request.getHeader("X-Url-Scheme"));
        if (StringUtils.isBlank(scheme)) {
            return request.getScheme();
        }
        return scheme.split(",")[0].trim();
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
