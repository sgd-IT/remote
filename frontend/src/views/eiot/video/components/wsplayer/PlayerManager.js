// WSPlayer源码调试时使用
// import WSPlayer from '../WSPlayer/WSPlayer'
import API from "./api.js";
/**
 * WSPlayer是核心组件
 * API 封装了接口
 */
class PlayerManager {
    constructor(opt) {
        if(!opt.el) {
            console.warn(`请初始化播放器时传入必传参数：
new PlayerManager({
    el: "wsplayer_id", // 播放器所在的容器ID
    ...
})`)
        }
        // 播放器所在的容器ID
        this.el = opt.el;
        // 实时预览播放器
        this.realPlayer = null;
        // 录像回放播放器
        this.recordPlayer = null;
        this.player = null;
        // 实时预览还是录像回放播放器
        this.type = "real";
        // 窗口的数量
        this.playNum = 1;
        // 当前选中的窗口的索引
        this.playIndex = 0;
        // 当前选中窗口正在播放视频的通道
        this.currentChannelId = "";
        // 索引对应窗口，保存当日录像信息
        this.recordList = [];

        /**
         * 兼容老版本字段 【已废弃】
         * 在 ws/wss 直连模式下, serverIp 表示MTS流媒体服务 IP
         * 在 代理 模式下, serverIp 表示的是 代理服务器的IP 地址
         */
         this.serverIp = opt.serverIp

         /**
          * V1.2.8 版本新增字段
          * 解码库资源文件前缀
          */
         this.prefixUrl = opt.prefixUrl || './static'
         /**
          * V1.2.8 版本新增字段
          * 优先级最高，该方法传入后
          * 结构如下：
          * eg:
          * (rtspUrl) => {
          *     return "wss://{ip}:9102"
          * }
          * 配置后：protocol, isIntranet, proxyServerIp, streamServerIp
          */
         this.setWSUrl = opt.setWSUrl
        /**
         * V1.2.8 版本新增字段
         * websocket协议，http支持wss协议拉流，因此增加protocol配置
         */
        this.protocol = opt.protocol
        /**
         * V1.2.8 版本新增字段[组合使用]
         * @desc 注意事项： 用于有内外网场景和分布式场景
         * @param isIntranet 是否内网, 用于有内外网环境，多台大华服务器[分布式]拉流场景配置
         * @param intranetMap 映射关系，结构 { "内网ip": "外网ip", ... }
         *
         */
        this.isIntranet = opt.isIntranet
        this.intranetMap = opt.intranetMap
         /**
          * V1.2.7 版本新增字段
          * 用于只有内网
          * @desc 代理 模式下，代理服务器的IP 地址
          * 如果代理服务有端口则拼接端口
          *
          */
         this.proxyServerIp = opt.proxyServerIp
         /**
          * V1.2.7 版本新增字段
          * 用于只有内网
          * @desc 流媒体服务 IP 地址
          * 如果拉流的 9100 9320 端口发生修改，则需要后面拼接修改后的端口
          */
         this.streamServerIp = opt.streamServerIp
         /**
          * V1.2.7 版本新增字段
          * @desc 模式类型 是直连模式/代理模式
          */
         this.useNginxProxy = opt.useNginxProxy || false

        if(opt.importLoad) {
            this.loadWSPlayer(() => {
                this.initPlayer(opt)
            })
            return
        }
        this.initPlayer(opt)
    }

    loadWSPlayer(callback) {
        let dom = document.createElement("script");
        dom.src = `${this.prefixUrl}/WSPlayer/WSPlayer.js`;
        document.head.appendChild(dom);
        var loaded = false;
        if(typeof callback === 'function') {
            dom.onload = dom.onreadystatechange = function() {
                if(!loaded && (!dom.readyState || /loaded|complete/.test(dom.readyState))) {
                    dom.onload = dom.onreadystatechange = null;
                    loaded = true;
                    callback();
                }
            }
        }
    }

