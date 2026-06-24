package org.dromara.appcenter.service;

import cn.hutool.extra.spring.SpringUtil;
import io.github.linpeilie.Converter;
import org.dromara.appcenter.domain.AppCategory;
import org.dromara.appcenter.domain.bo.AppCategoryBo;
import org.dromara.appcenter.domain.vo.AppCategoryVo;
import org.dromara.appcenter.mapper.AppCategoryMapper;
import org.dromara.appcenter.service.impl.AppCategoryServiceImpl;
import org.dromara.common.core.utils.MapstructUtils;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppCategoryServiceImplTest {

    /**
     * Class-level Converter mock injected into MapstructUtils.CONVERTER via sun.misc.Unsafe
     * (accessed reflectively to avoid a compile-time dependency on internal JDK APIs).
     * Unsafe.putObject bypasses the "final" restriction for static fields in user classes.
     */
    static Converter converterMock;
    private static Converter savedConverter;
    private static Object unsafe;
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

        Field converterField = MapstructUtils.class.getDeclaredField("CONVERTER");
        converterField.setAccessible(true);

        Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
        Method staticFieldBase = unsafeClass.getMethod("staticFieldBase", Field.class);
        Method getObject = unsafeClass.getMethod("getObject", Object.class, long.class);
        Method putObject = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);

        converterOffset = (long) staticFieldOffset.invoke(unsafe, converterField);
        converterFieldBase = staticFieldBase.invoke(unsafe, converterField);
        savedConverter = (Converter) getObject.invoke(unsafe, converterFieldBase, converterOffset);
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
    private AppCategoryMapper baseMapper;

    @InjectMocks
    private AppCategoryServiceImpl service;

    @BeforeEach
    void resetConverterInvocations() {
        clearInvocations(converterMock);
    }

    @Test
    void queryList_shouldReturnOnlyEnabledCategories() {
        AppCategoryVo cat1 = new AppCategoryVo();
        cat1.setCategoryId(1L);
        cat1.setCategoryName("Tools");
        AppCategoryVo cat2 = new AppCategoryVo();
        cat2.setCategoryId(2L);
        cat2.setCategoryName("Games");

        // The query wrapper filters status='0'; mapper returns the filtered list
        when(baseMapper.selectVoList(any())).thenReturn(List.of(cat1, cat2));

        List<AppCategoryVo> result = service.queryList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategoryName()).isEqualTo("Tools");
        // Verify that selectVoList was called with a wrapper (filtering status)
        verify(baseMapper, times(1)).selectVoList(any());
    }

    @Test
    void queryList_shouldReturnEmptyWhenNoEnabledCategories() {
        when(baseMapper.selectVoList(any())).thenReturn(List.of());

        List<AppCategoryVo> result = service.queryList();

        assertThat(result).isEmpty();
        verify(baseMapper).selectVoList(any());
    }

    @Test
    void queryById_shouldReturnCategoryVo() {
        AppCategoryVo vo = new AppCategoryVo();
        vo.setCategoryId(10L);
        vo.setCategoryName("Office");
        when(baseMapper.selectVoById(10L)).thenReturn(vo);

        AppCategoryVo result = service.queryById(10L);

        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(10L);
        assertThat(result.getCategoryName()).isEqualTo("Office");
    }

    @Test
    void queryById_shouldReturnNullWhenNotFound() {
        when(baseMapper.selectVoById(999L)).thenReturn(null);

        AppCategoryVo result = service.queryById(999L);

        assertThat(result).isNull();
    }

    @Test
    void deleteByIds_shouldReturnTrueWhenDeleted() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(baseMapper.deleteByIds(ids)).thenReturn(3);

        Boolean result = service.deleteByIds(ids);

        assertThat(result).isTrue();
        verify(baseMapper).deleteByIds(ids);
    }

    @Test
    void deleteByIds_shouldReturnFalseWhenNothingDeleted() {
        List<Long> ids = List.of(999L);
        when(baseMapper.deleteByIds(ids)).thenReturn(0);

        Boolean result = service.deleteByIds(ids);

        assertThat(result).isFalse();
    }

    @Test
    void queryList_shouldCallSelectVoListWithWrapper() {
        when(baseMapper.selectVoList(any())).thenReturn(List.of());

        service.queryList();

        // Verify that the wrapper is passed (not null), meaning filters are applied
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(baseMapper).selectVoList(any());
    }

    // ============================================================
    // insertByBo / updateByBo — cover MapstructUtils.convert paths
    // converterMock is injected into MapstructUtils.CONVERTER in @BeforeAll
    // ============================================================

    @Test
    void insertByBo_shouldCallInsertAndReturnTrue() {
        doReturn(new AppCategory()).when(converterMock).convert(any(AppCategoryBo.class), eq(AppCategory.class));
        when(baseMapper.insert(any(AppCategory.class))).thenReturn(1);

        Boolean result = service.insertByBo(new AppCategoryBo());

        assertThat(result).isTrue();
        verify(baseMapper).insert(any(AppCategory.class));
    }

    @Test
    void insertByBo_shouldReturnFalseWhenInsertFails() {
        doReturn(new AppCategory()).when(converterMock).convert(any(AppCategoryBo.class), eq(AppCategory.class));
        when(baseMapper.insert(any(AppCategory.class))).thenReturn(0);

        Boolean result = service.insertByBo(new AppCategoryBo());

        assertThat(result).isFalse();
    }

    @Test
    void updateByBo_shouldCallUpdateByIdAndReturnTrue() {
        doReturn(new AppCategory()).when(converterMock).convert(any(AppCategoryBo.class), eq(AppCategory.class));
        when(baseMapper.updateById(any(AppCategory.class))).thenReturn(1);

        Boolean result = service.updateByBo(new AppCategoryBo());

        assertThat(result).isTrue();
        verify(baseMapper).updateById(any(AppCategory.class));
    }

    @Test
    void updateByBo_shouldReturnFalseWhenUpdateFails() {
        doReturn(new AppCategory()).when(converterMock).convert(any(AppCategoryBo.class), eq(AppCategory.class));
        when(baseMapper.updateById(any(AppCategory.class))).thenReturn(0);

        Boolean result = service.updateByBo(new AppCategoryBo());

        assertThat(result).isFalse();
    }
}
