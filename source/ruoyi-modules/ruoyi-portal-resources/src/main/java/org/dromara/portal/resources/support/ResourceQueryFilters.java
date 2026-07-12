package org.dromara.portal.resources.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.portal.resources.domain.InfoResource;

import java.util.Calendar;
import java.util.Date;

/**
 * 资料列表工具条筛选的纯查询助手（时间/大小/排序/'all' 哨兵）。
 * 语义与门户工具条枚举一一对应：uploadedWithin=week|month|quarter|year，
 * sizeRange=small|medium|large（1MB/10MB 分界），sort=downloads|views|hot|默认时间倒序。
 */
public final class ResourceQueryFilters {

    private static final long ONE_MB = 1024L * 1024L;

    private ResourceQueryFilters() {
    }

    /** 'all' 为跳过筛选的哨兵值 */
    public static boolean isActiveFilter(String value) {
        return StringUtils.isNotBlank(value) && !"all".equals(value);
    }

    public static Date resolveUploadedSince(String uploadedWithin) {
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

    public static void applySizeRange(LambdaQueryWrapper<InfoResource> w, String sizeRange) {
        if (!isActiveFilter(sizeRange)) {
            return;
        }
        switch (sizeRange) {
            case "small" -> w.lt(InfoResource::getFileSize, ONE_MB);
            case "medium" -> w.ge(InfoResource::getFileSize, ONE_MB).lt(InfoResource::getFileSize, 10 * ONE_MB);
            case "large" -> w.ge(InfoResource::getFileSize, 10 * ONE_MB);
            default -> {
            }
        }
    }

    public static void applySort(LambdaQueryWrapper<InfoResource> w, String sort) {
        if ("downloads".equals(sort)) {
            w.orderByDesc(InfoResource::getDownloadCount).orderByDesc(InfoResource::getCreateTime);
        } else if ("views".equals(sort)) {
            w.orderByDesc(InfoResource::getViewCount).orderByDesc(InfoResource::getCreateTime);
        } else if ("hot".equals(sort)) {
            w.orderByDesc(InfoResource::getDownloadCount).orderByDesc(InfoResource::getViewCount).orderByDesc(InfoResource::getCreateTime);
        } else {
            w.orderByDesc(InfoResource::getCreateTime);
        }
    }
}
