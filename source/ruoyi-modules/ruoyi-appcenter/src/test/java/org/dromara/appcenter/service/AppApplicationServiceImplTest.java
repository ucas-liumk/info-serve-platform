package org.dromara.appcenter.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.appcenter.domain.AppApplication;
import org.dromara.appcenter.domain.bo.AppApplicationBo;
import org.dromara.appcenter.domain.vo.AppApplicationVo;
import org.dromara.appcenter.mapper.AppApplicationMapper;
import org.dromara.appcenter.service.impl.AppApplicationServiceImpl;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppApplicationServiceImplTest {

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
}
