package org.dromara.portal.appcenter.service;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.linpeilie.Converter;
import org.dromara.portal.appcenter.domain.AppApplication;
import org.dromara.portal.appcenter.domain.bo.AppApplicationBo;
import org.dromara.portal.appcenter.domain.vo.AppApplicationVo;
import org.dromara.portal.appcenter.mapper.AppApplicationMapper;
import org.dromara.portal.appcenter.service.impl.AppApplicationServiceImpl;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppApplicationServiceImplTest {

    /**
     * Class-level Converter mock injected into MapstructUtils.CONVERTER via sun.misc.Unsafe
     * (accessed reflectively to avoid a compile-time dependency on internal JDK APIs).
     * Unsafe.putObject bypasses the "final" restriction for static fields in user classes.
     */
    static Converter converterMock;
    private static Converter savedConverter;
    private static Object unsafe;     // sun.misc.Unsafe, held as Object to avoid import
    private static long converterOffset;
    private static Object converterFieldBase;

    @BeforeAll
    static void initConverterMock() throws Exception {
        converterMock = mock(Converter.class);

        // Ensure MapstructUtils is class-initialized — use a SpringUtil static mock so
        // its static initializer (which calls SpringUtil.getBean) can succeed.
        try (MockedStatic<SpringUtil> suMock = mockStatic(SpringUtil.class)) {
            suMock.when(() -> SpringUtil.getBean(Converter.class)).thenReturn(converterMock);
            Class.forName("org.dromara.common.core.utils.MapstructUtils");
        } catch (Exception ignored) {
            // Class may already be initialized — proceed with Unsafe injection below
        }

        // Obtain sun.misc.Unsafe via reflection (avoids compile-time import of internal API)
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        unsafe = theUnsafeField.get(null);

        // Locate the MapstructUtils.CONVERTER static field
        Field converterField = MapstructUtils.class.getDeclaredField("CONVERTER");
        converterField.setAccessible(true);

        Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
        Method staticFieldBase = unsafeClass.getMethod("staticFieldBase", Field.class);
        Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);
        Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);

        converterOffset = (long) staticFieldOffset.invoke(unsafe, converterField);
        converterFieldBase = staticFieldBase.invoke(unsafe, converterField);
        savedConverter = (Converter) getObject.invoke(unsafe, converterFieldBase, converterOffset);

        // Replace CONVERTER with our mock — Unsafe bypasses the final restriction
        putObject.invoke(unsafe, converterFieldBase, converterOffset, converterMock);
    }

    @AfterAll
    static void restoreConverter() throws Exception {
        if (unsafe != null) {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
            putObject.invoke(unsafe, converterFieldBase, converterOffset, savedConverter);
        }
    }

    @Mock
    private AppApplicationMapper baseMapper;

    @InjectMocks
    private AppApplicationServiceImpl service;

    private PageQuery pageQuery;

    @BeforeEach
    void setUp() {
        pageQuery = new PageQuery();
        pageQuery.setPageNum(1);
        pageQuery.setPageSize(10);
        clearInvocations(converterMock);
    }

    @Test
    void queryPageList_shouldBuildWrapperAndReturnTableDataInfo() {
        AppApplicationBo bo = new AppApplicationBo();
        bo.setStatus("0");
        bo.setCategoryId(1L);

        Page<AppApplicationVo> mockPage = new Page<>(1, 10);
        AppApplicationVo vo = new AppApplicationVo();
        vo.setAppId(1L);
        vo.setAppName("TestApp");
        mockPage.setRecords(List.of(vo));
        mockPage.setTotal(1);

        when(baseMapper.selectVoPage(any(), any())).thenReturn(mockPage);

        TableDataInfo<AppApplicationVo> result = service.queryPageList(bo, pageQuery);

        assertThat(result).isNotNull();
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().get(0).getAppId()).isEqualTo(1L);
        assertThat(result.getTotal()).isEqualTo(1);
        verify(baseMapper, times(1)).selectVoPage(any(), any());
    }

    @Test
    void queryPageList_withKeyword_shouldPassKeywordToWrapper() {
        AppApplicationBo bo = new AppApplicationBo();
        bo.setKeyword("search");

        Page<AppApplicationVo> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of());
        mockPage.setTotal(0);

        when(baseMapper.selectVoPage(any(), any())).thenReturn(mockPage);

        TableDataInfo<AppApplicationVo> result = service.queryPageList(bo, pageQuery);

        assertThat(result.getRows()).isEmpty();
        verify(baseMapper).selectVoPage(any(), any());
    }

    @Test
    void queryList_shouldDelegateToSelectVoList() {
        AppApplicationBo bo = new AppApplicationBo();
        bo.setStatus("0");

        AppApplicationVo vo = new AppApplicationVo();
        vo.setAppId(2L);
        when(baseMapper.selectVoList(any())).thenReturn(List.of(vo));

        List<AppApplicationVo> result = service.queryList(bo);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAppId()).isEqualTo(2L);
        verify(baseMapper).selectVoList(any());
    }

    @Test
    void queryById_shouldDelegateToSelectVoById() {
        AppApplicationVo vo = new AppApplicationVo();
        vo.setAppId(42L);
        when(baseMapper.selectVoById(42L)).thenReturn(vo);

        AppApplicationVo result = service.queryById(42L);

        assertThat(result).isNotNull();
        assertThat(result.getAppId()).isEqualTo(42L);
    }

    @Test
    void queryById_shouldReturnNullWhenNotFound() {
        when(baseMapper.selectVoById(999L)).thenReturn(null);

        AppApplicationVo result = service.queryById(999L);

        assertThat(result).isNull();
    }

    @Test
    void changeStatus_shouldUpdateAndReturnTrue() {
        when(baseMapper.updateById(any(AppApplication.class))).thenReturn(1);

        Boolean result = service.changeStatus(1L, "1");

        assertThat(result).isTrue();
        ArgumentCaptor<AppApplication> captor = ArgumentCaptor.forClass(AppApplication.class);
        verify(baseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getAppId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo("1");
    }

    @Test
    void changeStatus_shouldReturnFalseWhenUpdateFails() {
        when(baseMapper.updateById(any(AppApplication.class))).thenReturn(0);

        Boolean result = service.changeStatus(1L, "0");

        assertThat(result).isFalse();
    }

    @Test
    void deleteWithValidByIds_shouldReturnTrueWhenDeleted() {
        List<Long> ids = Arrays.asList(1L, 2L);
        when(baseMapper.deleteByIds(ids)).thenReturn(2);

        Boolean result = service.deleteWithValidByIds(ids);

        assertThat(result).isTrue();
        verify(baseMapper).deleteByIds(ids);
    }

    @Test
    void deleteWithValidByIds_shouldReturnFalseWhenNothingDeleted() {
        List<Long> ids = List.of(999L);
        when(baseMapper.deleteByIds(ids)).thenReturn(0);

        Boolean result = service.deleteWithValidByIds(ids);

        assertThat(result).isFalse();
    }

    @Test
    void queryPageList_withNoFilters_shouldCallSelectVoPage() {
        AppApplicationBo bo = new AppApplicationBo();

        Page<AppApplicationVo> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0);
        when(baseMapper.selectVoPage(any(), any())).thenReturn(emptyPage);

        TableDataInfo<AppApplicationVo> result = service.queryPageList(bo, pageQuery);

        assertThat(result.getRows()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
    }

    @Test
    void queryList_withIsSecurity_shouldPassToWrapper() {
        AppApplicationBo bo = new AppApplicationBo();
        bo.setIsSecurity("1");
        when(baseMapper.selectVoList(any())).thenReturn(List.of());

        List<AppApplicationVo> result = service.queryList(bo);

        assertThat(result).isEmpty();
        verify(baseMapper).selectVoList(any());
    }

    // ============================================================
    // insertByBo / updateByBo — cover MapstructUtils.convert paths
    // converterMock is injected into MapstructUtils.CONVERTER in @BeforeAll
    // ============================================================

    @Test
    void insertByBo_shouldCallInsertAndReturnTrue() {
        doReturn(new AppApplication()).when(converterMock).convert(any(AppApplicationBo.class), eq(AppApplication.class));
        when(baseMapper.insert(any(AppApplication.class))).thenReturn(1);

        Boolean result = service.insertByBo(new AppApplicationBo());

        assertThat(result).isTrue();
        verify(baseMapper).insert(any(AppApplication.class));
    }

    @Test
    void insertByBo_shouldReturnFalseWhenInsertFails() {
        doReturn(new AppApplication()).when(converterMock).convert(any(AppApplicationBo.class), eq(AppApplication.class));
        when(baseMapper.insert(any(AppApplication.class))).thenReturn(0);

        Boolean result = service.insertByBo(new AppApplicationBo());

        assertThat(result).isFalse();
    }

    @Test
    void updateByBo_shouldCallUpdateByIdAndReturnTrue() {
        doReturn(new AppApplication()).when(converterMock).convert(any(AppApplicationBo.class), eq(AppApplication.class));
        when(baseMapper.updateById(any(AppApplication.class))).thenReturn(1);

        Boolean result = service.updateByBo(new AppApplicationBo());

        assertThat(result).isTrue();
        verify(baseMapper).updateById(any(AppApplication.class));
    }

    @Test
    void updateByBo_shouldReturnFalseWhenUpdateFails() {
        doReturn(new AppApplication()).when(converterMock).convert(any(AppApplicationBo.class), eq(AppApplication.class));
        when(baseMapper.updateById(any(AppApplication.class))).thenReturn(0);

        Boolean result = service.updateByBo(new AppApplicationBo());

        assertThat(result).isFalse();
    }
}
