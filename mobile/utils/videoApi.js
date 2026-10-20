// 视频监控接口封装（大华 ICC 流经本项目后端转发，小程序端播放 HLS）
import { get } from '@/utils/request.js'

/** 消控室监控摄像头：按名称关键词"值班室"筛选，后端自动按当前登录租户过滤 */
export const getDutyRoomCameras = (keyword = '值班室') =>
	get('/eiot/video/camera/page', { pageNo: 1, pageSize: 50, keyword })

/** 摄像头实时流地址（HLS）：token 一次性，每次播放/切换都必须重新调用，禁止缓存复用 */
export const getCameraLiveUrl = (cameraId) =>
	get('/eiot/video/camera/live/url', { cameraId })