    // 初始化播放器
    initPlayer(opt) {
        // 初始化播放器
        if(window.WSPlayer.WSPlayer) {
            window.WSPlayer = window.WSPlayer.WSPlayer
        }
        switch(opt.type) {
            case "real":
                this.initRealPlayer(opt);
                break;
            case "record":
                this.initRecordPlayer(opt);
                break;
            default:
                console.warn(`请初始化播放器时传入必传参数：
new PlayerManager({
    type: "real", // real-实时预览 record-录像回放
    ...
})`)
                break;
        }
    }

    /**
     * 初始化实时预览播放器
     */
    initRealPlayer(opt) {
        this.playNum = opt.num;
        this.type = "real";
        this.realPlayer = new WSPlayer({
            el: this.el,
            type: 'real',
            importLoad: opt.importLoad, // 是否动态加载库
            serverIp: this.serverIp, // V1.2.7 版本已废弃[兼容老版本]
            prefixUrl: this.prefixUrl, // 解码库资源前缀
            protocol: this.protocol, // 协议 ws  wss
            isIntranet: this.isIntranet, // 传入当前是 内网还是外网, true-内网 false-外网. 内外网 + 分布式下, 使用该模式
            intranetMap: this.intranetMap, // 传入当前的内外网关系映射 {"内网ip": "外网ip"}.  内外网 + 分布式下, 使用该模式
            proxyServerIp: this.proxyServerIp, // V1.2.7 版本新增该配置 代理服务器的ip 【ws/wss直连时不传】
            streamServerIp: this.streamServerIp, // V1.2.7 版本新增该配置 流媒体服务器的ip
            rtspResponseTimeout: opt.rtspResponseTimeout, // rtsp 拉流超时时间
            playerAdapter: opt.playerAdapter || 'selfAdaption', // 默认实际比例, selfAdaption 实际比例 | stretching 画面铺满
            setWSUrl: this.setWSUrl, // 开放式的 传入函数， return websocket 连接地址，第三方自行处理
            ENV: this.ENV,
            config: {
                num: opt.num || 1,
                maxNum: opt.maxNum || 25,
                showControl: opt.showControl, // 默认是否显示底部工具栏
                showTopOperate: opt.showTopOperate, // 是否显示播放顶部操作
                useNginxProxy: this.useNginxProxy, // V1.2.7 版本新增该配置 直连时需要改为 false, 代理模式为 true
                showIcons: opt.showIcons || {
                    streamChangeSelect: true, // 主辅码流切换
                    replayIcon:true, // 刷新重播按钮
                    ivsIcon: true, // 控制智能帧按钮
                    talkIcon: true, // 对讲功能按钮
                    localRecordIcon: true, // 本地录像功能按钮
                    audioIcon: true, // 开启关闭声音按钮
                    snapshotIcon: true, // 抓图按钮
                    closeIcon: true, // 关闭视频按钮
                }, // V1.2.8 新增配置 顶部按钮自定义
                onlyLoadSingleLib: typeof opt.onlyLoadSingleLib === 'boolean' ? opt.onlyLoadSingleLib : false, // 是否走单线程
                isRTPStream: typeof opt.isRTPStream === 'boolean' ? opt.isRTPStream : false, // true-rtp标准拉流 false-私有拉流
                openIvs: typeof opt.openIvs === 'boolean' ? opt.openIvs : true, // 默认显示智能帧
                draggable: opt.draggable, // 1.3.0 新增，是否支持拖拽
                downloadMp4Record: typeof opt.downloadMp4Record === 'boolean' ? opt.downloadMp4Record : true,
                useH264MSE: typeof opt.useH264MSE === 'boolean' ? opt.useH264MSE : true,
                useH265MSE: typeof opt.useH265MSE === 'boolean' ? opt.useH265MSE : true,
                localeLang: opt.localeLang || 'zhCN', // zhCN 中文 enUS 英文
                localeI18n: opt.localeI18n, // 1.3.2新增配置
                cacheMode: opt.cacheMode || 1, //1.3.4新增 0-关闭实时流自适应缓冲模式 1-自适应缓冲 2-实时优先 3-流畅优先 4-智能缓冲模式
                nMSEFrameCount: opt.nMSEFrameCount, // MSE硬解码每个包的帧数(用于处理卡顿下，一次给video标签推的帧数，可以有效的防止画面过于卡顿，但是会影响实时性)
                ivsTypeArr: opt.ivsTypeArr || [1, 2], // 传入数组，支持显示的情况, 空表示没有智能信息，1-智能规则线 2-智能目标框
                isWebView: opt.isWebView, // 是否webview引擎 true-是 false-不是
                picCapCb: opt.picCapCb, // 是否需要抓图回调
                recordCb: opt.recordCb, // 是否需要本地录制数据回调
                dblclickDisabled: typeof opt.dblclickDisabled === 'boolean' ? opt.dblclickDisabled : false // 是否禁用默认的双击窗口全屏事件
            },
            receiveMessageFromWSPlayer: opt.receiveMessageFromWSPlayer || this.__receiveMessageFromWSPlayer.bind(this),
            getRealRtsp: API.getRealmonitor, // 获取实时预览rtsp接口 (不要做任何改动, 只传入方法, API.getRealmonitor() 是错误的， 会报错)
            getTalkRtsp: API.getTalkRtsp, // 获取对讲rtsp接口  (不要做任何改动, 只传入方法, API.getTalkRtsp() 是错误的， 会报错)
            stopTalk: API.stopTalk, // 关闭对讲rtsp接口  (不要做任何改动, 只传入方法, API.stopTalk() 是错误的， 会报错)
        })
        this.player = this.realPlayer;
        // 初始化云台控制组件
        if(opt.pztEl) {
            this.realPlayer.initPanTilt({
                // 云台控制容器的id
                el: opt.pztEl,
                // 云台区域控制接口
                setPtzDirection: API.setPtzDirection,
                // 云台镜头控制接口
                setPtzCamera: API.setPtzCamera,
                // 云台三维定位接口
                controlSitPosition: API.controlSitPosition
            })
        }
    }

