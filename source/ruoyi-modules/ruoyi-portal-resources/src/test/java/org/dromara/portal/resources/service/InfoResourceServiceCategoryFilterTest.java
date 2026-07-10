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
 * C2 列表接口 categoryCode 多值：'all' 哨兵跳过、单值 eq 语义 bit 级保留、
 * 多值部分命中 IN(命中集)、全无命中 eq(category_id, -1) 空集。
 * 另验资料只允许挂二级分类（栏目不直接挂资料）。
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
    void single_code_hit_keeps_eq_semantics() {
        when(categoryMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(activeCategory(300001L, "policy")));

        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("policy"));

        String sql = w.getSqlSegment();
        assertTrue(sql.contains("category_id ="), "单值命中保持 eq 语义");
        assertFalse(sql.contains("category_id IN"));
        assertTrue(w.getParamNameValuePairs().containsValue(300001L));
    }

    @Test
    void multi_codes_partial_hit_filters_in_matched_ids() {
        when(categoryMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(activeCategory(300001L, "policy"), activeCategory(300002L, "tech")));

        LambdaQueryWrapper<InfoResource> w = capturePortalWrapper(portalBo("policy,tech,missing"));

        assertTrue(w.getSqlSegment().contains("category_id IN"));
        assertTrue(w.getParamNameValuePairs().containsValue(300001L));
        assertTrue(w.getParamNameValuePairs().containsValue(300002L));
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
        InfoResource entity = new InfoResource();
        entity.setCategoryId(300000L);
        InfoResourceCategory section = new InfoResourceCategory();
        section.setCategoryId(300000L);
        section.setStatus("0");
        section.setParentId(null);
        when(categoryMapper.selectById(300000L)).thenReturn(section);

        try (MockedStatic<MapstructUtils> ms = mockStatic(MapstructUtils.class)) {
            ms.when(() -> MapstructUtils.convert(bo, InfoResource.class)).thenReturn(entity);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.insertByBo(bo));
            assertTrue(ex.getMessage().contains("分类"));
        }
        verify(baseMapper, never()).insert(any(InfoResource.class));
    }
}
