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
  resourceCount: number;
}

export interface InfoResource {
  resourceId: number;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  ossId: number;
  originalName: string;
  fileSuffix: string;
  mimeType: string;
  fileSize: number;
  previewType: string;
  downloadCount: number;
  viewCount: number;
  status: string;
  createTime: string;
}

export interface ResourceUploadResult {
  ossId: number;
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
