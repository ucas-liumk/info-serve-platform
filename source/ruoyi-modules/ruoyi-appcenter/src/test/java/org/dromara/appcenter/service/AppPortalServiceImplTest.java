package org.dromara.appcenter.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dromara.appcenter.domain.*;
import org.dromara.appcenter.domain.vo.AppCategoryVo;
import org.dromara.appcenter.domain.vo.AppMessageVo;
import org.dromara.appcenter.domain.vo.PortalAppVo;
import org.dromara.appcenter.mapper.*;
import org.dromara.appcenter.service.impl.AppPortalServiceImpl;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppPortalServiceImplTest {

    @Mock
    private AppApplicationMapper applicationMapper;
    @Mock
    private AppCategoryMapper categoryMapper;
    @Mock
    private AppFavoriteMapper favoriteMapper;
    @Mock
    private AppRecommendMapper recommendMapper;
    @Mock
    private AppMessageMapper messageMapper;

    @InjectMocks
    private AppPortalServiceImpl service;

    private MockedStatic<LoginHelper> loginHelperMock;
    private static final Long USER_ID = 100L;

    private PageQuery pageQuery;

    @BeforeEach
    void setUp() {
        loginHelperMock = mockStatic(LoginHelper.class);
        loginHelperMock.when(LoginHelper::getUserId).thenReturn(USER_ID);

        pageQuery = new PageQuery();
        pageQuery.setPageNum(1);
        pageQuery.setPageSize(10);
    }

    @AfterEach
    void tearDown() {
        loginHelperMock.close();
    }

    // ============================================================
    // categories()
    // ============================================================

    @Test
    void categories_shouldFilterByStatus0() {
        AppCategoryVo cat = new AppCategoryVo();
        cat.setCategoryId(1L);
        cat.setCategoryName("Tools");
        when(categoryMapper.selectVoList(any())).thenReturn(List.of(cat));

        List<AppCategoryVo> result = service.categories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("Tools");
        verify(categoryMapper).selectVoList(any());
    }

    @Test
    void categories_shouldReturnEmptyWhenNoEnabledCategories() {
        when(categoryMapper.selectVoList(any())).thenReturn(Collections.emptyList());

        List<AppCategoryVo> result = service.categories();

        assertThat(result).isEmpty();
    }

    // ============================================================
    // apps()
    // ============================================================

    @Test
    void apps_latestSort_shouldOrderByCreateTimeDesc() {
        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        TableDataInfo<PortalAppVo> result = service.apps(null, null, "latest", pageQuery);

        assertThat(result).isNotNull();
        assertThat(result.getRows()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void apps_hotSort_shouldCallSelectPage() {
        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        TableDataInfo<PortalAppVo> result = service.apps(null, null, "hot", pageQuery);

        assertThat(result).isNotNull();
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void apps_useSort_shouldCallSelectPage() {
        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        TableDataInfo<PortalAppVo> result = service.apps(null, null, "use", pageQuery);

        assertThat(result).isNotNull();
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void apps_unknownCategoryCode_shouldFilterByMinusOne() {
        // categoryCode not "all" and not blank → look up category → returns null → filter -1
        when(categoryMapper.selectOne(any())).thenReturn(null);
        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        TableDataInfo<PortalAppVo> result = service.apps("unknown-code", null, "latest", pageQuery);

        assertThat(result.getRows()).isEmpty();
        verify(categoryMapper).selectOne(any());
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void apps_knownCategoryCode_shouldFilterByCategoryId() {
        AppCategory category = new AppCategory();
        category.setCategoryId(5L);
        when(categoryMapper.selectOne(any())).thenReturn(category);

        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        TableDataInfo<PortalAppVo> result = service.apps("tools", null, "latest", pageQuery);

        assertThat(result).isNotNull();
        verify(categoryMapper).selectOne(any());
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void apps_withKeyword_shouldApplyKeywordFilter() {
        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        TableDataInfo<PortalAppVo> result = service.apps(null, "office", "latest", pageQuery);

        assertThat(result).isNotNull();
        verify(applicationMapper).selectPage(any(), any());
    }

    @Test
    void apps_withResults_shouldAssembleFavoritedAndRecommendedFlags() {
        AppApplication app = new AppApplication();
        app.setAppId(1L);
        app.setAppName("App1");
        app.setCategoryId(10L);

        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(List.of(app));
        page.setTotal(1);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        // User has favorited app 1
        AppFavorite fav = new AppFavorite();
        fav.setAppId(1L);
        when(favoriteMapper.selectList(any())).thenReturn(List.of(fav));
        // User has NOT recommended app 1
        when(recommendMapper.selectList(any())).thenReturn(Collections.emptyList());
        // Category names
        AppCategory cat = new AppCategory();
        cat.setCategoryId(10L);
        cat.setCategoryName("Office");
        when(categoryMapper.selectList(null)).thenReturn(List.of(cat));

        TableDataInfo<PortalAppVo> result = service.apps(null, null, "latest", pageQuery);

        assertThat(result.getRows()).hasSize(1);
        PortalAppVo vo = result.getRows().get(0);
        assertThat(vo.getFavorited()).isTrue();
        assertThat(vo.getRecommended()).isFalse();
        assertThat(vo.getCategoryName()).isEqualTo("Office");
    }

    @Test
    void apps_allCategoryCode_shouldNotFilterByCategoryId() {
        Page<AppApplication> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        when(applicationMapper.selectPage(any(), any())).thenReturn(page);

        service.apps("all", null, "latest", pageQuery);

        // categoryMapper.selectOne should NOT be called since code is "all"
        verify(categoryMapper, never()).selectOne(any());
    }

    // ============================================================
    // use()
    // ============================================================

    @Test
    void use_appNotFound_shouldThrowServiceException() {
        when(applicationMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.use(99L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不存在或已下架");
    }

    @Test
    void use_appOffline_shouldThrowServiceException() {
        AppApplication app = new AppApplication();
        app.setAppId(1L);
        app.setStatus("1"); // offline
        when(applicationMapper.selectById(1L)).thenReturn(app);

        assertThatThrownBy(() -> service.use(1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不存在或已下架");
    }

    @Test
    @SuppressWarnings("unchecked")
    void use_appOnline_shouldReturnAccessUrlAndIncrementUseCount() {
        AppApplication app = new AppApplication();
        app.setAppId(1L);
        app.setStatus("0");
        app.setAccessUrl("https://example.com/app");
        when(applicationMapper.selectById(1L)).thenReturn(app);
        when(applicationMapper.update(isNull(), any())).thenReturn(1);

        String result = service.use(1L);

        assertThat(result).isEqualTo("https://example.com/app");

        ArgumentCaptor<LambdaUpdateWrapper<AppApplication>> cap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(applicationMapper).update(isNull(), cap.capture());
        assertThat(cap.getValue().getSqlSet()).contains("use_count = use_count + 1");
    }

    // ============================================================
    // favorite()
    // ============================================================

    @Test
    void favorite_add_whenNotExists_shouldInsert() {
        when(favoriteMapper.selectCount(any())).thenReturn(0L);
        when(favoriteMapper.insert(any(AppFavorite.class))).thenReturn(1);

        service.favorite(1L, true);

        verify(favoriteMapper).insert(any(AppFavorite.class));
    }

    @Test
    void favorite_add_whenAlreadyExists_shouldNotInsert() {
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        service.favorite(1L, true);

        verify(favoriteMapper, never()).insert(any(AppFavorite.class));
    }

    @Test
    void favorite_remove_whenExists_shouldDelete() {
        when(favoriteMapper.selectCount(any())).thenReturn(1L);

        service.favorite(1L, false);

        verify(favoriteMapper).delete(any());
        verify(favoriteMapper, never()).insert(any(AppFavorite.class));
    }

    @Test
    void favorite_remove_whenNotExists_shouldNotDelete() {
        when(favoriteMapper.selectCount(any())).thenReturn(0L);

        service.favorite(1L, false);

        verify(favoriteMapper, never()).delete(any());
    }

    // ============================================================
    // recommend()
    // ============================================================

    @Test
    @SuppressWarnings("unchecked")
    void recommend_add_whenNotExists_shouldInsertAndIncrementCount() {
        when(recommendMapper.selectCount(any())).thenReturn(0L);
        when(recommendMapper.insert(any(AppRecommend.class))).thenReturn(1);
        when(applicationMapper.update(isNull(), any())).thenReturn(1);

        service.recommend(1L, true);

        verify(recommendMapper).insert(any(AppRecommend.class));

        ArgumentCaptor<LambdaUpdateWrapper<AppApplication>> cap =
            ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(applicationMapper).update(isNull(), cap.capture());
        assertThat(cap.getValue().getSqlSet()).contains("recommend_count = recommend_count + 1");
    }

    @Test
    void recommend_add_whenAlreadyExists_shouldNotInsert() {
        when(recommendMapper.selectCount(any())).thenReturn(1L);

        service.recommend(1L, true);

        verify(recommendMapper, never()).insert(any(AppRecommend.class));
        verify(applicationMapper, never()).update(any(), any());
    }

    @Test
    void recommend_remove_whenExists_shouldDeleteAndDecrementCount() {
        when(recommendMapper.selectCount(any())).thenReturn(1L);
        when(applicationMapper.update(isNull(), any())).thenReturn(1);

        service.recommend(1L, false);

        verify(recommendMapper).delete(any());
        verify(applicationMapper).update(isNull(), any());
    }

    @Test
    void recommend_remove_whenNotExists_shouldNotDelete() {
        when(recommendMapper.selectCount(any())).thenReturn(0L);

        service.recommend(1L, false);

        verify(recommendMapper, never()).delete(any());
        verify(applicationMapper, never()).update(any(), any());
    }

    // ============================================================
    // favorites()
    // ============================================================

    @Test
    void favorites_emptyFavorites_shouldReturnEmptyTableDataInfo() {
        when(favoriteMapper.selectList(any())).thenReturn(Collections.emptyList());

        TableDataInfo<PortalAppVo> result = service.favorites(pageQuery);

        assertThat(result.getRows()).isEmpty();
        assertThat(result.getTotal()).isEqualTo(0);
        verify(applicationMapper, never()).selectPage(any(), any());
    }

    @Test
    void favorites_nonEmpty_shouldFetchAppsAndReturnPageData() {
        AppFavorite fav = new AppFavorite();
        fav.setAppId(1L);
        when(favoriteMapper.selectList(any())).thenReturn(List.of(fav));

        AppApplication app = new AppApplication();
        app.setAppId(1L);
        app.setAppName("FavoriteApp");
        app.setCategoryId(2L);
        Page<AppApplication> appPage = new Page<>(1, 10);
        appPage.setRecords(List.of(app));
        appPage.setTotal(1);
        when(applicationMapper.selectPage(any(), any())).thenReturn(appPage);

        // For toPortalVos
        when(favoriteMapper.selectList(any())).thenReturn(List.of(fav));
        when(recommendMapper.selectList(any())).thenReturn(Collections.emptyList());
        AppCategory cat = new AppCategory();
        cat.setCategoryId(2L);
        cat.setCategoryName("Work");
        when(categoryMapper.selectList(null)).thenReturn(List.of(cat));

        TableDataInfo<PortalAppVo> result = service.favorites(pageQuery);

        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        verify(applicationMapper).selectPage(any(), any());
    }

    // ============================================================
    // messages()
    // ============================================================

    @Test
    void messages_withIsReadFilter_shouldReturnPage() {
        Page<AppMessageVo> msgPage = new Page<>(1, 10);
        AppMessageVo msgVo = new AppMessageVo();
        msgVo.setMessageId(1L);
        msgVo.setTitle("Hello");
        msgPage.setRecords(List.of(msgVo));
        msgPage.setTotal(1);
        when(messageMapper.selectVoPage(any(), any())).thenReturn(msgPage);

        TableDataInfo<AppMessageVo> result = service.messages("0", pageQuery);

        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(1);
        verify(messageMapper).selectVoPage(any(), any());
    }

    @Test
    void messages_withNoFilter_shouldReturnAllMessages() {
        Page<AppMessageVo> msgPage = new Page<>(1, 10);
        msgPage.setRecords(Collections.emptyList());
        msgPage.setTotal(0);
        when(messageMapper.selectVoPage(any(), any())).thenReturn(msgPage);

        TableDataInfo<AppMessageVo> result = service.messages(null, pageQuery);

        assertThat(result.getRows()).isEmpty();
        verify(messageMapper).selectVoPage(any(), any());
    }

    // ============================================================
    // unreadCount()
    // ============================================================

    @Test
    void unreadCount_shouldReturnCountForCurrentUser() {
        when(messageMapper.selectCount(any())).thenReturn(5L);

        long result = service.unreadCount();

        assertThat(result).isEqualTo(5L);
        verify(messageMapper).selectCount(any());
    }

    @Test
    void unreadCount_shouldReturnZeroWhenNoUnread() {
        when(messageMapper.selectCount(any())).thenReturn(0L);

        long result = service.unreadCount();

        assertThat(result).isEqualTo(0L);
    }

    // ============================================================
    // readMessage()
    // ============================================================

    @Test
    void readMessage_shouldUpdateMessageScopedToCurrentUser() {
        when(messageMapper.update(any(AppMessage.class), any())).thenReturn(1);

        service.readMessage(10L);

        verify(messageMapper).update(any(AppMessage.class), any());
        // Confirm LoginHelper was called for userId scoping
        loginHelperMock.verify(LoginHelper::getUserId);
    }

    @Test
    void readMessage_shouldSetIsReadTo1() {
        // Capture the AppMessage entity passed to update
        org.mockito.ArgumentCaptor<AppMessage> captor =
            org.mockito.ArgumentCaptor.forClass(AppMessage.class);
        when(messageMapper.update(captor.capture(), any())).thenReturn(1);

        service.readMessage(7L);

        AppMessage captured = captor.getValue();
        assertThat(captured.getMessageId()).isEqualTo(7L);
        assertThat(captured.getIsRead()).isEqualTo("1");
    }
}
