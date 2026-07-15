export interface LatestRequestGuard {
  begin: () => number;
  isCurrent: (requestId: number) => boolean;
}

/** 让快速筛选时只有最后一次请求可以写入页面状态。 */
export const createLatestRequestGuard = (): LatestRequestGuard => {
  let latestRequestId = 0;
  return {
    begin: () => {
      latestRequestId += 1;
      return latestRequestId;
    },
    isCurrent: (requestId) => requestId === latestRequestId
  };
};