    /**
     * 初始化录像回放播放器
     */
    initRecordPlayer(opt) {
        this.playNum = opt.num;
        this.type = "record";
        this.recordPlayer = new WSPlayer({
            el: this.el,
            type: 'record',
            importLoad: opt.importLoad, // 是否动态加载
            serverIp: this.serverIp, // V1.2.7 版本已废弃[兼容老版本]
            prefixUrl: this.prefixUrl, // 解码库资源前缀
            protocol: this.protocol, // 协议 ws  wss
            isIntranet: this.isIntranet, // 传入当前是 内网还是外网, true-内网 false-外网. 内外网 + 分布式下, 使用该模式
            intranetMap: this.intranetMap, // 传入当前的内外网关系映射 {"内网ip": "外网ip"}.  内外网 + 分布式下, 使用该模式
            proxyServerIp: this.proxyServerIp, // V1.2.7 版本新增该配置 代理服务器的ip 【ws/wss直连时不传】
            streamServerIp: this.streamServerIp, // V1.2.7 版本新增该配置 流媒体服务器的ip
            playerAdapter: opt.playerAdapter || 'selfAdaption', // 默认实际比例,  selfAdaption 实际比例 | stretching 画面铺满
            setWSUrl: this.setWSUrl, // 开放式的 传入函数， return websocket 连接地址，第三方自行处理
            rtspResponseTimeout: opt.rtspResponseTimeout, // rtsp 拉流超时时间
            ENV: this.ENV,
            config: {
                num: opt.num,
                maxNum: opt.maxNum,
                showControl: opt.showControl, // 是否显示播放器底部工具栏，默认显示
                showTopOperate: opt.showTopOperate, // 是否显示播放顶部操作
                useNginxProxy: this.useNginxProxy, // V1.2.7 版本新增该配置 直连时需要改为 false, 【代理模式可不传】
                showIcons: opt.showIcons || {
                    streamChangeSelect: true, // 主辅码流切换
                    replayIcon:true, // 刷新重播按钮
                    ivsIcon: true, // 控制智能帧按钮
                    talkIcon: true, // 对讲功能按钮
                    localRecordIcon: true, // 本地录像功能按钮
                    audioIcon: true, // 开启关闭声音按钮
                    snapshotIcon: true, // 抓图按钮
                    closeIcon: true, // 关闭视频按钮
                }, // V1.2.8 新增配置 顶部按钮自定义
                recordProgressStyle: opt.recordProgressStyle || {
                    fontSize: 12,
                    centerFontSize: 14,
                    rulerLength: {
                        short: 6,
                        middle: 12,
                        long: 18
                    },
                    rulerScale: 2,
                    fontOffsetX: 0,
                    centerFontOffsetX: 55,
                    recordsColor: "#318efd"
                }, // 录像进度条样式
                openIvs: typeof opt.openIvs === 'boolean' ? opt.openIvs : true, // 默认显示智能帧
                onlyLoadSingleLib: typeof opt.onlyLoadSingleLib === 'boolean' ? opt.onlyLoadSingleLib : false, // 是否走单线程
                isRTPStream: typeof opt.isRTPStream === 'boolean' ? opt.isRTPStream : false, // true-rtp标准拉流 false-私有拉流
                showRecordProgressBar: typeof opt.showRecordProgressBar === 'boolean' ? opt.showRecordProgressBar : true, // V1.2.8 新增配置 是否显示回放进度条
                draggable: opt.draggable, // 1.3.0 新增，是否支持拖拽
                downloadMp4Record: typeof opt.downloadMp4Record === 'boolean' ? opt.downloadMp4Record : true,
                useH264MSE: typeof opt.useH264MSE === 'boolean' ? opt.useH264MSE : true,
                useH265MSE: typeof opt.useH265MSE === 'boolean' ? opt.useH265MSE : true,
                localeLang: opt.localeLang || 'zhCN', // zhCN 中文 enUS 英文
                localeI18n: opt.localeI18n, // 1.3.2新增配置
                ivsTypeArr: opt.ivsTypeArr || [1, 2], // 传入数组，支持显示的情况, 空表示没有智能信息，1-智能规则线 2-智能目标框
                isWebView: opt.isWebView, // 是否webview引擎 true-是 false-不是
                picCapCb: opt.picCapCb, // 是否需要抓图回调
                recordCb: opt.recordCb, // 是否需要本地录制数据回调
                dblclickDisabled: typeof opt.dblclickDisabled === 'boolean' ? opt.dblclickDisabled : false // 是否禁用默认的双击窗口全屏事件
            },
            receiveMessageFromWSPlayer: opt.receiveMessageFromWSPlayer || this.__receiveMessageFromWSPlayer.bind(this),
            getRecords: API.getRecords, // 获取录像列表接口  (不要做任何改动, 只传入方法, API.getRecords() 是错误的， 会报错)
            getRecordRtspByTime: API.getRecordRtspByTime, // 根据时间形式获取录像rtsp接口 (不要做任何改动, 只传入方法, API.getRecordRtspByTime() 是错误的， 会报错)
            getRecordRtspByFile: API.getRecordRtspByFile, // 根据文件形式获取录像rtsp接口 (不要做任何改动, 只传入方法, API.getRecordRtspByFile() 是错误的， 会报错)
        })
        this.player = this.recordPlayer;
    }

