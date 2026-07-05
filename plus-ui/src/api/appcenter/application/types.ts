export interface AppApplicationVo extends BaseEntity {
  appId: number | string;
  appName: string;
  appCode: string;
  version: string;
  categoryId: number | string;
  categoryName: string;
  icon: string;
  accent: string;
  description: string;
  tags: string;
  accessUrl: string;
  appType: string;
  requiredRoleKey?: string;
  packageOssId?: number | string;
  packageName?: string;
  packageSize?: number;
  packageUrl?: string;
  isSecurity: string;
  status: string;
  useCount: number;
  recommendCount: number;
  orderNum: number;
  remark: string;
}

export interface AppApplicationForm {
  appId: number | string | undefined;
  appName: string;
  appCode: string;
  version: string;
  categoryId: number | string | undefined;
  icon: string;
  accent: string;
  description: string;
  tags: string;
  accessUrl: string;
  appType: string;
  requiredRoleKey?: string;
  packageOssId?: number | string;
  packageName?: string;
  packageSize?: number;
  packageUrl?: string;
  isSecurity: string;
  status: string;
  orderNum: number;
  remark: string;
}

export interface AppApplicationQuery extends PageQuery {
  keyword: string;
  categoryId: number | string | undefined;
  appType: string;
  status: string;
  isSecurity: string;
  requiredRoleKey?: string;
}

export interface AppPackageUploadVo {
  packageOssId: number | string;
  packageName: string;
  packageSize: number;
  packageUrl?: string;
}
