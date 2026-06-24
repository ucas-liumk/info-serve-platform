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
  isSecurity: string;
  useCount: number;
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

export interface PortalMessage {
  messageId: number;
  title: string;
  content: string;
  msgType: string;
  isRead: string;
  createTime: string;
}