    /**
     * 播放实时预览视频
     * @param opt.channelList: {Array<Object>} 必填，通道列表
     * @param opt.streamType: {Number|String} 选填，码流类型，不填默认播放辅码流1，若不存在辅码流1，则自动切换到主码流  1-主码流 2-辅码流1 3-辅码流2
     * @param opt.windowIndex: {Number} 选填，指定从哪个窗口开始播放。不填默认从选中的窗口开始播放
     */
    // channelList: [{
    //     id: channelCode, // {String} 通道编码 -- 用于预览，必填
    //     deviceCode: deviceCode, // {String} 设备编码 -- 用于对讲，对讲必填，无对讲功能可不填
    //     deviceType: deviceType, // {String} 设备类型 -- 用于对讲，对讲必填，无对讲功能可不填
    //     channelSeq: channelSeq, // {String|Number} 通道序号 -- 用于对讲，对讲必填，无对讲功能可不填
    //     cameraType: cameraType, // {String|Number} 摄像头类型 -- 用于云台，云台必填，无云台功能可不填
    //     capability: capability, // {String} 能力集 -- 用于云台，云台必填，无云台功能可不填
    // }]
    playRealVideo(opt) {
        this.realPlayer && this.realPlayer.playRealVideo({
            channelList: opt.channelList,
            streamType: opt.streamType,
            windowIndex: opt.windowIndex
        })
    }

