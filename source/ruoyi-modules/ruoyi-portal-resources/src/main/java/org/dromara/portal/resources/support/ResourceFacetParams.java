package org.dromara.portal.resources.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;

import java.util.Calendar;
import java.util.Date;

/**
 * 门户资料分面筛选入参映射（关键词 + 工具条筛选，不含分类维度自身）。
 * 语义与 InfoResourceServiceImpl#buildWrapper 列表筛选保持一致：
 * previewType 与 fileType defaultIfBlank 合流；'all' 哨兵视为未筛选；
 * uploadedWithin 映射为起始时间；sizeRange 映射为 [min, max) 字节界。
 * 不可变对象：字段构造后不再变化。
 */
public final class ResourceFacetParams {

    private static final long ONE_MB = 1024L * 1024L;

    private final String keyword;
    private final String previewType;
    private final Date uploadedSince;
    private final Long minFileSize;
    private final Long maxFileSize;

    private ResourceFacetParams(String keyword, String previewType, Date uploadedSince,
                                Long minFileSize, Long maxFileSize) {
        this.keyword = keyword;
        this.previewType = previewType;
        this.uploadedSince = uploadedSince;
        this.minFileSize = minFileSize;
        this.maxFileSize = maxFileSize;
    }

    public static ResourceFacetParams from(InfoResourceBo bo) {
        String keyword = StringUtils.isBlank(bo.getKeyword()) ? null : bo.getKeyword();
        String previewType = StringUtils.defaultIfBlank(bo.getPreviewType(), bo.getFileType());
        if (!isActiveFilter(previewType)) {
            previewType = null;
        }
        Date uploadedSince = resolveUploadedSince(bo.getUploadedWithin());
        Long minFileSize = null;
        Long maxFileSize = null;
        if (isActiveFilter(bo.getSizeRange())) {
            switch (bo.getSizeRange()) {
                case "small" -> maxFileSize = ONE_MB;
                case "medium" -> {
                    minFileSize = ONE_MB;
                    maxFileSize = 10 * ONE_MB;
                }
                case "large" -> minFileSize = 10 * ONE_MB;
                default -> {
                }
            }
        }
        return new ResourceFacetParams(keyword, previewType, uploadedSince, minFileSize, maxFileSize);
    }

    private static boolean isActiveFilter(String value) {
        return StringUtils.isNotBlank(value) && !"all".equals(value);
    }

    private static Date resolveUploadedSince(String uploadedWithin) {
        if (!isActiveFilter(uploadedWithin)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        switch (uploadedWithin) {
            case "week" -> calendar.add(Calendar.DATE, -7);
            case "month" -> calendar.add(Calendar.MONTH, -1);
            case "quarter" -> calendar.add(Calendar.MONTH, -3);
            case "year" -> calendar.add(Calendar.YEAR, -1);
            default -> {
                return null;
            }
        }
        return calendar.getTime();
    }

    public String getKeyword() {
        return keyword;
    }

    public String getPreviewType() {
        return previewType;
    }

    /** Date 可变，返回防御性拷贝以保持本对象不可变 */
    public Date getUploadedSince() {
        return uploadedSince == null ? null : new Date(uploadedSince.getTime());
    }

    public Long getMinFileSize() {
        return minFileSize;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }
}
