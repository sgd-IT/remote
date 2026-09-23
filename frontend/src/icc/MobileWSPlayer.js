import API from "./api.js";
// import WSPlayer from '../WSPlayer/WSPlayer'

// 移动端蒙层
function mobileLayer(el) {
    return `
        <div class="wsplayer-mobile-layer-container">
            <!-- 顶部放置按钮 -->
            <div class="wsplayer-control-btns">
                <div style="color: #fff; font-size: 20px; margin-right: 20px;" class="mobile-stream-text"></div>
                <div class="mobile-opt-icon" id="mobile-pantilt-control" title="云台"><div class="mobile-show-pantilt-icon off"></div></div>
                <div class="mobile-opt-icon" id="mobile-speed-control" title="倍速"><div class="mobile-speed-control-icon"></div></div>
                <div class="mobile-opt-icon" id="mobile-refresh" title="刷新"><div class="mobile-refresh-replay-icon"></div></div>
                <div class="mobile-opt-icon" id="mobile-volume" title="音量"><div class="mobile-audio-icon off"></div></div>
                <div class="mobile-opt-icon" id="mobile-ivs" title="智能帧"><div class="mobile-draw-triangle-icon off"></div></div>
                <div class="mobile-opt-icon" id="mobile-talk" title="对讲"><div class="mobile-talk-icon off"></div></div>
            </div>
            <!--倍速-->
            <div class="wsplayer-mobile-speed-container" style="display: none;">
                <div>0.25X</div>
                <div>0.5X</div>
                <div>0.75X</div>
                <div style="color: #63beff;">1X</div>
                <div>1.5X</div>
                <div>2X</div>
                <div>4X</div>
            </div>
            <!-- 录像暂停，继续 -->
            <!-- <div class="mobile-wsplayer-pause-play-container">
                // <div class="mobile-wsplayer-back-10" id="mobile-pause-icon" title="后退10秒"></div>
                <div class="center-play-icon" id="mobile-pause-play-btn" style="display:none;" title="继续"></div>
                <div class="center-pause-icon" id="mobile-pause-pause-btn" title="暂停"></div>
                // <div class="mobile-wsplayer-round-10" id="mobile-pause-icon" title="前进10秒"></div>
            </div> -->
            <!-- 云台 -->
            <div id="${el}-mobile-pantilt" class="wsplayer-mobile-pantilt default-bg" style="display: none;"></div>
            <!-- 进度条 -->
            <div class="bottom-controller-container">
                <canvas id="wsplayer-mobile-record-progress" height="60" class="ws-record-area"></canvas>
            </div>
        </div>
    `
}

// 开启音频
// 苹果手机播放音频需要在点击事件中手动开启
const __resumeAudio = function() {
    if(!window.wsAudioPlayer) {
        let intervalId = setInterval(() => {
            if(window.wsAudioPlayer) {
                window.wsAudioPlayer.manualResume('fromTalk')
                clearInterval(intervalId);
            }
        }, 100)
    } else {
        window.wsAudioPlayer.manualResume('fromTalk')
    }
}


// 节流函数：在触发事件后，delay时间内无法重复触发
function throttle(fn, delay) {
    let timeoutId;
    return function() {
        if(!timeoutId) {
            fn.apply(this, arguments);
            timeoutId = setTimeout(() => {
                timeoutId = 0;
            }, delay)
        }
    }
}


class MobileWSPlayer {
    constructor(opt) {
        // 播放器所在的容器ID
        this.el = opt.el;
        this.$mobileEl = document.querySelector(`#${this.el}`);
        this.initialWSPlayer = false
        this.player = null;
        this.mobilePanTilt = null;
        this.recordProgress = null;
        // 实时预览还是录像回放播放器
        this.type = opt.type || 'real';
        // 显示的窗口的数量
        this.showNum = opt.num;
        // 当前选中的窗口的索引
        this.playIndex = 0;
        // 当前选中窗口正在播放视频的通道
        this.currentChannelId = "";
        // 索引对应窗口，保存当日录像信息
        this.recordList = [];
        this.showLayer = true
        this.isFullScreenFlag = false
        this.currentWindowPlaying = false
        this.receiveMessageFromWSPlayer = opt.receiveMessageFromWSPlayer
        this.playType = ''
        this.initPlayer(opt)
        this.currentSpeed = 1
        this.currentSelectIndex = 0 // 当前选中的窗口序列号
        this.streamTypeText = {
            1: '主码流',
            2: '辅码流1',
            3: '辅码流2'
        }
    }
    