    /**
     * 传入 wsUri 和 wsUrl 播放预览视频
     * @param { String } opt.rtspURL 必传 rtsp流地址
     * @param { String } opt.wsURL 必传 建立的websocket连接地址
     * @param { String } opt.channelId 必传 通道id
     * @param { String } opt.streamType 必传 码流类型 1-主码流 2-辅码流1  3-辅码流2
     * @param { String } opt.streamServerIp 选传 流媒体服务ip
     * @param { String } opt.playerAdapter 选传 是否拉伸窗口 selfAdaption 自适应 | stretching 拉伸
     * @param { Number } opt.selectIndex 必传 窗口号 从0开始
     * @param { Object } opt.channelData 选传 通道信息，若使用云台功能，则必传
     */
    realByUrl(opt) {
        this.player && this.player.realByUrl(opt)
    }

    /**
     * @name talkByUrl
     * @desc 通过流地址方式进行对讲
     * @param playTalk 流地址方式
     * @param rtspURL rtsp的地址
     * @param wsURL websocket地址
     * @param selectIndex 窗口号
     * @param audioType 音频类型
     * @param sampleRate 采样率
     */
    talkByUrl(opt) {
        this.realPlayer && this.realPlayer.talkByUrl(opt)
    }

    // 参数同上
    onlyTalk(opt) {
        this.realPlayer && this.realPlayer.onlyTalk(opt)
    }
    /**
     * @name 停止流地址对讲
     * @desc 流地址停止对讲
     */
    stopUrlTalk(windowIndex) {
        this.realPlayer && this.realPlayer.stopUrlTalk(windowIndex)
    }

    // 音量设置
    /**
     * 开启声音
     */
    openVolume(windowIndex) {
        this.player.openVolume(windowIndex)
    }

    /**
     * 关闭声音
     */
    closeVolume(windowIndex) {
        this.player.closeVolume(windowIndex)
    }

    /**
     * 设置声音
     * @param volume 声音大小 传 0-1 小数点一位的小数
     */
    setVolume(windowIndex, volume) {
        this.player.setVolume(windowIndex, volume)
    }

    /**
     * 播放录像回放
     * @param opt.channelList {Array<Object>} 通道集合 必填
     * @param opt.startTime {String|Number} 开始时间  必选  timestamp到秒
     * @param opt.endTime {String|Number} 结束时间  必选  timestamp到秒
     * @param opt.recordSource {String|Number} 录像来源  必选 2表示设备录像  3表示中心录像
     * @param opt.streamType {String|Number} 码流类型 可选
     * @param opt.recordType {String|Number} 录像类型 可选
     * @param {Boolean} opt.playRecordByFile 选传 是否根据文件播放，默认为false
     */
    // channelList: [{
    //     id: channelCode, // {String} 通道编码 -- 用于回放，必填
    // }]
    playRecordVideo(opt) {
        this.recordPlayer && this.recordPlayer.playRecordVideo(opt)
    }

