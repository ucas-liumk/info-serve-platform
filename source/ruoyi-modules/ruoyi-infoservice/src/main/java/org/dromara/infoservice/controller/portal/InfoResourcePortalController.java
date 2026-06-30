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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources")
public class InfoResourcePortalController {

    private static final int MINIO_INTERNAL_PORT = 9000;
    private static final int MINIO_EXTERNAL_PORT = 8160;
    private static final int KKFILEVIEW_EXTERNAL_PORT = 8012;
    private static final String MINIO_INTERNAL_HOST = "minio";
    private static final Set<String> OFFICE_SUFFIXES = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "wps", "et", "dps");
    private static final Pattern OFFICE_IMAGE_PATTERN = Pattern.compile("data-src=\"([^\"]+?\\.jpg)\\s*\"", Pattern.CASE_INSENSITIVE);

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

    @GetMapping("/{resourceId}/thumbnail")
    public void thumbnail(@PathVariable Long resourceId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        InfoResourceVo resource = resourceService.queryPortalReadableDetail(resourceId);
        if ("image".equals(resource.getPreviewType())) {
            response.sendRedirect(toBrowserReachableUrl(resourceService.resolveFileUrl(resourceId, false), request));
            return;
        }
        if ("pdf".equals(resource.getPreviewType())) {
            resourceService.writePdfThumbnail(resourceId, response);
            return;
        }
        if (isOfficeResource(resource)) {
            String firstPageImageUrl = resolveOfficeFirstPageImageUrl(resourceId, resource, request);
            if (StringUtils.isNotBlank(firstPageImageUrl)) {
                response.sendRedirect(firstPageImageUrl);
                return;
            }
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @GetMapping("/{resourceId}/kk-preview-url")
    public R<String> kkPreviewUrl(@PathVariable Long resourceId, HttpServletRequest request) {
        InfoResourceVo resource = resourceService.queryPortalReadableDetail(resourceId);
        String fileUrl = toKkFileViewReachableUrl(resourceService.resolveFileUrl(resourceId, false));
        String previewFileUrl = appendFullFileName(
            fileUrl,
            StringUtils.defaultIfBlank(resource.getOriginalName(), resource.getTitle())
        );
        return R.ok(buildKkPreviewUrl(previewFileUrl, request));
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
            boolean internalMinio = MINIO_INTERNAL_HOST.equalsIgnoreCase(host) || (loopbackHost && port == MINIO_INTERNAL_PORT);
            boolean mappedLocalMinio = loopbackHost && port == MINIO_EXTERNAL_PORT;
            if (internalMinio || mappedLocalMinio) {
                return rebuildUrl(uri, resolveExternalHost(request), mappedLocalMinio ? port : MINIO_EXTERNAL_PORT);
            }
        } catch (Exception ignored) {
            return url;
        }
        return url;
    }

    private String toKkFileViewReachableUrl(String url) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            boolean minioUrl = MINIO_INTERNAL_HOST.equalsIgnoreCase(uri.getHost())
                || port == MINIO_INTERNAL_PORT
                || port == MINIO_EXTERNAL_PORT;
            if (minioUrl) {
                return rebuildUrl(uri, MINIO_INTERNAL_HOST, MINIO_INTERNAL_PORT);
            }
        } catch (Exception ignored) {
            return url;
        }
        return url;
    }

    private String toBrowserReachableKkFileViewUrl(String url, HttpServletRequest request) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            int port = uri.getPort();
            boolean loopbackHost = "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host);
            if (loopbackHost && (port == KKFILEVIEW_EXTERNAL_PORT || port == -1)) {
                return rebuildUrl(uri, resolveExternalHost(request), port == -1 ? KKFILEVIEW_EXTERNAL_PORT : port);
            }
        } catch (Exception ignored) {
            return url;
        }
        return url;
    }

    private String resolveOfficeFirstPageImageUrl(Long resourceId, InfoResourceVo resource, HttpServletRequest request) throws IOException {
        String fileUrl = toKkFileViewReachableUrl(resourceService.resolveFileUrl(resourceId, false));
        String previewFileUrl = appendFullFileName(
            fileUrl,
            StringUtils.defaultIfBlank(resource.getOriginalName(), resource.getTitle())
        );
        String previewHtml = fetchText(buildKkPreviewUrl(previewFileUrl, request));
        Matcher matcher = OFFICE_IMAGE_PATTERN.matcher(previewHtml);
        if (!matcher.find()) {
            return StringUtils.EMPTY;
        }
        return toBrowserReachableKkFileViewUrl(matcher.group(1).trim(), request);
    }

    private String buildKkPreviewUrl(String previewFileUrl, HttpServletRequest request) {
        String encodedUrl = URLEncoder.encode(
            Base64.getEncoder().encodeToString(previewFileUrl.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
        );
        return resolveKkFileViewBaseUrl(request) + "/onlinePreview?url=" + encodedUrl;
    }

    private String fetchText(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(120_000);
        connection.setRequestProperty("Accept", "text/html,*/*");
        int status = connection.getResponseCode();
        if (status >= HttpServletResponse.SC_BAD_REQUEST) {
            connection.disconnect();
            return StringUtils.EMPTY;
        }
        try (InputStream inputStream = connection.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private boolean isOfficeResource(InfoResourceVo resource) {
        if ("office".equals(resource.getPreviewType())) {
            return true;
        }
        return OFFICE_SUFFIXES.contains(StringUtils.defaultString(resource.getFileSuffix()).toLowerCase());
    }

    private String rebuildUrl(URI uri, String host, int port) {
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://");
        if (StringUtils.isNotBlank(uri.getRawUserInfo())) {
            builder.append(uri.getRawUserInfo()).append('@');
        }
        builder.append(formatHost(host));
        if (port >= 0) {
            builder.append(':').append(port);
        }
        builder.append(StringUtils.defaultString(uri.getRawPath()));
        if (StringUtils.isNotBlank(uri.getRawQuery())) {
            builder.append('?').append(uri.getRawQuery());
        }
        if (StringUtils.isNotBlank(uri.getRawFragment())) {
            builder.append('#').append(uri.getRawFragment());
        }
        return builder.toString();
    }

    private String formatHost(String host) {
        if (StringUtils.isBlank(host) || host.startsWith("[") || !host.contains(":")) {
            return host;
        }
        return "[" + host + "]";
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
