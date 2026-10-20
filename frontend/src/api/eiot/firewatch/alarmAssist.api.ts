import request from '@/config/axios'

enum Api {
  get = '/eiot/firewatch/alarm-assist/get'
}

/** 告警辅助判断 - 摄像头 */
export interface AlarmAssistCamera {
  cameraId: number
  name: string
  status: number | null
}

/** 告警辅助判断 - 设备（报警设备与同区域设备共用） */
export interface AlarmAssistDevice {
  deviceId: number
  name: string
  dn: string
  type: string
  location: string
  online: boolean
  alarming: boolean
  dataTime: number | null
  /** 非空 = 该设备可反控（自身是用传设备或父链上溯命中），值为用传设备ID；
   *  注意：辅助判断行的「设备控制室」仅 type='transfer' 行显示，不以此字段为准 */
  transferDeviceId: number | null
  cameras: AlarmAssistCamera[]
}

/** 告警辅助判断 - 安装空间 */
export interface AlarmAssistSpace {
  id: number
  building: string
  floor: string
  area: string
  position: string
  path: string
}

/** 告警辅助判断响应（GET /eiot/firewatch/alarm-assist/get） */
export interface AlarmAssistData {
  alarmId: number
  alertName: string
  alertLevel: string
  alarmDevice: AlarmAssistDevice | null
  space: AlarmAssistSpace | null
  relatedDevices: AlarmAssistDevice[]
  ownCameras: AlarmAssistCamera[]
}

/** 按告警ID查询辅助判断（安装空间 + 同区域设备 + 关联摄像头） */
export const getAlarmAssist = (alarmId: number): Promise<AlarmAssistData> => {
  return request.get({ url: Api.get, params: { alarmId } })
}
