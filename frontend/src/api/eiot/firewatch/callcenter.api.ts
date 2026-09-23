// 电话中心 API：座机号码管理 + 话单通话记录
import request from '@/config/axios'

enum Api {
  // 座机号码管理
  dutyPhoneCreate = '/eiot/firewatch/duty-phone/create',
  dutyPhoneUpdate = '/eiot/firewatch/duty-phone/update',
  dutyPhoneDelete = '/eiot/firewatch/duty-phone/delete',
  dutyPhoneGet = '/eiot/firewatch/duty-phone/get',
  dutyPhonePage = '/eiot/firewatch/duty-phone/page',
  dutyPhoneList = '/eiot/firewatch/duty-phone/list',

  // 话单通话记录
  callRecordPage = '/eiot/firewatch/call-record/page',
  callRecordGet = '/eiot/firewatch/call-record/get',
  callRecordVoice = '/eiot/firewatch/call-record/voice'
}

// ==================== 座机号码管理 API ====================

// 创建座机号码
export const createDutyPhone = (data: any) => {
  return request.post({ url: Api.dutyPhoneCreate, data })
}

// 更新座机号码
export const updateDutyPhone = (data: any) => {
  return request.put({ url: Api.dutyPhoneUpdate, data })
}

// 删除座机号码
export const deleteDutyPhone = (id: number) => {
  return request.delete({ url: Api.dutyPhoneDelete, params: { id } })
}

// 获取座机号码详情
export const getDutyPhone = (id: number) => {
  return request.get({ url: Api.dutyPhoneGet, params: { id } })
}

// 分页查询座机号码
export const getDutyPhonePage = (params: any) => {
  return request.get({ url: Api.dutyPhonePage, params })
}

// 获取座机号码列表（不分页）
export const getDutyPhoneList = () => {
  return request.get({ url: Api.dutyPhoneList })
}

// ==================== 话单通话记录 API ====================

// 分页查询通话记录
export const getCallRecordPage = (params: any) => {
  return request.get({ url: Api.callRecordPage, params })
}

// 获取通话记录详情
export const getCallRecord = (id: number) => {
  return request.get({ url: Api.callRecordGet, params: { id } })
}

// 获取录音文件（blob，用于播放）
export const getCallRecordVoice = (id: number, side?: string) => {
  return request.get({
    url: `${Api.callRecordVoice}/${id}`,
    params: { side },
    responseType: 'blob'
  })
}
