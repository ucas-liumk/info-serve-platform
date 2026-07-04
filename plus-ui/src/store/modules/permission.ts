import { defineStore } from 'pinia';
import router, { constantRoutes, dynamicRoutes } from '@/router';
import store from '@/store';
import { getRouters } from '@/api/menu';
import auth from '@/plugins/auth';
import { RouteRecordRaw } from 'vue-router';
import Layout from '@/layout/index.vue';
import ParentView from '@/components/ParentView/index.vue';
import InnerLink from '@/layout/components/InnerLink/index.vue';
import { ref } from 'vue';
import { createCustomNameComponent } from '@/utils/createCustomNameComponent';
import { ADMIN_BASE_PATH } from '@/constants/router';

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue');

const viewAliasMap: Record<string, string> = {
  'appcenter/application/index': 'admin/appcenter/application/index',
  'appcenter/category/index': 'admin/appcenter/category/index',
  'infoservice/resource/index': 'admin/resources/resource/index',
  'infoservice/resource/category': 'admin/resources/category/index',
  'infoservice/forum/topic': 'admin/forum/topic/index',
  'infoservice/forum/board': 'admin/forum/board/index'
};

const adminViewRoots = ['system', 'monitor'];

const resolveViewCandidates = (view: any): string[] => {
  const viewPath = String(view || '');
  const candidates = [viewPath];
  const aliasPath = viewAliasMap[viewPath];

  if (aliasPath) {
    candidates.push(aliasPath);
  }
  if (adminViewRoots.some((root) => viewPath === root || viewPath.startsWith(`${root}/`))) {
    candidates.push(`admin/${viewPath}`);
  }

  return [...new Set(candidates)];
};

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([]);
  const addRoutes = ref<RouteRecordRaw[]>([]);
  const defaultRoutes = ref<RouteRecordRaw[]>([]);
  const topbarRouters = ref<RouteRecordRaw[]>([]);
  const sidebarRouters = ref<RouteRecordRaw[]>([]);

  const getRoutes = (): RouteRecordRaw[] => {
    return routes.value as RouteRecordRaw[];
  };
  const getDefaultRoutes = (): RouteRecordRaw[] => {
    return defaultRoutes.value as RouteRecordRaw[];
  };
  const getSidebarRoutes = (): RouteRecordRaw[] => {
    return sidebarRouters.value as RouteRecordRaw[];
  };
  const getTopbarRoutes = (): RouteRecordRaw[] => {
    return topbarRouters.value as RouteRecordRaw[];
  };

  const setRoutes = (newRoutes: RouteRecordRaw[]): void => {
    addRoutes.value = newRoutes;
    routes.value = constantRoutes.concat(newRoutes);
  };
  const setDefaultRoutes = (routes: RouteRecordRaw[]): void => {
    defaultRoutes.value = constantRoutes.concat(routes);
  };
  const setTopbarRoutes = (routes: RouteRecordRaw[]): void => {
    topbarRouters.value = routes;
  };
  const setSidebarRouters = (routes: RouteRecordRaw[]): void => {
    sidebarRouters.value = routes;
  };
  const generateRoutes = async (): Promise<RouteRecordRaw[]> => {
    const res = await getRouters();
    const { data } = res;
    const sdata = JSON.parse(JSON.stringify(data));
    const rdata = JSON.parse(JSON.stringify(data));
    const defaultData = JSON.parse(JSON.stringify(data));
    const sidebarRoutes = withAdminBaseRoutes(filterAsyncRouter(sdata));
    const rewriteRoutes = withAdminBaseRoutes(filterAsyncRouter(rdata, undefined, true));
    const defaultRoutes = withAdminBaseRoutes(filterAsyncRouter(defaultData));
    const asyncRoutes = filterDynamicRoutes(dynamicRoutes);
    asyncRoutes.forEach((route) => {
      router.addRoute(route);
    });
    setRoutes(rewriteRoutes);
    setSidebarRouters(constantRoutes.concat(sidebarRoutes));
    setDefaultRoutes(sidebarRoutes);
    setTopbarRoutes(defaultRoutes);
    // 路由name重复检查
    duplicateRouteChecker(asyncRoutes, sidebarRoutes);
    return new Promise<RouteRecordRaw[]>((resolve) => resolve(rewriteRoutes));
  };

  /**
   * 遍历后台传来的路由字符串，转换为组件对象
   * @param asyncRouterMap 后台传来的路由字符串
   * @param lastRouter 上一级路由
   * @param type 是否是重写路由
   */
  const filterAsyncRouter = (asyncRouterMap: RouteRecordRaw[], lastRouter?: RouteRecordRaw, type = false): RouteRecordRaw[] => {
    return asyncRouterMap.filter((route) => {
      if (type && route.children) {
        route.children = filterChildren(route.children, undefined);
      }
      // Layout ParentView 组件特殊处理
      if (route.component?.toString() === 'Layout') {
        route.component = Layout;
      } else if (route.component?.toString() === 'ParentView') {
        route.component = ParentView;
      } else if (route.component?.toString() === 'InnerLink') {
        route.component = InnerLink;
      } else {
        route.component = loadView(route.component, route.name as string);
      }
      if (route.children != null && route.children && route.children.length) {
        route.children = filterAsyncRouter(route.children, route, type);
      } else {
        delete route.children;
        delete route.redirect;
      }
      return true;
    });
  };
  const filterChildren = (childrenMap: RouteRecordRaw[], lastRouter?: RouteRecordRaw): RouteRecordRaw[] => {
    let children: RouteRecordRaw[] = [];
    childrenMap.forEach((el) => {
      el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path;
      if (el.children && el.children.length && el.component?.toString() === 'ParentView') {
        children = children.concat(filterChildren(el.children, el));
      } else {
        children.push(el);
      }
    });
    return children;
  };

  const withAdminBaseRoutes = (routeList: RouteRecordRaw[]): RouteRecordRaw[] => {
    return routeList.map((route) => withAdminBaseRoute(route, true));
  };

  const withAdminBaseRoute = (route: RouteRecordRaw, isTopLevel = false): RouteRecordRaw => {
    const nextRoute = { ...route };
    if (isTopLevel) {
      nextRoute.path = withAdminBasePath(nextRoute.path);
    }
    if (typeof nextRoute.redirect === 'string') {
      nextRoute.redirect = withAdminBaseRedirect(nextRoute.redirect);
    }
    if (nextRoute.meta?.activeMenu) {
      nextRoute.meta = {
        ...nextRoute.meta,
        activeMenu: withAdminBasePath(nextRoute.meta.activeMenu as string)
      };
    }
    if (nextRoute.children?.length) {
      nextRoute.children = nextRoute.children.map((child) => withAdminBaseRoute(child));
    }
    return nextRoute;
  };

  const withAdminBasePath = (path: string): string => {
    if (!path || path.startsWith(ADMIN_BASE_PATH) || path.startsWith('http')) {
      return path;
    }
    if (path === '/') {
      return ADMIN_BASE_PATH;
    }
    return `${ADMIN_BASE_PATH}/${path.replace(/^\/+/, '')}`;
  };

  const withAdminBaseRedirect = (redirect: string): string => {
    if (!redirect || redirect === 'noRedirect' || redirect === 'noredirect' || redirect.startsWith('http')) {
      return redirect;
    }
    return withAdminBasePath(redirect);
  };
  return {
    routes,
    topbarRouters,
    sidebarRouters,
    defaultRoutes,

    getRoutes,
    getDefaultRoutes,
    getSidebarRoutes,
    getTopbarRoutes,

    setRoutes,
    generateRoutes,
    setSidebarRouters
  };
});

