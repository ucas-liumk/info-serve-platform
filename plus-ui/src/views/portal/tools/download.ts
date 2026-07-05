import FileSaver from 'file-saver';
import { ElMessage } from 'element-plus';
import errorCode from '@/utils/errorCode';
import { downloadPackageBlob } from '@/api/portal/appcenter';
import type { PortalApp } from '@/api/appcenter/types';

const decodeHeaderFileName = (value: unknown) => {
  const raw = Array.isArray(value) ? value[0] : String(value || '');
  if (!raw) return '';
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
};

const fallbackPackageName = (app: PortalApp) => app.packageName || `${app.appName || '离线应用安装包'}.zip`;

const showBlobError = async (data: Blob) => {
  const text = await data.text();
  const rspObj = JSON.parse(text);
  const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode.default;
  ElMessage.error(errMsg);
};

export const downloadPortalAppPackage = async (app: PortalApp) => {
  if (!app.appId) {
    ElMessage.warning('应用不存在或已下架');
    return;
  }
  try {
    const res = await downloadPackageBlob(app.appId);
    if (res.data.type?.includes('application/json')) {
      await showBlobError(res.data);
      return;
    }
    const fileName = decodeHeaderFileName(res.headers['download-filename']) || fallbackPackageName(app);
    FileSaver.saveAs(res.data, fileName);
  } catch (error) {
    console.error(error);
    ElMessage.error('下载安装包出现错误，请联系管理员！');
  }
};
