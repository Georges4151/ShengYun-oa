/**
 * 小程序 JSON-Schema 表单引擎的 schema 定义与注册表。
 * schema 使用 JSON Schema 的子集：type/enum/required/maxLength/minimum/maximum，
 * 通过 format 指定 date/time/textarea/attachment 控件，enumNames 提供中文选项文案。
 */
import assetBorrowSchema from './asset-borrow.json'
import changeRequestSchema from './change-request.json'
import checkinSupplementSchema from './checkin-supplement.json'
import expenseSchema from './expense.json'
import hrLeaveSchema from './hr-leave.json'
import meetingApplySchema from './meeting-apply.json'
import vehicleApplySchema from './vehicle-apply.json'

export type FormFieldType = 'string' | 'number' | 'integer'

export interface FormField {
  /** 表单字段名，同时作为提交给接口的 key */
  key: string
  label: string
  type: FormFieldType
  /** 控件形态；缺省按 type 推断为单行文本或数字输入 */
  format?: 'textarea' | 'date' | 'time' | 'attachment'
  /** 枚举值列表，存在时渲染为下拉选择 */
  enum?: string[]
  /** 与 enum 一一对应的中文文案 */
  enumNames?: string[]
  required?: boolean
  maxLength?: number
  minimum?: number
  maximum?: number
  /** 数值步进：要求数值为该值的整数倍，如 0.5 表示支持半天 */
  multipleOf?: number
  placeholder?: string
  /** 字段下方说明文案 */
  help?: string
  default?: string | number
}

export interface FormSection {
  title: string
  fields: FormField[]
}

export interface FormSchema {
  bizType: string
  sections: FormSection[]
}

/** 表单模型统一用字符串承载，提交前由页面按需转换。 */
export type FormModel = Record<string, string>

const SCHEMAS: Record<string, FormSchema> = {
  EXPENSE: expenseSchema as unknown as FormSchema,
  CHANGE_REQUEST: changeRequestSchema as unknown as FormSchema,
  HR_LEAVE: hrLeaveSchema as unknown as FormSchema,
  // M6 行政域与 HR 补卡表单：页面级资源选择器保留，仅字段由 schema 渲染
  ADMIN_ASSET_BORROW: assetBorrowSchema as unknown as FormSchema,
  ADMIN_VEHICLE_APPLY: vehicleApplySchema as unknown as FormSchema,
  ADMIN_MEETING_APPLY: meetingApplySchema as unknown as FormSchema,
  HR_CHECKIN_SUPPLEMENT: checkinSupplementSchema as unknown as FormSchema
}

/** 按业务类型获取表单 schema；未注册时返回 undefined。 */
export function getFormSchema(bizType: string): FormSchema | undefined {
  return SCHEMAS[bizType.toUpperCase()]
}

/** 依据 schema 生成初始表单模型，未声明的字段一律为空串。 */
export function createFormModel(schema: FormSchema): FormModel {
  const model: FormModel = {}
  for (const section of schema.sections) {
    for (const field of section.fields) {
      model[field.key] = field.default === undefined ? '' : String(field.default)
    }
  }
  return model
}
