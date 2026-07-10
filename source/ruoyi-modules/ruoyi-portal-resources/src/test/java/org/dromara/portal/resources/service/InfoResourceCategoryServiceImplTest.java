package org.dromara.portal.resources.service;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.linpeilie.Converter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.InfoResourceCategory;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.bo.InfoResourceCategoryBo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryCountVo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryTreeVo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryVo;
import org.dromara.portal.resources.mapper.InfoResourceCategoryMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.resources.service.impl.InfoResourceCategoryServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 栏目/分类两级体系：树组装（C1）、平铺只出二级（C3）、树表全量（C4）、CRUD 校验（C5）。
 * 纯单测（无 Spring 上下文），范本 ResourceConvertListenerTest。
 */
@ExtendWith(MockitoExtension.class)
class InfoResourceCategoryServiceImplTest {

    private static final long ONE_MB = 1024L * 1024L;

    /**
     * MapstructUtils.CONVERTER 在类初始化时经 SpringUtils.getBean(Converter) 取 bean，
     * 纯单测须先在受控的 SpringUtil 静态 mock 下强制完成类初始化，之后才能安全 mockStatic(MapstructUtils)。
     * 同时初始化实体 TableInfo，允许在断言中渲染 LambdaQueryWrapper 的 SQL 片段。
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
        TableInfoHelper.initTableInfo(assistant, InfoResourceCategory.class);
        TableInfoHelper.initTableInfo(assistant, InfoResource.class);
    }

    @Mock
    private InfoResourceCategoryMapper baseMapper;

    @Mock
    private InfoResourceMapper resourceMapper;

    @InjectMocks
    private InfoResourceCategoryServiceImpl service;

    private InfoResourceCategory category(Long id, Long parentId, String code, String name, int orderNum) {
        InfoResourceCategory c = new InfoResourceCategory();
        c.setCategoryId(id);
        c.setParentId(parentId);
        c.setCategoryCode(code);
        c.setCategoryName(name);
        c.setOrderNum(orderNum);
        c.setStatus("0");
        return c;
    }

    private InfoResourceCategoryCountVo count(Long categoryId, Long resourceCount) {
        InfoResourceCategoryCountVo vo = new InfoResourceCategoryCountVo();
        vo.setCategoryId(categoryId);
        vo.setResourceCount(resourceCount);
        return vo;
    }

    // ---------- C1 树组装 ----------

    @Test
    void portalCategoryTree_assembles_two_levels_drops_empty_section_and_orphan() {
        InfoResourceCategory sectionA = category(300000L, null, "general", "综合资料", 0);
        InfoResourceCategory sectionEmpty = category(400000L, null, "empty", "空栏目", 1);
        InfoResourceCategory child1 = category(300001L, 300000L, "policy", "政策制度", 1);
        InfoResourceCategory child2 = category(300002L, 300000L, "tech", "技术文档", 2);
        InfoResourceCategory orphan = category(500001L, 999999L, "orphan", "孤儿分类", 3);
        when(baseMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(sectionA, sectionEmpty, child1, child2, orphan));
        when(resourceMapper.countActiveByCategory(any(), any(), any(), any(), any()))
            .thenReturn(List.of(count(300001L, 6L)));

        List<InfoResourceCategoryTreeVo> tree = service.portalCategoryTree(new InfoResourceBo());

        assertEquals(1, tree.size(), "空栏目与孤儿分类不出现");
        InfoResourceCategoryTreeVo root = tree.get(0);
        assertEquals(300000L, root.getCategoryId());
        assertEquals("general", root.getCategoryCode());
        assertEquals("综合资料", root.getCategoryName());
        assertNull(root.getResourceCount(), "栏目节点不带计数（组计数由前端求和）");
        assertEquals(2, root.getChildren().size());
        assertEquals(300001L, root.getChildren().get(0).getCategoryId());
        assertEquals(6L, root.getChildren().get(0).getResourceCount());
        assertEquals(300002L, root.getChildren().get(1).getCategoryId());
        assertEquals(0L, root.getChildren().get(1).getResourceCount(), "无资料分类计数补 0");
    }

    @Test
    void portalCategoryTree_without_children_returns_empty_and_skips_count_query() {
        when(baseMapper.selectList(any(Wrapper.class)))
            .thenReturn(List.of(category(300000L, null, "general", "综合资料", 0)));

        List<InfoResourceCategoryTreeVo> tree = service.portalCategoryTree(new InfoResourceBo());

        assertTrue(tree.isEmpty());
        verify(resourceMapper, never()).countActiveByCategory(any(), any(), any(), any(), any());
    }

    @Test
    void portalCategoryTree_passes_facet_params_to_single_aggregate_query() {
        when(baseMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
            category(300000L, null, "general", "综合资料", 0),
            category(300001L, 300000L, "policy", "政策制度", 1)));
        when(resourceMapper.countActiveByCategory(any(), any(), any(), any(), any()))
            .thenReturn(List.of());

        InfoResourceBo bo = new InfoResourceBo();
        bo.setKeyword("安全");
        bo.setFileType("pdf");
        bo.setUploadedWithin("week");
        bo.setSizeRange("medium");
        service.portalCategoryTree(bo);

        verify(resourceMapper).countActiveByCategory(eq("安全"), eq("pdf"),
            argThat(date -> date != null), eq(ONE_MB), eq(10 * ONE_MB));
    }

    // ---------- C3 平铺接口只出二级 ----------

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void queryList_only_returns_child_level_with_counts() {
        InfoResourceCategoryVo vo = new InfoResourceCategoryVo();
        vo.setCategoryId(300001L);
        when(baseMapper.selectVoList(any(Wrapper.class))).thenReturn(List.of(vo));
        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(5L);

        List<InfoResourceCategoryVo> rows = service.queryList(new InfoResourceCategoryBo());

        assertEquals(5L, rows.get(0).getResourceCount());
        ArgumentCaptor<LambdaQueryWrapper<InfoResourceCategory>> captor =
            ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(baseMapper).selectVoList(captor.capture());
        assertTrue(captor.getValue().getSqlSegment().contains("parent_id IS NOT NULL"),
            "平铺接口须追加 parent_id IS NOT NULL");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void portalCategories_filters_active_child_level() {
        when(baseMapper.selectVoList(any(Wrapper.class))).thenReturn(List.of());

        service.portalCategories();

        ArgumentCaptor<LambdaQueryWrapper<InfoResourceCategory>> captor =
            ArgumentCaptor.forClass((Class) LambdaQueryWrapper.class);
        verify(baseMapper).selectVoList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("status ="));
        assertTrue(sql.contains("parent_id IS NOT NULL"));
        assertTrue(captor.getValue().getParamNameValuePairs().containsValue("0"));
    }

    // ---------- C4 管理树表全量 ----------

    @Test
    void queryTreeList_returns_all_rows_with_aggregated_counts() {
        InfoResourceCategoryVo section = new InfoResourceCategoryVo();
        section.setCategoryId(300000L);
        InfoResourceCategoryVo child = new InfoResourceCategoryVo();
        child.setCategoryId(300001L);
        child.setParentId(300000L);
        when(baseMapper.selectVoList(any(Wrapper.class))).thenReturn(List.of(section, child));
        when(resourceMapper.countActiveByCategory(isNull(), isNull(), isNull(), isNull(), isNull()))
            .thenReturn(List.of(count(300001L, 3L)));

        List<InfoResourceCategoryVo> rows = service.queryTreeList();

        assertEquals(2, rows.size());
        assertEquals(0L, rows.get(0).getResourceCount());
        assertEquals(3L, rows.get(1).getResourceCount());
    }

    // ---------- C5 新增/修改校验（两级封顶 + 编码唯一） ----------

    @Test
    void insert_child_rejected_when_parent_missing() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setCategoryName("政策制度");
        bo.setCategoryCode("policy");
        bo.setParentId(1L);
        when(baseMapper.selectById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertByBo(bo));
        assertTrue(ex.getMessage().contains("上级栏目"));
        verify(baseMapper, never()).insert(any(InfoResourceCategory.class));
    }

    @Test
    void insert_child_rejected_when_parent_is_child_level() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setCategoryName("三级分类");
        bo.setCategoryCode("l3");
        bo.setParentId(300001L);
        when(baseMapper.selectById(300001L))
            .thenReturn(category(300001L, 300000L, "policy", "政策制度", 1));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertByBo(bo));
        assertTrue(ex.getMessage().contains("两级"));
        verify(baseMapper, never()).insert(any(InfoResourceCategory.class));
    }

    @Test
    void update_rejected_when_parent_is_self() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setCategoryId(300000L);
        bo.setCategoryName("综合资料");
        bo.setCategoryCode("general");
        bo.setParentId(300000L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateByBo(bo));
        assertTrue(ex.getMessage().contains("自己"));
    }

    @Test
    void update_section_with_children_cannot_become_child() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setCategoryId(300000L);
        bo.setCategoryName("综合资料");
        bo.setCategoryCode("general");
        bo.setParentId(600000L);
        when(baseMapper.selectById(600000L))
            .thenReturn(category(600000L, null, "other", "其他栏目", 9));
        when(baseMapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.updateByBo(bo));
        assertTrue(ex.getMessage().contains("分类"));
    }

    @Test
    void insert_rejected_when_code_duplicated_across_levels() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setCategoryName("新栏目");
        bo.setCategoryCode("general");
        when(baseMapper.exists(any(Wrapper.class))).thenReturn(true);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertByBo(bo));
        assertTrue(ex.getMessage().contains("编码"));
    }

    @Test
    void insert_section_and_child_succeed_when_valid() {
        InfoResourceCategory entity = new InfoResourceCategory();
        when(baseMapper.exists(any(Wrapper.class))).thenReturn(false);
        when(baseMapper.insert(entity)).thenReturn(1);

        try (MockedStatic<MapstructUtils> ms = mockStatic(MapstructUtils.class)) {
            InfoResourceCategoryBo sectionBo = new InfoResourceCategoryBo();
            sectionBo.setCategoryName("新栏目");
            sectionBo.setCategoryCode("newsec");
            ms.when(() -> MapstructUtils.convert(sectionBo, InfoResourceCategory.class)).thenReturn(entity);
            assertTrue(service.insertByBo(sectionBo));

            InfoResourceCategoryBo childBo = new InfoResourceCategoryBo();
            childBo.setCategoryName("新分类");
            childBo.setCategoryCode("newchild");
            childBo.setParentId(300000L);
            when(baseMapper.selectById(300000L))
                .thenReturn(category(300000L, null, "general", "综合资料", 0));
            ms.when(() -> MapstructUtils.convert(childBo, InfoResourceCategory.class)).thenReturn(entity);
            assertTrue(service.insertByBo(childBo));
        }
    }

    // ---------- C5 删除校验 ----------

    @Test
    void delete_section_rejected_when_children_exist() {
        when(baseMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.deleteWithValidByIds(List.of(300000L)));
        assertTrue(ex.getMessage().contains("栏目"));
        verify(baseMapper, never()).deleteByIds(anyCollection());
    }

    @Test
    void delete_child_rejected_when_resources_attached() {
        when(baseMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.deleteWithValidByIds(List.of(300001L)));
        assertTrue(ex.getMessage().contains("资料"));
        verify(baseMapper, never()).deleteByIds(anyCollection());
    }

    @Test
    void delete_succeeds_when_no_children_and_no_resources() {
        when(baseMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(resourceMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(baseMapper.deleteByIds(List.of(300002L))).thenReturn(1);

        assertTrue(service.deleteWithValidByIds(List.of(300002L)));
    }

    @Test
    void delete_empty_ids_returns_false_without_touching_db() {
        assertFalse(service.deleteWithValidByIds(List.of()));
        assertFalse(service.deleteWithValidByIds(null));
        verify(baseMapper, never()).deleteByIds(anyCollection());
    }
}