    // 初始化播放器
    initPlayer(opt) {
        if(window.WSPlayer.WSPlayer) {
            window.WSPlayer = window.WSPlayer.WSPlayer
        }
        if(!opt.type) {
            console.warn(`请初始化播放器时传入必传参数：
new PlayerManager({
    type: "real", // real-实时预览 record-录像回放
    ...
})`)
        return
        }
        this.player = new WSPlayer({
            el: opt.el,
            type: opt.type,
            importLoad: opt.importLoad, // 是否动态加载库
            prefixUrl: opt.prefixUrl, // 解码库资源前缀
            protocol: opt.protocol, // 协议 ws  wss
            isIntranet: opt.isIntranet, // 传入当前是 内网还是外网, true-内网 false-外网. 内外网 + 分布式下, 使用该模式
            intranetMap: opt.intranetMap, // 传入当前的内外网关系映射 {"内网ip": "外网ip"}.  内外网 + 分布式下, 使用该模式
            proxyServerIp: opt.proxyServerIp, // V1.2.7 版本新增该配置 代理服务器的ip 【ws/wss直连时不传】
            streamServerIp: opt.streamServerIp, // V1.2.7 版本新增该配置 流媒体服务器的ip
            rtspResponseTimeout: opt.rtspResponseTimeout, // rtsp 拉流超时时间
            setWSUrl: opt.setWSUrl, // 开放式的 传入函数， return websocket 连接地址，第三方自行处理
            config: {
                num: opt.num,
                maxNum: 4,
                showControl: false, // 默认是否显示底部工具栏
                showRecordProgressBar: false, // 底部进度条禁用
                showTopOperate: false, // 是否显示播放顶部操作 V1.3.5 新增
                useNginxProxy: this.useNginxProxy, // V1.2.7 版本新增该配置 直连时需要改为 false, 代理模式为 true
                openIvs: typeof opt.openIvs === 'boolean' ? opt.openIvs : true, // 默认显示智能帧
                draggable: false, // 1.3.0 新增，是否支持拖拽
                useH264MSE: typeof opt.useH264MSE === 'boolean' ? opt.useH264MSE : true,
                useH265MSE: typeof opt.useH265MSE === 'boolean' ? opt.useH265MSE : true,
                localeLang: opt.localeLang || 'zhCN', // zhCN 中文 enUS 英文
                localeI18n: opt.localeI18n, // 1.3.2新增配置
                cacheMode: opt.cacheMode || 1, //1.3.4新增 0-关闭实时流自适应缓冲模式 1-自适应缓冲 2-实时优先 3-流畅优先 4-智能缓冲模式
                nMSEFrameCount: opt.nMSEFrameCount, // MSE硬解码每个包的帧数(用于处理卡顿下，一次给video标签推的帧数，可以有效的防止画面过于卡顿，但是会影响实时性)
                ivsTypeArr: opt.ivsTypeArr || [1, 2], // 传入数组，支持显示的情况, 空表示没有智能信息，1-智能规则线 2-智能目标框
                isWebView: opt.isWebView, // 是否webview引擎 true-是 false-不是
                picCapCb: opt.picCapCb // 是否需要抓图回调
            },
            receiveMessageFromWSPlayer: (method, data, err) => {
                switch(method) {
                    case "initializationCompleted":
                        this.initialWSPlayer = true;
                        this.player.createWSPlayerLayer("mobile-player-layer", (id) => this.layerCallback(id, this.el));
                        if(this.type === 'record') {
                            WSPlayerJQ.$(`#${this.el} .bottom-controller-container`).css('display', 'block')
                            WSPlayerJQ.$(`#${this.el} canvas#wsplayer-mobile-record-progress`).css('display', 'block')
                            WSPlayerJQ.$(`#${this.el} .mobile-stream-text`).css('display', 'none')
                            WSPlayerJQ.$(`#${this.el} #mobile-pantilt-control`).css('display', 'none')
                            WSPlayerJQ.$(`#${this.el} #mobile-speed-control`).css('display', 'block')
                            WSPlayerJQ.$(`#${this.el} #mobile-talk`).css('display', 'none')
                        } else {
                            WSPlayerJQ.$(`#${this.el} .bottom-controller-container`).css('display', 'none')
                            WSPlayerJQ.$(`#${this.el} canvas#wsplayer-mobile-record-progress`).css('display', 'none')
                            WSPlayerJQ.$(`#${this.el} .mobile-stream-text`).css('display', 'block')
                            WSPlayerJQ.$(`#${this.el} #mobile-pantilt-control`).css('display', 'block')
                            WSPlayerJQ.$(`#${this.el} #mobile-speed-control`).css('display', 'none')
                            WSPlayerJQ.$(`#${this.el} #mobile-talk`).css('display', 'block')
                        }
                        opt.receiveMessageFromWSPlayer(method, data, err);
                        break;
                    case "selectWindowChanged": // 窗口数量变化
                        if(this.initialWSPlayer) {
                            this.currentSelectIndex = data.playIndex // 当前选中的窗口序列号
                            this.showMobileLayer(false)
                            this.currentWindowPlaying = false
                            if(data.currentWindowPlaying) {
                                this.currentWindowPlaying = true
                                this.showMobileLayer(true)
                            }
                            opt.receiveMessageFromWSPlayer(method, data, err);
                        }
                        break;
                    case "dblclickWindow":
                        this.showNum = data.showNum
                        if(data.showNum === 1) {
                            // 表示一分屏
                            this.showMobileLayer(true)
                        } else {
                            this.showMobileLayer(false)
                        }
                        opt.receiveMessageFromWSPlayer(method, data, err);
                        break;
                    case "windowNumChanged":
                        this.showNum = data
                        if(this.initialWSPlayer) {
                            if(data === 1) {
                                // 表示一分屏
                                this.showMobileLayer(true)
                            } else {
                                this.showMobileLayer(false)
                            }
                        }
                        opt.receiveMessageFromWSPlayer(method, data, err);
                        break;
                    case "realSuccess":
                        if(data.selectIndex === this.currentSelectIndex) {
                            this.currentWindowPlaying = true
                        }
                        let controller = this.mobilePanTilt.setChannel({
                            id: data.channelInfo.channelId,
                            cameraType: "2", // 1-枪机，2-球机
                        })
                        WSPlayerJQ.$(`#${this.el} #mobile-pantilt-control`).css('display', controller ? 'block' : 'none')
                        opt.receiveMessageFromWSPlayer(method, data, err);
                        break;
                    case "getVideoFrameInfo": // 获取视频帧信息
                        if(this.type === 'record' && data.selectIndex === this.currentSelectIndex) {
                            this.recordProgress.updateProgress({
                                currentTime: data.timeStamp
                            })
                        }
                        opt.receiveMessageFromWSPlayer(method, data, err);
                        break;
                    case "recordSuccess":
                        if(data.selectIndex === this.currentSelectIndex) {
                            this.currentWindowPlaying = true
                            this.recordProgress.updateProgress({
                                records: data.channelInfo.records, // 更新录像进度条录像
                                currentTime: data.channelInfo.records && data.channelInfo.records.length && data.channelInfo.records[0].startTime
                            }
                            )
                        }
                        opt.receiveMessageFromWSPlayer(method, data, err);
                        break;
                    case "closeVideo": // 视频关闭
                        // 点击关闭按钮引发的视频关闭进行提示
                        // 切换视频引发的视频关闭不进行提示
                        if(!data.changeVideoFlag) {
                            console.log(`窗口${data.selectIndex}的视频已关闭`)
                        }
                        if(['api', 'close', 'destroy', 'btnClick', 'chromeMemoryOut', 'playSDKError', 'recordFinish'].includes(data.closeReason)) {
                            this.showMobileLayer(false)
                        }
                        break;
                    default:
                    opt.receiveMessageFromWSPlayer(method, data, err)
                }
            },
            getRealRtsp: API.getRealmonitor, // 获取实时预览rtsp接口 (不要做任何改动, 只传入方法, API.getRealmonitor() 是错误的， 会报错)
            getTalkRtsp: API.getTalkRtsp, // 获取对讲rtsp接口  (不要做任何改动, 只传入方法, API.getTalkRtsp() 是错误的， 会报错)
            stopTalk: API.stopTalk, // 关闭对讲rtsp接口  (不要做任何改动, 只传入方法, API.stopTalk() 是错误的， 会报错)
            getRecords: API.getRecords, // 获取录像列表接口  (不要做任何改动, 只传入方法, API.getRecords() 是错误的， 会报错)
            getRecordRtspByTime: API.getRecordRtspByTime, // 根据时间形式获取录像rtsp接口 (不要做任何改动, 只传入方法, API.getRecordRtspByTime() 是错误的， 会报错)
            getRecordRtspByFile: API.getRecordRtspByFile, // 根据文件形式获取录像rtsp接口 (不要做任何改动, 只传入方法, API.getRecordRtspByFile() 是错误的， 会报错)
            fetchChannelAuthority: API.fetchChannelAuthority, // 获取通道权限接口
        })
    }