// 动态路由遍历，验证是否具备权限
export const filterDynamicRoutes = (routes: RouteRecordRaw[]) => {
  const res: RouteRecordRaw[] = [];
  routes.forEach((route) => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route);
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route);
      }
    }
  });
  return res;
};

export const loadView = (view: any, name: string) => {
  const candidates = resolveViewCandidates(view);

  for (const path in modules) {
    const viewsIndex = path.indexOf('/views/');
    let dir = path.substring(viewsIndex + 7);
    dir = dir.substring(0, dir.lastIndexOf('.vue'));
    if (candidates.includes(dir)) {
      return createCustomNameComponent(modules[path], { name });
    }
  }
};

// 非setup
export const usePermissionStoreHook = () => {
  return usePermissionStore(store);
};

interface Route {
  name?: string | symbol;
  path: string;
  children?: Route[];
}

/**
 * 检查路由name是否重复
 * @param localRoutes 本地路由
 * @param routes 动态路由
 */
function duplicateRouteChecker(localRoutes: Route[], routes: Route[]) {
  // 展平
  function flatRoutes(routes: Route[]) {
    const res: Route[] = [];
    routes.forEach((route) => {
      if (route.children) {
        res.push(...flatRoutes(route.children));
      } else {
        res.push(route);
      }
    });
    return res;
  }

  const allRoutes = flatRoutes([...localRoutes, ...routes]);

  const nameList: string[] = [];
  allRoutes.forEach((route) => {
    const name = route.name.toString();
    if (name && nameList.includes(name)) {
      const message = `路由名称: [${name}] 重复, 会造成 404`;
      console.error(message);
      ElNotification({
        title: '路由名称重复',
        message,
        type: 'error'
      });
      return;
    }
    nameList.push(route.name.toString());
  });
}
