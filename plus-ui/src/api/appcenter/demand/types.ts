export interface AppDemandVo extends BaseEntity {
  demandId: number | string;
  demandType: string;
  appId?: number | string;
  appName: string;
  content: string;
  contact?: string;
  requesterId?: number | string;
  requesterName?: string;
  status: string;
  handleRemark?: string;
  handledBy?: number | string;
  handledTime?: string;
  createTime?: string;
}

export interface AppDemandForm {
  demandId: number | string | undefined;
  status: string;
  handleRemark: string;
}

export interface AppDemandQuery extends PageQuery {
  keyword: string;
  demandType: string;
  status: string;
}
