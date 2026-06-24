export interface AppCategoryVo extends BaseEntity {
  categoryId: number | string;
  categoryName: string;
  categoryCode: string;
  icon: string;
  orderNum: number;
  status: string;
  remark: string;
}

export interface AppCategoryForm {
  categoryId: number | string | undefined;
  categoryName: string;
  categoryCode: string;
  icon: string;
  orderNum: number;
  status: string;
  remark: string;
}

export interface AppCategoryQuery {
  categoryName?: string;
  categoryCode?: string;
  status?: string;
}