    /**
     * 传入 wsUri 和 wsUrl 播放回放视频
     * @param {Object} opt
     * @param {String} opt.wsURL 必传 拉流的websocket连接地址
     * @param {String} opt.rtspURL 必传 rtsp地址
     * @param {String} opt.channelId 必传 通道id
     * @param {String} opt.startTime 必传 开始时间 到秒的时间戳
     * @param {String} opt.endTime 必传 结束时间 到秒的时间戳
     * @param {String} opt.playerAdapter 选传 是否拉伸窗口 selfAdaption 自适应 | stretching 拉伸
     * @param {Number} opt.selectIndex 选传 窗口号
     * @param {Object} opt.channelData 选传 通道信息
     * @param {Array} opt.records 录像文件信息 [数组格式，用于渲染进度条]
     * @param {Boolean} opt.playRecordByTime 是否设备录像播放 true-设备录像播放，false-中心录像播放
     * @param {Boolean} opt.isSeekRecord 是否连续上一次播放
     */
    recordByUrl(opt) {
        this.player && this.player.recordByUrl(opt)
    }

    // 更新窗口
    updatePlayerWindow() {
        this.player.__updatePlayerWindow()
    }

    // 开始对讲
    playTalkAudio(selectIndex) {
        this.player.playTalkAudio(selectIndex)
    }

    /**
     * 录像暂停
     * 只有正在播放的录像调用才有效
     * @param { number } index 窗口号
     */
    pause(index) {
        this.recordPlayer && this.recordPlayer.pause(index);
    }

    /**
     * 录像暂停后播放
     * 只有暂停后的录像调用才有效
     * @param { number } index 窗口号
     */
    play(index) {
        this.recordPlayer && this.recordPlayer.play(index);
    }

    /**
     * 倍速播放
     * @param { number } speed 速率 0.125 0.25 0.5 1 1.25 1.5 2 4 8 共7种速率
     * @param { number } index 窗口号
     */
    playSpeed(speed, index) {
        this.recordPlayer && this.recordPlayer.playSpeed(speed, index);
    }

    /**
     * 关闭播放器
     * @param {number} index 可选，关闭指定索引的窗口的播放器，不传则表示关闭所有播放器
     */
    close(index, closeReason="api") {
        this.player && this.player.close(index, closeReason);
    }

    /**
     * 设置全屏
     */
    setFullScreen() {
        this.player.setFullScreen();
    }
    /**
     * 设置退出全屏
     */
    setExitFullScreen() {
        this.player.setExitFullScreen();
    }

    /**
     * 设置窗口自适应还是拉伸
     * @param {string} playerAdapter selfAdaption 自适应 | stretching 拉伸
     */
    setPlayerAdapter(playerAdapter) {
        this.player.setPlayerAdapter(playerAdapter);
    }

    /**
     * 控制视频播放器显示的路数: 1 2 3 4 6 8 9 16 25，不会超过最大显示路数
     * @param {number} number
     */
    setPlayerNum(number) {
        this.player.setPlayerNum(number);
    }

    /**
     * 设置选中的播放器的索引
     * @param {number} index 设置选中的窗口的索引
     */
    setSelectIndex(index) {
        this.player.setSelectIndex(index);
    }

