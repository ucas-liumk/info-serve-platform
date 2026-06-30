export interface PortalStats {
  resourceCount: number;
  toolCount: number;
  topicCount: number;
  activeUserCount: number;
  todayVisitCount: number;
}

export interface ResourceCategory {
  categoryId: number;
  categoryName: string;
  categoryCode: string;
  description: string;
  icon: string;
  orderNum?: number;
  status?: string;
  remark?: string;
  createTime?: string;
  resourceCount: number;
}

export interface InfoResource {
  resourceId: number | string;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  ossId: number | string;
  originalName: string;
  fileSuffix: string;
  mimeType: string;
  fileSize: number;
  previewType: string;
  downloadCount: number;
  viewCount: number;
  status: string;
  remark?: string;
  createBy?: number | string;
  ownerName?: string;
  createByName?: string;
  canManage?: boolean;
  createTime: string;
}

export interface ResourceUploadResult {
  ossId: number | string;
  url: string;
  fileName: string;
  originalName: string;
  fileSuffix: string;
  mimeType: string;
  fileSize: number;
  previewType: string;
}

export interface ForumBoard {
  boardId: number;
  boardName: string;
  boardCode: string;
  description: string;
  orderNum?: number;
  status?: string;
  remark?: string;
  createTime?: string;
  topicCount: number;
}

export interface ForumTopic {
  topicId: number;
  boardId: number;
  boardName: string;
  title: string;
  content: string;
  authorId: number;
  authorName: string;
  viewCount: number;
  replyCount: number;
  likeCount: number;
  isTop: string;
  isClosed: string;
  status: string;
  remark?: string;
  createTime: string;
  liked: boolean;
}

export interface ForumReply {
  replyId: number;
  topicId: number;
  content: string;
  authorId: number;
  authorName: string;
  createTime: string;
}

export interface ForumTopicDetail {
  topic: ForumTopic;
  replies: ForumReply[];
}

export interface InfoResourceForm {
  resourceId: number | string | undefined;
  title: string;
  description: string;
  categoryId: number | string | undefined;
  ossId: number | string | undefined;
  originalName: string;
  fileSuffix: string;
  mimeType: string;
  fileSize: number | undefined;
  previewType: string;
  status: string;
  remark: string;
}

export interface InfoResourceQuery extends PageQuery {
  keyword?: string;
  categoryId?: number | string;
  categoryCode?: string;
  status?: string;
}

export interface ResourcePortalQuery extends PageQuery {
  scope?: 'public' | 'mine';
  keyword?: string;
  categoryId?: number | string;
  categoryCode?: string;
  previewType?: string;
  fileType?: string;
  uploadedWithin?: string;
  sizeRange?: string;
  sort?: string;
  status?: string;
}

export interface ResourcePortalPayload {
  title: string;
  description?: string;
  categoryId: number | string | undefined;
  ossId?: number | string | undefined;
  originalName?: string;
  fileSuffix?: string;
  mimeType?: string;
  fileSize?: number;
  previewType?: string;
  status?: string;
  remark?: string;
}

export interface ResourceCategoryForm {
  categoryId: number | string | undefined;
  categoryName: string;
  categoryCode: string;
  description: string;
  icon: string;
  orderNum: number;
  status: string;
  remark: string;
}

export interface ResourceCategoryQuery extends PageQuery {
  keyword?: string;
  categoryName?: string;
  categoryCode?: string;
  status?: string;
}

export interface ForumBoardForm {
  boardId: number | string | undefined;
  boardName: string;
  boardCode: string;
  description: string;
  orderNum: number;
  status: string;
  remark: string;
}

export interface ForumBoardQuery extends PageQuery {
  keyword?: string;
  boardName?: string;
  boardCode?: string;
  status?: string;
}

export interface ForumTopicForm {
  topicId: number | string | undefined;
  boardId: number | string | undefined;
  title: string;
  content: string;
  isTop: string;
  isClosed: string;
  status: string;
  remark: string;
}

export interface ForumTopicQuery extends PageQuery {
  keyword?: string;
  boardId?: number | string;
  boardCode?: string;
  status?: string;
}
