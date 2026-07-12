package org.dromara.portal.resources.service;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.portalevent.publisher.PortalEventPublisher;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.InfoResourceCategory;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;
import org.dromara.portal.resources.mapper.InfoResourceCategoryLinkMapper;
import org.dromara.portal.resources.mapper.InfoResourceCategoryMapper;
import org.dromara.portal.resources.mapper.InfoResourceFavoriteMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.resources.mapper.InfoResourceViewRecordMapper;
import org.dromara.portal.resources.service.impl.InfoResourceServiceImpl;
import org.dromara.portal.resources.support.DocumentPreviewConverter;
import org.dromara.portal.resources.support.ResourceUserDisplayNameResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * C2 列表接口 categoryCode 多值：'all' 哨兵跳过、全无命中 eq(category_id, -1) 空集、
 * 命中走关联表 EXISTS（资料多分类事实源，任一分类命中即可见）。
 * 另验资料只允许挂二级分类（栏目不直接挂资料）与多分类写入整替关联行。
 */
@ExtendWith(MockitoExtension.class)
class InfoResourceServiceCategoryFilterTest {

    /**
     * 初始化实体 TableInfo 以便渲染 LambdaQueryWrapper SQL 片段；
     * MapstructUtils 类初始化依赖 Spring 容器，须先在受控 SpringUtil 静态 mock 下完成一次类初始化。
     */
    @BeforeAll
    static void initStatics() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Converter.class)).thenReturn(mock(Converter.class));
            Class.forName("org.dromara.common.core.utils.MapstructUtils");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, InfoResource.class);
        TableInfoHelper.initTableInfo(assistant, InfoResourceCategory.class);
    }

    @Mock
    private InfoResourceMapper baseMapper;
    @Mock
    private InfoResourceCategoryMapper categoryMapper;
    @Mock
    private InfoResourceCategoryLinkMapper categoryLinkMapper;
    @Mock
    private InfoResourceFavoriteMapper favoriteMapper;
    @Mock
    private InfoResourceViewRecordMapper viewRecordMapper;
    @Mock
    private PortalEventPublisher eventPublisher;
    @Mock
    private ResourceUserDisplayNameResolver userDisplayNameResolver;
    @Mock
    private DocumentPreviewConverter previewConverter;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private InfoResourceServiceImpl service;

    private InfoResourceBo portalBo(String categoryCode) {
        InfoResourceBo bo = new InfoResourceBo();
        bo.setCategoryCode(categoryCode);
        return bo;
    }

    private InfoResourceCategory activeCategory(Long id, String code) {
        InfoResourceCategory c = new InfoResourceCategory();
        c.setCategoryId(id);
        c.setCategoryCode(code);
        c.setParentId(300000L);
        c.setStatus("0");
        return c;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private LambdaQueryWrapper<InfoResource> capturePortalWrapper(InfoResourceBo bo) {
        doReturn(new Page<InfoResourceVo>()).when(baseMapper).selectVoPage(any(), any());
        service.portalPage(bo, new PageQuery(10, 1));
        ArgumentCaptor<LambdaQueryWrapper<InfoResource>> captor =
            ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(baseMapper).selectVoPage(any(), captor.capture());
        return captor.getValue();
    }

    @Test
    void all_sentinel_skips_category_filter_and_category_lookup() {
        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("all"));
        assertFalse(w.getSqlSegment().contains("category_id"));
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void blank_code_skips_category_filter() {
        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("  "));
        assertFalse(w.getSqlSegment().contains("category_id"));
        verifyNoInteractions(categoryMapper);
    }

    @Test
    void single_code_hit_filters_via_link_exists() {
        when(categoryMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(activeCategory(300001L, "policy")));

        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("policy"));

        String sql = w.getSqlSegment();
        assertTrue(sql.contains("info_resource_category_link"), "命中走关联表 EXISTS");
        assertTrue(sql.contains("info_resource.resource_id"), "外层列必须带表名限定，防子查询自指恒真");
        assertTrue(sql.contains("(300001)"));
    }

    @Test
    void multi_codes_partial_hit_filters_via_link_exists_in_matched_ids() {
        when(categoryMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(activeCategory(300001L, "policy"), activeCategory(300002L, "tech")));

        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("policy,tech,missing"));

        String sql = w.getSqlSegment();
        assertTrue(sql.contains("info_resource_category_link"));
        assertTrue(sql.contains("300001, 300002"));
    }

    @Test
    void legacy_single_category_id_param_filters_via_link_exists() {
        InfoResourceBo bo = new InfoResourceBo();
        bo.setCategoryId(300001L);

        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(bo);

        String sql = w.getSqlSegment();
        assertTrue(sql.contains("info_resource_category_link"));
        assertTrue(sql.contains("(300001)"));
    }

    @Test
    void no_hit_yields_minus_one_empty_set_semantics() {
        when(categoryMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("ghost"));

        assertTrue(w.getSqlSegment().contains("category_id ="));
        assertTrue(w.getParamNameValuePairs().containsValue(-1L));
    }

    @Test
    void insert_rejects_resource_attached_to_section_level_category() {
        InfoResourceBo bo = new InfoResourceBo();
        bo.setCategoryId(300000L);
        InfoResourceCategory section = new InfoResourceCategory();
        section.setCategoryId(300000L);
        section.setStatus("0");
        section.setParentId(null);
        when(categoryMapper.selectById(300000L)).thenReturn(section);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertByBo(bo));
        assertTrue(ex.getMessage().contains("分类"));
        verify(baseMapper, never()).insert(any(InfoResource.class));
        verifyNoInteractions(categoryLinkMapper);
    }

    @Test
    void insert_with_multiple_categories_writes_all_links_and_first_as_primary() {
        InfoResourceBo bo = new InfoResourceBo();
        bo.setCategoryId(300002L);
        bo.setCategoryIds(List.of(300001L, 300002L, 300001L));
        InfoResourceCategory sectionRow = new InfoResourceCategory();
        sectionRow.setCategoryId(300000L);
        sectionRow.setStatus("0");
        sectionRow.setParentId(null);
        when(categoryMapper.selectById(300000L)).thenReturn(sectionRow);
        when(categoryMapper.selectById(300001L)).thenReturn(activeCategory(300001L, "policy"));
        when(categoryMapper.selectById(300002L)).thenReturn(activeCategory(300002L, "tech"));
        InfoResource entity = new InfoResource();
        entity.setResourceId(42L);
        when(baseMapper.insert(entity)).thenReturn(1);

        try (MockedStatic<MapstructUtils> ms = mockStatic(MapstructUtils.class)) {
            ms.when(() -> MapstructUtils.convert(bo, InfoResource.class)).thenReturn(entity);
            assertTrue(service.insertByBo(bo));
        }

        // categoryIds 优先且去重保序：主分类=首个（300001，而非 bo.categoryId 的 300002）
        assertTrue(Long.valueOf(300001L).equals(entity.getCategoryId()));
        verify(categoryLinkMapper).deleteByResourceId(42L);
        verify(categoryLinkMapper).insertLinks(42L, List.of(300001L, 300002L));
    }
}
