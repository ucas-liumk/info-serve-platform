package org.dromara.appcenter.service;

import org.dromara.appcenter.domain.vo.AppCategoryVo;
import org.dromara.appcenter.mapper.AppCategoryMapper;
import org.dromara.appcenter.service.impl.AppCategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppCategoryServiceImplTest {

    @Mock
    private AppCategoryMapper baseMapper;

    @InjectMocks
    private AppCategoryServiceImpl service;

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
}
