import FileSaver from 'file-saver';
import { ElMessage } from 'element-plus';
import errorCode from '@/utils/errorCode';
import { downloadResourceBlob } from '@/api/infoservice/portal';
import type { InfoResource } from '@/api/infoservice/types';

const decodeHeaderFileName = (value: unknown) => {
  const raw = Array.isArray(value) ? value[0] : String(value || '');
  if (!raw) return '';
  try {
    return decodeURIComponent(raw);
  } catch {
    return raw;
  }
};

const fallbackFileName = (resource: InfoResource) => {
  const name = resource.originalName || resource.title || '资料下载';
  const suffix = resource.fileSuffix?.replace(/^\./, '');
  if (suffix && !/\.[^./\\]+$/.test(name)) {
    return `${name}.${suffix}`;
  }
  return name;
};

const showBlobError = async (data: Blob) => {
  const text = await data.text();
  const rspObj = JSON.parse(text);
  const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode.default;
  ElMessage.error(errMsg);
};

export const downloadPortalResource = async (resource: InfoResource) => {
  if (!resource.resourceId) {
    ElMessage.warning('资料不存在或已下架');
    return;
  }
  try {
    const res = await downloadResourceBlob(resource.resourceId);
    if (res.data.type?.includes('application/json')) {
      await showBlobError(res.data);
      return;
    }
    const fileName = decodeHeaderFileName(res.headers['download-filename']) || fallbackFileName(resource);
    FileSaver.saveAs(res.data, fileName);
  } catch (error) {
    console.error(error);
    ElMessage.error('下载文件出现错误，请联系管理员！');
  }
};
