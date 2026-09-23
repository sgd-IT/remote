// 注意： 本文接口均为 ICC 接口， 接口需要三方后端转发使用，每个方法对请求进行改写，固定格式请看注释
// 本项目实现：所有 ICC 接口由本项目后端转发（/eiot/video/camera/wsplayer/*），前端仅调用后端接口
import request from '@/config/axios'

// 跟平台相关的业务JS
const API = {
    /**
     * 请求实时视频流地址（WSPlayer 内部回调 getRealRtsp）
     * @returns Promise<>
     * @desc 后端转发 ICC StartVideo，返回 { url, token, session, channelId, deviceCode, streamType }
     *
     * 固定格式如下
     * return new Promise ((resolve, reject) => {
     *      ...
     *      resolve({     resolve 返回格式示例如下
     *          url: "",
     *          token: 1,
     *          ...
     *      })
     * })
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
            url: '/eiot/video/camera/wsplayer/start-video',
            params: { channelId, streamType: data.streamType || '2' }
        })
    },
    /**
     * 获取对讲的rtsp（WSPlayer 内部回调 getTalkRtsp）
     * @returns Promise<>
     * @desc 后端转发 ICC StartTalk
     *
     * 固定格式如下
     * return new Promise ((resolve, reject) => {
     *      ...
     *      resolve({     resolve 返回格式示例如下
     *          url: "",
     *          token: 1,
     *          ...
     *      })
     * })
    */
    getTalkRtsp(talkPram) {
        const data = talkPram?.data || {}
        return request.post({
            url: '/eiot/video/camera/wsplayer/talk/start',
            data: {
                deviceCode: data.deviceCode,
                talkType: data.talkType || '1', // 1-设备对讲 2-通道对讲
                audioType: data.audioType || '2', // 2-G711A 4-G711U
                channelSeq: data.channelSeq
            }
        })
    },

    /**
     * 停止对讲（WSPlayer 内部回调 stopTalk）
     * @desc 后端转发 ICC StopTalk
     */
    stopTalk(recordByTimeParam) {
        const data = recordByTimeParam?.data || {}
        return request.post({
            url: '/eiot/video/camera/wsplayer/talk/stop',
            data: {
                deviceCode: data.deviceCode,
                talkType: data.talkType || '1',
                session: data.session
            }
        })
    },

    /**
     * 获取通道权限（WSPlayer 内部可选回调，不传则播放器直接放行）
     * @returns Promise<{ result: true }>
     */
    fetchChannelAuthority() {
        return Promise.resolve({ result: true })
    },

    /**
     * 根据时间查询设备上的录像（暂未集成）
     * @param recordByTimeParam.recordType 录像类型：1=一般录像，2=报警录像
     * @param recordByTimeParam.recordSource 录像来源：1=全部，2=设备，3=中心
     * @param recordByTimeParam.streamType 码流类型：1=主码流, 2=辅码流 使用辅码流 码率低更加流畅
     * @param recordByTimeParam.channelId 通道code
     * @param recordByTimeParam.startTime 开始时间
     * @param recordByTimeParam.endTime 结束时间
     * @returns Promise<>
    */
    getRecordRtspByTime() {
        return Promise.reject(new Error('录像回放功能暂未集成'))
    },

    /**
     * 根据文件形式回放录像（暂未集成）
     * @returns Promise<>
    */
    getRecordRtspByFile() {
        return Promise.reject(new Error('录像回放功能暂未集成'))
    },

    /**
     * 查询中心录像上的录像文件信息（暂未集成）
     * @returns Promise<>
     */
    getRecords() {
        return Promise.reject(new Error('录像回放功能暂未集成'))
    },

    /**
     * 获取哪个日期有录像（暂未集成）
     */
    getChannelsMonthRecordStatus() {
        return Promise.reject(new Error('录像回放功能暂未集成'))
    },

    /**
     * 云台方向控制（暂未集成）
     * @param directionParam
     * @returns {Promise<unknown>}
     */
    setPtzDirection() {
        return Promise.reject(new Error('云台控制功能暂未集成'))
    },

    /**
     * 云台镜头控制（暂未集成）
     * @param cameraParam
     * @returns {Promise<unknown>}
     */
    setPtzCamera() {
        return Promise.reject(new Error('云台控制功能暂未集成'))
    },

    /**
     * 云台三维定位接口（暂未集成）
     * @param positionParam
     * @returns {Promise<unknown>}
     */
    controlSitPosition() {
        return Promise.reject(new Error('云台控制功能暂未集成'))
    },

    /**
     * 云台锁定接口（暂未集成）
     */
    setPtzLock() {
        return Promise.reject(new Error('云台控制功能暂未集成'))
    },

    /**
     * 查询实时预览配置（暂未集成）
     */
    searchRealtimePreviewConfig() {
        return Promise.reject(new Error('云台控制功能暂未集成'))
    },

    /**
     * 查询用户信息（用于云台抢占，暂未集成）
     */
    getUserInfo() {
        return Promise.reject(new Error('云台控制功能暂未集成'))
    }
}
export default API
