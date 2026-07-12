package org.dromara.portal.resources.support;

import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C1 分面计数入参映射：与列表接口 buildWrapper 筛选语义一致
 * （previewType/fileType defaultIfBlank 合流、'all' 哨兵、uploadedWithin 时间窗、sizeRange 字节界）。
 */
class ResourceFacetParamsTest {

    private static final long ONE_MB = 1024L * 1024L;

    private InfoResourceBo bo() {
        return new InfoResourceBo();
    }

    @Test
    void empty_bo_maps_to_all_null() {
        ResourceFacetParams params = ResourceFacetParams.from(bo());
        assertNull(params.getKeyword());
        assertNull(params.getPreviewType());
        assertNull(params.getUploadedSince());
        assertNull(params.getMinFileSize());
        assertNull(params.getMaxFileSize());
    }

    @Test
    void keyword_blank_is_null_and_raw_value_kept() {
        InfoResourceBo bo = bo();
        bo.setKeyword("  ");
        assertNull(ResourceFacetParams.from(bo).getKeyword());
        bo.setKeyword("安全");
        assertEquals("安全", ResourceFacetParams.from(bo).getKeyword());
    }

    @Test
    void preview_type_merges_file_type_when_blank() {
        InfoResourceBo bo = bo();
        bo.setFileType("pdf");
        assertEquals("pdf", ResourceFacetParams.from(bo).getPreviewType());

        bo.setPreviewType("office");
        assertEquals("office", ResourceFacetParams.from(bo).getPreviewType());
    }

    @Test
    void preview_type_all_sentinel_means_no_filter_even_with_file_type() {
        InfoResourceBo bo = bo();
        bo.setPreviewType("all");
        bo.setFileType("pdf");
        assertNull(ResourceFacetParams.from(bo).getPreviewType());

        InfoResourceBo bo2 = bo();
        bo2.setFileType("all");
        assertNull(ResourceFacetParams.from(bo2).getPreviewType());
    }

    @Test
    void uploaded_within_week_maps_to_seven_days_ago() {
        InfoResourceBo bo = bo();
        bo.setUploadedWithin("week");
        Date since = ResourceFacetParams.from(bo).getUploadedSince();
        assertNotNull(since);
        long diff = System.currentTimeMillis() - since.getTime();
        assertTrue(diff > Duration.ofDays(6).toMillis(), "应约为 7 天前");
        assertTrue(diff < Duration.ofDays(8).toMillis(), "应约为 7 天前");
    }

    @Test
    void uploaded_within_all_or_unknown_means_no_filter() {
        InfoResourceBo bo = bo();
        bo.setUploadedWithin("all");
        assertNull(ResourceFacetParams.from(bo).getUploadedSince());
        bo.setUploadedWithin("bogus");
        assertNull(ResourceFacetParams.from(bo).getUploadedSince());
    }

    @Test
    void size_range_maps_to_byte_bounds() {
        InfoResourceBo bo = bo();
        bo.setSizeRange("small");
        ResourceFacetParams small = ResourceFacetParams.from(bo);
        assertNull(small.getMinFileSize());
        assertEquals(ONE_MB, small.getMaxFileSize());

        bo.setSizeRange("medium");
        ResourceFacetParams medium = ResourceFacetParams.from(bo);
        assertEquals(ONE_MB, medium.getMinFileSize());
        assertEquals(10 * ONE_MB, medium.getMaxFileSize());

        bo.setSizeRange("large");
        ResourceFacetParams large = ResourceFacetParams.from(bo);
        assertEquals(10 * ONE_MB, large.getMinFileSize());
        assertNull(large.getMaxFileSize());

        bo.setSizeRange("all");
        ResourceFacetParams all = ResourceFacetParams.from(bo);
        assertNull(all.getMinFileSize());
        assertNull(all.getMaxFileSize());
    }
}
