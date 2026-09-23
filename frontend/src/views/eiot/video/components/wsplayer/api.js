// 本项目 WSPlayer 对接 API 封装
// 大华 ICC 接口由本项目后端转发（evo-apigw 网关），前端仅调用后端接口
// WSPlayer 内部回调约定：resolve 返回 { url, token, session, ... } 结构
import request from '@/config/axios'

// 本项目后端 WSPlayer 转发接口基础路径
const WS_BASE = '/eiot/video/camera/wsplayer'

const API = {
  /**
   * 请求实时视频流地址（WSPlayer 内部回调 getRealRtsp）
   * @param realmonitorParam WSPlayer 传入：{ data: { channelId, streamType }, channelList: [{ id, deviceCode, ... }] }
   * @returns Promise<{ url, token, session, channelId, deviceCode, streamType }>
   */
  getRealmonitor(realmonitorParam) {
    const data = realmonitorParam?.data || {}
    const channelList = realmonitorParam?.channelList || []
    // WSPlayer 内置播放模式传入 4 段式通道编码，直接透传给后端
    const channelId = data.channelId || (channelList[0] && channelList[0].id) || null
    if (!channelId) {
      return Promise.reject(new Error('getRealmonitor 缺少 channelId'))
    }
    return request.get({
      url: WS_BASE + '/start-video',
      params: { channelId }
    })
  },

  /**
   * 获取对讲 rtsp（WSPlayer 内部回调 getTalkRtsp）
   * @param talkParam WSPlayer 传入：{ data: { deviceCode, channelSeq, audioType, sampleRate, audioBit } }
   * @returns Promise<{ url, token, session, audioType, audioBit, sampleRate, gbDevice }>
   */
  getTalkRtsp(talkParam) {
    const data = talkParam?.data || {}
    return request.post({
      url: WS_BASE + '/talk/start',
      data: {
        deviceCode: data.deviceCode,
        talkType: '1', // 1-设备对讲 2-通道对讲
        audioType: data.audioType || '2', // 2-G711A 4-G711U
        channelSeq: data.channelSeq
      }
    })
  },

  /**
   * 停止对讲（WSPlayer 内部回调 stopTalk）
   * @param talkParam WSPlayer 传入：{ data: { deviceCode, talkType, session } }
   */
  stopTalk(talkParam) {
    const data = talkParam?.data || {}
    return request.post({
      url: WS_BASE + '/talk/stop',
      data: {
        deviceCode: data.deviceCode,
        talkType: data.talkType || '1',
        session: data.session
      }
    })
  },

  // ============ 以下接口本次暂不集成（录像回放/云台），保留占位避免 WSPlayer 报错 ============

  /** 根据时间查询设备上的录像（暂未支持） */
  getRecordRtspByTime() {
    return Promise.reject(new Error('录像回放功能暂未集成'))
  },

  /** 根据文件形式回放录像（暂未支持） */
  getRecordRtspByFile() {
    return Promise.reject(new Error('录像回放功能暂未集成'))
  },

  /** 查询中心录像文件信息（暂未支持） */
  getRecords() {
    return Promise.reject(new Error('录像回放功能暂未集成'))
  },

  /** 云台方向控制（暂未支持） */
  setPtzDirection() {
    return Promise.reject(new Error('云台控制功能暂未集成'))
  },

  /** 云台镜头控制（暂未支持） */
  setPtzCamera() {
    return Promise.reject(new Error('云台控制功能暂未集成'))
  },

  /** 云台三维定位（暂未支持） */
  controlSitPosition() {
    return Promise.reject(new Error('云台控制功能暂未集成'))
  }
}

export default API