    /**
     * 录像跳转播放
     * /**
     * 根据时间跳转播放
     * @param {String|Number} time
     * time格式支持情况如下：
     * new Date("2024-11-01 01:00:00") // 时间戳对象格式 (直接跳转到当前时间)
     * "2024-11-01 01:00:00" // 时间字符串 (同上)
     * "2024/11/01 01:00:00" // 时间字符串 (同上)
     * 1706979600 // 到秒时间戳, (同上)
     * "1706979600" // 到秒时间戳字符串 (同上)
     * 1706979600000 // 到毫秒时间戳 (同上)
     * "1706979600000" // 到毫秒时间戳字符串 (同上)
     * "01:00:00" // 当日时分秒跳转 (此值跳转到开始播放的当前的时间点， 如录像是 11-1号的，则跳转到 11-1号01:00:00)
     * "3600" // 1点的时间戳 (此值根据录像的开始时间，往后推1小时，跳转到这个时间点, 如开始时间为 11-1号2点，则跳转到 11-1号3点开始播放)
     * 3600 // 1点的时间戳字符串 (同上)
     * @param {number} selectIndex 窗口号
     */
    jumpPlayByTime(time, selectIndex) {
        this.player.jumpPlayByTime(time, selectIndex);
    }

    /**
     * 开始本地录像
     * @param selectIndex 选择本地录像下载的窗口索引 必填
     * @param name 录像名称 必填
     * @param size 单个录像文件大小（单位M） 可选，默认值为 ./config/config.js 中 localRecordSize
     * @param downloadMp4Record 可选，默认值为全局配置。 true - 以MP4格式下载录像，false - 以dav格式下载录像
     * @param segment 可选，是否分段下载录像。 true - 分段下载, false - 不分段下载
     */
    startLocalRecord(selectIndex, name, size, downloadMp4Record, segment) {
        this.player.startLocalRecord(selectIndex, name, size, downloadMp4Record, segment)
    }
    /**
     * 停止本地录像下载
     * @param {Number} selectIndex 选择关闭本地录像下载的窗口索引
     */
    stopLocalRecord(selectIndex) {
        this.player.stopLocalRecord(selectIndex)
    }

    /**
     * 智能规则线和目标框设置
     * @param {Boolean} showIvs 是否显示智能规则线和目标框，true-显示，false-隐藏
     * @param {Number} selectIndex 选择的窗口号
     * @param {Array} ivsType [1] - 智能规则线， [2] - 智能目标框， [1, 2] - 规则线和智能目标框都显示
     */
    setIvs(showIvs, selectIndex, ivsType) {
        this.player.setIvs({
            showIvs,
            ivsType,
            selectIndex
        })
    }

    /**
     * 抓图
     * @param {Number} index 抓图的窗口索引
     * @param {String} name 抓图的图片名称（选填）
     */
    picCap(index, name) {
        this.player.picCap(index, name)
    }

    /**
     * 在窗口里显示自定义信息
     * @param index 窗口索引
     * @param msg 显示的信息
     * @param channelData 通道信息
     */
    showMsgInWindow(index, msg, channelData) {
        this.player.showMsgInWindow(index, msg, channelData)
    }

    /**
     * 创建WSPlayer蒙层
     */
    createWSPlayerLayer(layerId, callback) {
        this.player.createWSPlayerLayer(layerId, callback)
    }

    /**
     * 创建自定义dom(可自定义水印,智能帧等)
     * @param {*} domId
     */
    createCustomDom(domId) {
        this.player.createCustomDom(domId)
    }

    /**
     * destroy 销毁播放器
     * @param {*} method
     * @param {*} data
     */
    destroy() {
        this.player.destroy();
    }

