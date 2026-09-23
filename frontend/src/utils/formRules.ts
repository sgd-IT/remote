const { t } = useI18n()

// 必填项
export const required = {
  required: true,
  message: t('common.required')
}

/** 密码复杂度校验器工厂：长度不低于 8 位，且包含大小写字母、数字、特殊符号中至少三类 */
export const validatePassword = (emptyMsg: string = '密码不能为空') => {
  return (_rule: any, value: string, callback: (error?: string | Error) => void) => {
    if (!value) {
      callback(new Error(emptyMsg))
      return
    }
    if (value.length < 8) {
      callback(new Error('密码长度不能低于 8 位'))
      return
    }
    const categories = [/[a-z]/, /[A-Z]/, /\d/, /[^a-zA-Z0-9]/].filter((re) => re.test(value)).length
    if (categories < 3) {
      callback(new Error('密码必须包含大小写字母、数字、特殊符号中至少三类'))
      return
    }
    callback()
  }
}