    addMobileLayerEvent(el) {
        WSPlayerJQ.$(`#${el}`).on('click', (e) => {
            e.preventDefault()
            if(this.showNum === 1) {
                this.showMobileLayer(this.showLayer)
                this.showLayer = !this.showLayer
            } else {
                this.showMobileLayer(false)
                this.showLayer = false
            }
            
        })
        WSPlayerJQ.$(`#${el} .wsplayer-control-btns`).on("click", (e) => {
            e.preventDefault()
            e.stopPropagation()
        })
        WSPlayerJQ.$(`#${el} .bottom-controller-container`).on("click", (e) => {
            e.preventDefault()
            e.stopPropagation()
        })
    
        // 添加屏幕方向检测
        window.addEventListener('orientationchange', (event) => {
            // 初始检查屏幕方向
            if (window.orientation === 0 || window.orientation === 180) {
                console.log('初始竖屏模式');
                this.showMobileLayer(false);
                if(this.isFullscreen()) {
                    this.setExitFullScreen()
                }
                if(this.isIOS()) {
                    this.isFullScreenFlag = false
                    // window.WSPlayerJQ && window.WSPlayerJQ.$('.play-pause-wrapper').css('display', 'unset')
                }
                this.updatePlayerWindow()
            } else if (window.orientation === 90 || window.orientation === -90) {
                console.log('初始横屏模式');
                this.setFullScreen()
                this.setPlayerAdapter('stretching')
                if(this.isIOS()) {
                    this.showMobileLayer(true);
                    // window.WSPlayerJQ && window.WSPlayerJQ.$('.play-pause-wrapper').css('display', 'none')
                }
                this.isFullScreenFlag = true
                this.updatePlayerWindow()
            }
        });
    
        // 全屏状态变化事件（主要用于安卓场景下，IOS单独处理）
        document.addEventListener('fullscreenchange', this.handleFullscreenChange.call(this));
        document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange.call(this));
        document.addEventListener('mozfullscreenchange', this.handleFullscreenChange.call(this));
    }

    // 权限控制
    setAuthority(params, callback, errorCallBack) {
        if (this.player.fetchChannelAuthority) {
            this.player.fetchChannelAuthority(params).then(res => {
                if (res.data.result) {
                    callback();
                }
            }).catch(err => {
                errorCallBack(err);
            })
        } else {
            callback();
        }
    }

    // 检查是否全屏
    isFullscreen() {
        return document.fullscreenElement || document.webkitFullscreenElement || document.mozFullscreenElement || this.isFullScreenFlag;
    }

    // 是否显示/隐藏图层
    showMobileLayer(showLayer) {
        console.log("showLayer:", showLayer, this.isFullscreen(), this.currentWindowPlaying)
        if(showLayer && this.isFullscreen() && this.currentWindowPlaying) {
            WSPlayerJQ.$('.wsplayer-mobile-layer-container', this.$mobileEl).css("display", "block")
        } else {
            WSPlayerJQ.$('.wsplayer-mobile-layer-container', this.$mobileEl).css("display", "none")
            this.showSpeed(false)
        }
    }

    // 显示/隐藏倍速
    showSpeed(status) {
        if(status) {
            WSPlayerJQ.$(`#${this.el} .wsplayer-mobile-speed-container`).css("display", "flex")
            WSPlayerJQ.$(`#${this.el} .bottom-controller-container`).css("display", "none")
            WSPlayerJQ.$(`#${this.el} .wsplayer-control-btns`).css("display", "none")
        } else {
            WSPlayerJQ.$(`#${this.el} .wsplayer-mobile-speed-container`).css("display", "none")
            WSPlayerJQ.$(`#${this.el} .bottom-controller-container`).css("display", this.type === 'real' ? 'none' : "unset")
            WSPlayerJQ.$(`#${this.el} .wsplayer-control-btns`).css("display", "flex")
        }
    }

    // 全屏状态监测
    handleFullscreenChange() {
        this.showMobileLayer(false);
        this.showLayer = false
        if (document.fullscreenElement) {
            console.log('全屏模式');
            window.WSPlayerJQ && window.WSPlayerJQ.$('.play-pause-wrapper').css('display', 'none')
            this.isFullScreenFlag = true
            if(this.num === 1) {
                this.showMobileLayer(true);
                this.showLayer = true
            }
        } else {
            this.isFullScreenFlag = false
            console.log('非全屏模式');
            window.WSPlayerJQ && window.WSPlayerJQ.$('.play-pause-wrapper').css('display', 'unset')
        }
    }

    layerCallback(id, el) {
        let layerDom = document.querySelector(`#${id}`);
        layerDom.innerHTML = mobileLayer(el)
        this.mobilePanTilt = this.initMobilePanTilt(`${el}-mobile-pantilt`)
        this.recordProgress = this.initRecordProgress()
        this.addMobileLayerEvent(el)
        this.mobileLayerBtnEvent()
        this.showMobileLayer(this.num === 1)
    }

    mobileLayerBtnEvent() {
        // 云台显隐控制
        window.WSPlayerJQ.$('#mobile-pantilt-control', this.$mobileEl).click((e) => {
            if(e.target.getAttribute('class').includes('off')) {
                WSPlayerJQ.$('.mobile-show-pantilt-icon', this.$mobileEl).removeClass('off').addClass('on')
                WSPlayerJQ.$(`#${this.el} .wsplayer-mobile-pantilt`).css("display", "block")
            } else {
                WSPlayerJQ.$('.mobile-show-pantilt-icon', this.$mobileEl).removeClass('on').addClass('off')
                WSPlayerJQ.$(`#${this.el} .wsplayer-mobile-pantilt`).css("display", "none")
            }
        })
        // 倍速
        window.WSPlayerJQ.$('#mobile-speed-control', this.$mobileEl).click((e) => {
            this.showSpeed(true)
        })
        // 刷新重连
        window.WSPlayerJQ.$('#mobile-refresh', this.$mobileEl).click(() =>{
            let playerItem = this.player.playerList[this.currentSelectIndex]
            if(playerItem.playType === 'url') {
                this.player.sendMessage('refreshReplay', {selectIndex: playerItem.options.selectIndex, channelInfo: {...playerItem.options}})
            } else {
                this.player.type === 'real' ? this.player.playRealVideo({
                    channelList: [{...playerItem.options.channelData}],
                    streamType: playerItem.options.streamType,
                    windowIndex: playerItem.index
                }) : this.player.playRecordVideo({
                    channelList: [{...playerItem.options.channelData}],
                    startTime: playerItem.options.startTime,
                    endTime: playerItem.options.endTime,
                    recordSource: playerItem.options.recordSource,
                    streamType: playerItem.options.streamType,
                    recordType: playerItem.options.recordType,
                    windowIndex: playerItem.index,
                    playRecordByFile: playerItem.options.playRecordByFile
                })
            }
        })
        // 声音开关
        window.WSPlayerJQ.$('#mobile-volume', this.$mobileEl).click((e) =>{
            let playerItem = this.player.playerList[this.currentSelectIndex]
            if(!playerItem.currentWindowPlaying) {
                // 表示当前暂未播放视频
                return
            }
            if(this.player.isTalking && this.isTalking && this.gbTalk) {
                // 表示有对讲且是当前对讲，且是国标对讲时
            } else if(this.player.isTalking) {
                // 对讲的时候，无法开启音频
                this.player.sendErrorMessage(this.isTalking ? "301" : "302")
                return;
            }
            if (playerItem.isAudioPlay) {
                // 正在播放，关闭声音
                this.closeVolume();
                window.WSPlayerJQ.$(e.target).removeClass('on').addClass('off')
            } else {
                // 录像回放支持0.25 - 4倍速播放声音，0.125 和 8 倍速不支持音频
                if(this.player.type === 'record' && ![0.25, 0.5, 1, 1.25, 1.5, 2, 4].includes(Number(playerItem.speed))) {
                    this.player.sendErrorMessage("204", {insert: [this.speed]})
                    return;
                }
                // 开启声音
                playerItem.openVolume()
                window.WSPlayerJQ.$(e.target).removeClass('off').addClass('on')
            }
            
        })
        // 显隐智能帧
        window.WSPlayerJQ.$('#mobile-ivs', this.$mobileEl).click((e) => {
            let playerItem = this.player.playerList[this.currentSelectIndex]
            if(!playerItem.currentWindowPlaying) {
                // 表示当前暂未播放视频
                return
            }
            // 设置智能帧按钮状态
            if(e.target.getAttribute('class').includes('off')) {
                window.WSPlayerJQ.$(e.target).removeClass('off').addClass('on')
                this.player.setIvs({
                    showIvs: true,
                    selectIndex: this.player.wsPlayerIndex * 100 + playerItem.options.selectIndex,
                    ivsTypeArr: this.player.config.ivsTypeArr
                })
            } else {
                window.WSPlayerJQ.$(e.target).removeClass('on').addClass('off')
                this.player.setIvs({
                    showIvs: false,
                    selectIndex: this.player.wsPlayerIndex * 100 + playerItem.options.selectIndex,
                    ivsTypeArr: this.player.config.ivsTypeArr
                })
            }
            
        })

        // 对讲
        window.WSPlayerJQ.$('#mobile-talk', this.$mobileEl).click(throttle((e) =>{
            let playerItem = this.player.playerList[this.currentSelectIndex]
            if(!playerItem.currentWindowPlaying) {
                // 表示当前暂未播放视频
                return
            }
            if(location.protocol === 'http:') {
                // http协议不支持对讲
                this.player.sendErrorMessage("305")
                return;
            }
            // 其他窗口有对讲时进行提示
            if (this.player.isTalking && !playerItem.isTalking) {
                // 其他设备对讲中，无法开启对讲
                this.player.sendErrorMessage("303")
            } else if(playerItem.isTalking) {
                // 本窗口在进行对讲，则关闭对讲
                window.WSPlayerJQ.$(e.target).removeClass('on').addClass('off')
                playerItem.stopTalk();
            } else {
                const {selectIndex, channelData} = playerItem.options;
                if(playerItem.options && playerItem.options.playType === 'url') {
                    window.WSPlayerJQ.$(e.target).removeClass('off').addClass('on')
                    this.player.sendMessage('notifyTalk', {selectIndex, channelData: {...channelData}})
                    return
                }
                __resumeAudio();
                // 权限控制
                this.setAuthority({
                    channelCode: playerItem.options.channelData ? playerItem.options.channelData.channelCode : playerItem.options.channelId,
                    function: "3"
                }, () => {
                    // 如果都没有在对讲，则通知业务层获取对讲rtsp
                    this.player.__startTalk(playerItem.options.channelData, playerItem.index);
                    window.WSPlayerJQ.$(e.target).removeClass('off').addClass('on')
                }, err => {
                    if (err.code === 1103) {
                        this.player.sendErrorMessage(401, { selectIndex, channelData: {...channelData}, type: 'talk' });
                    }
                })
            }
        }, 2000))

        // 倍速
        window.WSPlayerJQ.$('.wsplayer-mobile-speed-container div', this.$mobileEl).click((e) => {
            e.preventDefault()
            e.stopPropagation()
            this.selectedSpeed = Number(e.target.innerHTML.replace('X', ''))
            window.WSPlayerJQ.$('.wsplayer-mobile-speed-container div', this.$mobileEl).css('color', '#fff')
            e.target.style.color = '#63beff'
            this.playSpeed(this.selectedSpeed, this.currentSelectIndex)
            this.showSpeed(false)
            this.showLayer = false
            this.showMobileLayer(this.showLayer)
        })

        // 继续播放
        window.WSPlayerJQ.$('#mobile-pause-play-btn', this.$mobileEl).click((e) => {
            e.preventDefault()
            e.stopPropagation()
            window.WSPlayerJQ.$(e.target).css('display', 'none')
            window.WSPlayerJQ.$('#mobile-pause-pause-btn', this.$mobileEl).css('display', 'block')
            this.play(this.currentSelectIndex)
        })
        
        // 暂停播放
        window.WSPlayerJQ.$('#mobile-pause-pause-btn', this.$mobileEl).click((e) => {
            e.preventDefault()
            e.stopPropagation()
            window.WSPlayerJQ.$(e.target).css('display', 'none')
            window.WSPlayerJQ.$('#mobile-pause-play-btn', this.$mobileEl).css('display', 'block')
            this.pause(this.currentSelectIndex)
        })
    }

    // 更新窗口
    updatePlayerWindow() {
        this.player.__updatePlayerWindow()
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
        this.playType = 'playVideo'
        WSPlayerJQ.$(`#${this.el} .mobile-stream-text`).text(this.streamTypeText[Number(opt.streamType || 1)])
        this.player && this.player.playRealVideo({
            channelList: opt.channelList,
            streamType: opt.streamType,
            windowIndex: opt.windowIndex
        })
    }

    playRecordVideo(opt) {
        this.playType = 'playVideo'
        this.recordPlayer && this.recordPlayer.playRecordVideo(opt)
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
        this.playType = 'url'
        WSPlayerJQ.$(`#${this.el} .mobile-stream-text`).text(this.streamTypeText[Number(opt.streamType || 1)])
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
    */
    talkByUrl(opt) {
        this.player && this.player.talkByUrl(opt)
    }

    /**
     * @name 停止流地址对讲
     * @desc 流地址停止对讲
     */
    stopUrlTalk(windowIndex) {
        this.player && this.player.stopUrlTalk(windowIndex)
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
        this.player && this.player.playRecordVideo(opt)
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
     * @param {Boolean} opt.isSeekRecord 是否连贯上一段录像
     */
    recordByUrl(opt) {
        this.playType = 'url'
        this.player && this.player.recordByUrl(opt)
    }

    /**
     * 录像暂停
     * 只有正在播放的录像调用才有效
     * @param { number } index 窗口号
     */
    pause(index) {
        this.player && this.player.pause(index);
    }

    /**
     * 录像暂停后播放
     * 只有暂停后的录像调用才有效
     * @param { number } index 窗口号
     */
    play(index) {
        this.player && this.player.play(index);
    }

    /**
     * 倍速播放
     * @param { number } speed 速率 0.125 0.25 0.5 1 1.25 1.5 2 4 8 共7种速率
     * @param { number } index 窗口号
     */
    playSpeed(speed, index) {
        this.player && this.player.playSpeed(speed, index);
    }

    /**
     * 关闭播放器
     * @param {number} index 可选，关闭指定索引的窗口的播放器，不传则表示关闭所有播放器
     */
    close(index) {
        this.player && this.player.close(index);
    }

    /**
     * 判断是否为IOS设备
     */
    isIOS() {
        return this.player.isIOS()
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
     */
    startLocalRecord(selectIndex, name, size, downloadMp4Record) {
        this.player.startLocalRecord(selectIndex, name, size, downloadMp4Record)
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
        this.player.setIvs(showIvs, selectIndex, ivsType)
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

    initMobilePanTilt(panTiltEl) {
        return this.player.initMobilePanTilt({
            el: panTiltEl,
            setPtzDirection: API.setPtzDirection,
            setPtzCamera: API.setPtzCamera,
            currentPanTiltDirect: this.currentPanTiltDirect.bind(this)
        });
    }

    // 收到当前云台的方向和状态
    currentPanTiltDirect(data) {
        this.receiveMessageFromWSPlayer("mobilePanTiltDirect", data)
    }

    // 初始化进度条
    initRecordProgress() {
        return this.player.initRecordProgress({
            recordProgressId: `#${this.el} canvas#wsplayer-mobile-record-progress`,
            // getCurrentTime: (timeStamp) => {
            //     console.log("当前时间")
            // },
        })
    }
}

export {
    MobileWSPlayer
}

export default MobileWSPlayer