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
  /** 上级栏目 id；空=本行是栏目（一级），非空=分类（二级） */
  parentId?: number | null;
  orderNum?: number;
  status?: string;
  remark?: string;
  createTime?: string;
  resourceCount: number;
  children?: ResourceCategory[];
}

/** 门户分类树节点（GET /portal/resources/category-tree）：一级=栏目（含 children），二级=分类（含 resourceCount） */
export interface CategoryTreeNode {
  categoryId: number;
  categoryCode: string;
  categoryName: string;
  orderNum?: number;
  /** 分面计数：仅二级分类返回（响应关键词+工具条筛选，不含分类维度自身） */
  resourceCount?: number;
  children?: CategoryTreeNode[];
}

/** 门户分类树查询参数：与列表接口共用筛选语义，但不含 categoryCode（分面计数不受分类勾选影响） */
export interface ResourceCategoryTreeQuery {
  keyword?: string;
  previewType?: string;
  fileType?: string;
  uploadedWithin?: string;
  sizeRange?: string;
}

export interface InfoResource {
  resourceId: number | string;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  /** 多分类全量（categoryId/categoryName 为主分类） */
  categoryIds?: Array<number | string>;
  ossId: number | string;
  originalName: string;
  fileSuffix: string;
  mimeType: string;
  fileSize: number;
  previewType: string;
  downloadCount: number;
  viewCount: number;
  favoriteCount?: number;
  favorited?: boolean;
  status: string;
  remark?: string;
  createBy?: number | string;
  ownerName?: string;
  createByName?: string;
  canManage?: boolean;
  createTime: string;
  updateTime?: string;
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
  status?: string;
  remark?: string;
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
  categoryIds?: Array<number | string>;
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
  scope?: 'public' | 'mine' | 'favorites';
  keyword?: string;
  categoryId?: number | string;
  /** 分类编码：支持逗号分隔多值（如 "policy,tech"）；'all' 或缺省=不按分类过滤 */
  categoryCode?: string;
  previewType?: string;
  fileType?: string;
  uploadedWithin?: string;
  sizeRange?: string;
  sort?: string;
  status?: string;
}

/** 上传进度条目（uploading=传输中百分比；processing=服务端落库/OSS 写入；done=完成） */
export interface ResourceUploadProgress {
  name: string;
  percent: number;
  status: 'pending' | 'uploading' | 'processing' | 'done';
}

export interface ResourcePortalPayload {
  title: string;
  description?: string;
  categoryId: number | string | undefined;
  categoryIds?: Array<number | string>;
  ossId?: number | string | undefined;
  originalName?: string;
  fileSuffix?: string;
  mimeType?: string;
  fileSize?: number;
  previewType?: string;
  status?: string;
  remark?: string;
}

export interface ResourceNote {
  noteId: number | string;
  resourceId: number | string;
  userId: number | string;
  authorName: string;
  content: string;
  visibility: 'private' | 'public';
  mine?: boolean;
  createTime: string;
  updateTime?: string;
}

export interface ResourceViewRecord {
  recordId: number | string;
  resourceId: number | string;
  userId: number | string;
  userName: string;
  actionType: string;
  createTime: string;
}

export interface ResourceNotePayload {
  content: string;
  visibility: 'private' | 'public';
}

export interface ResourceCategoryForm {
  categoryId: number | string | undefined;
  categoryName: string;
  categoryCode: string;
  description: string;
  icon: string;
  /** 上级栏目 id；空=建栏目，非空=建分类（两级封顶） */
  parentId?: number | string | null;
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
