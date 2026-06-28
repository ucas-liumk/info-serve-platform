import { to as tos } from 'await-to-js';
import router from './router';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import { getToken } from '@/utils/auth';
import { isHttp, isPathMatch } from '@/utils/validate';
import { isRelogin } from '@/utils/request';
import { useUserStore } from '@/store/modules/user';
import { useSettingsStore } from '@/store/modules/settings';
import { usePermissionStore } from '@/store/modules/permission';
import { ElMessage } from 'element-plus/es';
import { ADMIN_BASE_PATH, PORTAL_HOME_PATH } from '@/constants/router';

NProgress.configure({ showSpinner: false });

const whiteList = ['/login', '/social-callback'];
const adminLegacyRoots = ['/system', '/monitor', '/tool', '/workflow', '/infoservice'];
const adminLegacyPrefixes = ['/appcenter'];

const isWhiteList = (path: string) => {
  return whiteList.some((pattern) => isPathMatch(pattern, path));
};

const isSameOrChildPath = (path: string, rootPath: string) => {
  return path === rootPath || path.startsWith(`${rootPath}/`);
};

const getAdminCompatPath = (path: string) => {
  if (isSameOrChildPath(path, ADMIN_BASE_PATH)) {
    return path;
  }
  if (adminLegacyRoots.some((rootPath) => isSameOrChildPath(path, rootPath))) {
    return `${ADMIN_BASE_PATH}${path}`;
  }
  if (adminLegacyPrefixes.some((rootPath) => path.startsWith(`${rootPath}/`))) {
    return `${ADMIN_BASE_PATH}${path}`;
  }
  return path;
};

router.beforeEach(async (to, from, next) => {
  NProgress.start();

  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title as string);
    const userStore = useUserStore();

    if (to.path === '/login') {
      next({ path: PORTAL_HOME_PATH });
      NProgress.done();
      return;
    }

    if (isWhiteList(to.path)) {
      next();
      return;
    }

    const targetPath = getAdminCompatPath(to.path);

    if (userStore.roles.length === 0) {
      isRelogin.show = true;
      const [err] = await tos(userStore.getInfo());

      if (err) {
        await userStore.logout();
        ElMessage.error(err);
        next({ path: PORTAL_HOME_PATH });
        return;
      }

      isRelogin.show = false;
      const accessRoutes = await usePermissionStore().generateRoutes();
      accessRoutes.forEach((route) => {
        if (!isHttp(route.path)) {
          router.addRoute(route);
        }
      });
      next({ path: targetPath, replace: true, params: to.params, query: to.query, hash: to.hash });
      return;
    }

    if (targetPath !== to.path) {
      next({ path: targetPath, replace: true, query: to.query, hash: to.hash });
      return;
    }

    next();
    return;
  }

  if (isWhiteList(to.path)) {
    next();
    return;
  }

  const redirect = encodeURIComponent(to.fullPath || PORTAL_HOME_PATH);
  next(`/login?redirect=${redirect}`);
  NProgress.done();
});

router.afterEach(() => {
  NProgress.done();
});
