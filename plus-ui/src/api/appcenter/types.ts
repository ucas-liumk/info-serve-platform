export interface PortalApp {
  appId: number;
  appName: string;
  version: string;
  categoryId: number;
  categoryName: string;
  icon: string;
  accent: string;
  description: string;
  tags: string;
  accessUrl: string;
  appType: 'business' | 'online' | 'offline';
  packageName?: string;
  packageSize?: number;
  isSecurity: string;
  useCount: number;
  favoriteCount: number;
  recommendCount: number;
  favorited: boolean;
  recommended: boolean;
}

export interface PortalCategory {
  categoryId: number;
  categoryName: string;
  categoryCode: string;
  icon: string;
  appCount: number;
}

export interface PortalDemandForm {
  demandType: 'new_app' | 'suggestion';
  appId?: number;
  appName: string;
  content: string;
  contact?: string;
}

export interface PortalDemandItem {
  demandId: number;
  demandType: 'new_app' | 'suggestion';
  appName: string;
  content: string;
  contact?: string;
  status: '0' | '1' | '2' | '3';
  handleRemark?: string;
  handledTime?: string;
  createTime?: string;
}