    // ----------------- 播放器事件 ------------------------
    __receiveMessageFromWSPlayer(method, data) {
        switch(method) {
            // ------------- 公共事件 ---------------------
            case "initializationCompleted":
                // 初始化完成，可调用播放方法（适用于动态加载解码库）
                break;
            case "realSuccess": // 实时预览成功
                console.log("实时预览成功")
                break;
            case "realError": // 实时预览失败
                console.log("实时预览失败", err)
                break;
            case "talkSuccess": // 对讲成功 V1.3.4版本新增
                console.log("对讲成功");
                break;
            case "talkError": // 对讲失败
                console.log("对讲失败");
                break;
            case "recordSuccess": // 录像回放成功
                console.log("录像回放成功");
                break;
            case "recordError": // 录像回放失败
                console.log("录像回放失败", err);
                break;
            case 'recordFinish':
                console.log("当前录像回放完成", data)
                break;
            case "selectWindowChanged": // 选中的窗口发生改变
                this.currentChannelId = data.channelId;
                this.playIndex = data.playIndex;
                break;
            case "windowNumChanged": // 播放器显示的路数发生改变
                this.playNum = data;
                break;
            case "clickWindow":
                console.log("点击窗口");
                break;
            case "dblclickWindow":
                console.log("双击窗口")
                break;
            case "closeVideo": // 视频关闭
                // 点击关闭按钮引发的视频关闭进行提示
                // 切换视频引发的视频关闭不进行提示
                if(!data.changeVideoFlag) {
                    console.log(`窗口${data.selectIndex}的视频已关闭`)
                }
                break;
            case "statusChanged": // 视频状态发生改变
                break;
            case "getVideoFrameInfo":
                // 获取视频帧信息
                break;
            case "getAudioFrameInfo":
                // 获取视频内音频帧信息
                break;
            case "getPlayBackStreamRange":
                // 获取录像回放时长
                console.log("getPlayBackStreamRange", data)
                break;
            case "dragWindow":
                // 拖拽窗口
                console.log(`窗口${data.dragIndex}的视频拖拽到窗口${data.dropIndex}上`)
                break;
            case "errorInfo": // 错误信息提示
                console.log(data, "可打印查看错误消息");
                // data = {
                //     errorCode: xxx,
                //     errorMsg: "",
                //     errorData: {
                //         channelList: [],
                //         apiErrorInfo: {},
                //         method: "",
                //         arguments: [],
                //     },
                // }
                console.error(data)
                break;
            case "customDomInfo": // 窗口大小发生变化，视频播放时均会触发
				// data.customDomId 为每一个小窗口的自定义dom的id
				// data.width 为当前窗口的宽度
				// data.height 为当前窗口的高度
				// data.currentWindowPlaying 当前窗口是否正在播放视频
				// 根据上述内容 进行水印的添加，或其他操作
				break;
            case "chromeMemoryOut":
                console.warn("浏览器资源不足！")
                break;
            case "fullScreen":
                console.log("点击播放器底部的全屏按钮触发此回调")
                break;
            case 'picCap': // 抓图回调，配置在 new PlayerManager 中配置 picCapCb为true后即可触发
                // data.channelData
                // data.selectIndex 窗口索引
                // data.base64Img 图片base64字符串
                break;
            case 'localRecordBuf': // 本地录像回调，一帧一帧返回
                break;
            case 'localRecordFile': // 本地录像停止时返回Blob数据
                break;
            case "notifyTalk": // 点击顶部对讲按钮，触发此回调
                // 当采用 realByUrl 进行实时预览时，点击顶部对讲按钮，会触发此回调，需要在此回调内 获取对讲rtsp发起对讲
            case "switchStartTime":
                // 在调用 recordByUrl 中配置 playRecordByTime: false 后， 每次拖拽都会触发此回调，此回调需要重新获取rtsp拉流(不支持直接拖拽跳转的设备需要重新拉流，走此回调)
                break;
            case "setSeekTime":
                // 此回调不需要重新获取rtsp拉流，录像内部会自动跳转，只返回时间告知用户。如此方法跳转无效，请采用上述 switchStartTime方法
                // 在调用 recordByUrl 中配置 playRecordByTime: true 后, 录像进度条触发拖拽跳转，默认触发此回调。返回跳转后的时间
                break;
            default:
                break;
        }
    }
}

export {
    PlayerManager
}

export default PlayerManager
