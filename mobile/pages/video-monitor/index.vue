<template>
	<view class="page">
		<NavBar title="消控室监控" title-mode="center" :back="true" :border="false">
			<template #right>
				<view class="nav-refresh" @click="refreshAll">
					<text>↻</text>
				</view>
			</template>
		</NavBar>

		<!-- 主播放区：16:9 单路画面，左上 LIVE 角标，异常态用遮罩承接 -->
		<view class="player-card">
			<view class="player-wrap">
				<video
					v-if="currentUrl"
					id="dutyVideo"
					class="player"
					:src="currentUrl"
					:autoplay="true"
					:muted="true"
					:controls="true"
					:show-center-play-btn="false"
					:enable-progress-gesture="false"
					object-fit="contain"
					is-live
					@play="onVideoPlay"
					@waiting="onVideoWaiting"
					@timeupdate="onVideoTimeUpdate"
					@error="onVideoError"
				/>
				<view v-else class="player-empty">
					<text>{{ playerEmptyText }}</text>
				</view>

				<view v-if="loading" class="player-mask">
					<text class="mask-text">正在连接视频流...</text>
				</view>
				<view v-else-if="errorMsg" class="player-mask" @click="retry">
					<text class="mask-text">{{ errorMsg }}</text>
					<text class="mask-btn">点击重试</text>
				</view>

				<view v-if="currentUrl && !errorMsg" class="live-badge">
					<view class="live-dot"></view>
					<text class="live-text">LIVE</text>
				</view>
			</view>

			<view class="player-info" v-if="currentCam">
				<text class="info-name">{{ currentCam.channelName }}</text>
				<view class="info-status">
					<view class="status-dot" :class="currentCam.status === 1 ? 'is-on' : 'is-off'"></view>
					<text class="info-status-text">{{ currentCam.status === 1 ? '在线' : '离线' }}</text>
				</view>
			</view>
		</view>

		<!-- 视角切换：点击在线卡片原地换源，离线卡片不可点 -->
		<view class="section-title" v-if="cameras.length">
			<text class="section-title-text">切换视角</text>
			<text class="section-count">{{ cameras.length }} 路</text>
		</view>
		<view class="cam-grid">
			<view
				v-for="c in cameras"
				:key="c.id"
				class="cam-card"
				:class="{ active: isCurrent(c), offline: c.status !== 1 }"
				@click="switchCam(c)"
			>
				<text class="cam-card-name">{{ c.channelName }}</text>
				<view class="cam-card-status">
					<view class="status-dot" :class="c.status === 1 ? 'is-on' : 'is-off'"></view>
					<text class="cam-card-status-text" :class="{ 'is-playing': isCurrent(c) }">
						{{ isCurrent(c) ? '播放中' : c.status === 1 ? '在线' : '离线' }}
					</text>
				</view>
			</view>
		</view>

		<view v-if="!cameras.length && !pageLoading" class="empty">
			<text>未配置消控室监控摄像头</text>
		</view>
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'
import { getDutyRoomCameras, getCameraLiveUrl } from '@/utils/videoApi.js'

// video 拉流偶发静默失败（既不起播也不回调 error）的兜底超时，超时后提示手动重试
const PLAY_TIMEOUT = 10000
// iOS 微信播直播 HLS 有个已知毛病：画面明明在正常播，原生 loading 圈却会一直转不消失，
// 原因是"播放恢复"的通知丢了、原生 UI 收不回去。处置：waiting 后观察 10s，确认画面心跳还在
// 就做一次 pause→play 强制复位播放器状态，让原生 UI 把转圈收掉
const BUFFER_WATCHDOG = 10000
// 自愈的最小间隔：复位动作本身可能引出新一轮 waiting，60s 冷却防止来回折腾
const HEAL_COOLDOWN = 60000

export default {
	components: { NavBar },
	data() {
		return {
			cameras: [],
			currentCam: null,
			currentUrl: '',
			loading: false,
			errorMsg: '',
			pageLoading: false,
			// 是否已自动重试过一次：@error 自动重拉仅做一次，防止死循环
			retried: false,
			timeoutTimer: null,
			// —— 原生转圈自愈相关（背景见文件头 BUFFER_WATCHDOG 注释）——
			waitingAt: 0, // 进入缓冲的时间戳（0 = 本轮缓冲已收尾）
			lastTimeupdate: 0, // 最近一次画面心跳时间戳，判断"其实在播"的依据
			watchdogTimer: null, // 缓冲看门狗：waiting 后盯 10s
			healTimer: null, // 自愈复位的延时与校验定时器
			healing: false, // 复位进行中，防重入
			lastHealAt: 0 // 上次复位时间，冷却用
		}
	},
	computed: {
		playerEmptyText() {
			if (this.pageLoading) return '加载中...'
			if (!this.cameras.length) return '暂无画面'
			if (!this.cameras.some(c => c.status === 1)) return '所有视角均离线'
			return '正在连接视频流...'
		}
	},
	onLoad() {
		// 每次进入页面都重新拉取列表并重新取流（token 一次性且短时效，不能复用）
		this.refreshAll()
	},
	onUnload() {
		this.clearTimer()
		this.clearBufferWatch()
	},
	methods: {
		isCurrent(cam) {
			return !!(this.currentCam && cam.id === this.currentCam.id)
		},
		clearTimer() {
			if (this.timeoutTimer) {
				clearTimeout(this.timeoutTimer)
				this.timeoutTimer = null
			}
		},
		/** 清掉缓冲观察的全部状态：收到 play 或重连，都意味着旧的缓冲轮次翻篇了 */
		clearBufferWatch() {
			if (this.watchdogTimer) {
				clearTimeout(this.watchdogTimer)
				this.watchdogTimer = null
			}
			if (this.healTimer) {
				clearTimeout(this.healTimer)
				this.healTimer = null
			}
			this.waitingAt = 0
			this.lastTimeupdate = 0
			this.healing = false
		},
		/** 拉摄像头列表 + 重播：当前视角仍在线则保持，否则切第一个在线的 */
		async refreshAll() {
			this.pageLoading = true
			try {
				const d = await getDutyRoomCameras()
				this.cameras = d.list || []
			} catch (e) {} finally {
				this.pageLoading = false
			}
			if (!this.cameras.length) {
				this.stopPlay()
				this.currentCam = null
				return
			}
			const kept = this.cameras.find(c => this.isCurrent(c) && c.status === 1)
			const target = kept || this.cameras.find(c => c.status === 1)
			if (target) {
				this.playCamera(target)
			} else {
				this.stopPlay()
			}
		},
		/** 停止播放并释放画面（全部离线或列表为空时） */
		stopPlay() {
			this.clearTimer()
			this.clearBufferWatch()
			this.loading = false
			this.errorMsg = ''
			this.currentUrl = ''
		},
		/** 播放指定视角：每次都重新取带 token 的流地址（token 一次性，禁止缓存复用） */
		async playCamera(cam, isAutoRetry = false) {
			this.currentCam = cam
			this.loading = true
			this.errorMsg = ''
			if (!isAutoRetry) this.retried = false
			this.clearTimer()
			this.clearBufferWatch()
			try {
				const res = await getCameraLiveUrl(cam.id)
				const url = res && res.url
				if (!url) throw new Error('未获取到视频流地址')
				this.currentUrl = url
				this.timeoutTimer = setTimeout(() => {
					if (this.loading) {
						this.loading = false
						this.errorMsg = '视频加载超时，请重试'
					}
				}, PLAY_TIMEOUT)
			} catch (e) {
				this.loading = false
				this.errorMsg = e.message || '获取视频流失败'
			}
		},
		switchCam(cam) {
			if (cam.status !== 1) {
				uni.showToast({ title: '该视角离线', icon: 'none' })
				return
			}
			// 点当前视角且画面正常：不重连；异常态下点击等同重试
			if (this.isCurrent(cam) && this.currentUrl && !this.errorMsg) return
			this.playCamera(cam)
		},
		retry() {
			if (this.currentCam) this.playCamera(this.currentCam)
		},
		onVideoPlay() {
			// 起播/恢复成功：清加载态、错误态、超时兜底与缓冲观察
			// （能收到 play 说明播放器状态机是活的，本轮缓冲观察收尾，自愈不再介入）
			this.loading = false
			this.errorMsg = ''
			this.clearTimer()
			this.clearBufferWatch()
		},
		onVideoWaiting() {
			// 原生转圈从这次 waiting 开始；交给看门狗盯 10s，期间 waiting 抖动不重启窗口
			this.waitingAt = Date.now()
			if (this.watchdogTimer) return
			this.watchdogTimer = setTimeout(() => {
				this.watchdogTimer = null
				this.tryHealSpinner()
			}, BUFFER_WATCHDOG)
		},
		onVideoTimeUpdate() {
			// 画面心跳：waiting 之后仍在持续触发，说明流其实是通的
			this.lastTimeupdate = Date.now()
		},
		/** 缓冲自愈：画面在动却没有等到恢复通知 → 判定为原生转圈残留，pause→play 强制复位 */
		tryHealSpinner() {
			const waitingAt = this.waitingAt
			if (!waitingAt || !this.currentUrl || this.loading || this.errorMsg || this.healing) return
			if (Date.now() - this.lastHealAt < HEAL_COOLDOWN) return
			// 只有"waiting 之后画面还有心跳"才算其实在播；真卡顿不碰，留给自动重试
			if (this.lastTimeupdate - waitingAt < 1500) return
			this.healing = true
			this.waitingAt = 0
			this.lastHealAt = Date.now()
			const ctx = uni.createVideoContext('dutyVideo')
			ctx.pause()
			// pause 和 play 分两拍发：让播放器走一次完整的"停→播"，原生 UI 才会跟着复位
			this.healTimer = setTimeout(() => {
				this.healTimer = null
				ctx.play()
				// 复位后 3s 没等到 play 事件，说明叫不醒，走整路重连兜底
				this.healTimer = setTimeout(() => {
					this.healTimer = null
					if (this.healing) {
						this.healing = false
						this.playCamera(this.currentCam, true)
					}
				}, 3000)
			}, 400)
		},
		onVideoError() {
			// 先清缓冲观察，避免自愈的 pause/play 跟重连流程互相干扰
			this.clearBufferWatch()
			// 流中断后旧 token 已失效，必须重拉新地址；自动重试一次，仍失败转手动重试
			if (!this.retried && this.currentCam) {
				this.retried = true
				this.playCamera(this.currentCam, true)
				return
			}
			this.loading = false
			this.errorMsg = '视频连接失败'
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	background: #f2f3f5;
	padding-top: calc(var(--status-bar-height, 20px) + 44px + 20px);
	padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 40rpx);
	box-sizing: border-box;
}

.nav-refresh {
	padding: 10rpx 16rpx;
	text { font-size: 32rpx; color: #1a1a1a; }
}

/* ══════════ 主播放区 ══════════ */
.player-card {
	margin: 24rpx;
	background: #fff;
	border-radius: 24rpx;
	overflow: hidden;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);
}

.player-wrap {
	position: relative;
	width: 100%;
	height: 395rpx;
	background: #0b0f1a;
}

.player {
	width: 100%;
	height: 100%;
}

.player-empty {
	position: absolute;
	inset: 0;
	display: flex;
	align-items: center;
	justify-content: center;

	text {
		color: rgba(255, 255, 255, 0.45);
		font-size: 26rpx;
	}
}

.player-mask {
	position: absolute;
	inset: 0;
	background: rgba(11, 15, 26, 0.62);
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: 18rpx;
}

.mask-text {
	color: #fff;
	font-size: 26rpx;
}

.mask-btn {
	color: #7b93e8;
	font-size: 26rpx;
	font-weight: 600;
	padding: 8rpx 24rpx;
}

.live-badge {
	position: absolute;
	top: 16rpx;
	left: 16rpx;
	display: flex;
	align-items: center;
	gap: 8rpx;
	background: rgba(0, 0, 0, 0.45);
	border-radius: 999rpx;
	padding: 6rpx 18rpx;
}

.live-dot {
	width: 12rpx;
	height: 12rpx;
	border-radius: 50%;
	background: #ff4d4f;
	animation: livePulse 1.6s ease-in-out infinite;
}

@keyframes livePulse {
	0%, 100% { opacity: 1; }
	50% { opacity: 0.3; }
}

.live-text {
	color: #fff;
	font-size: 20rpx;
	letter-spacing: 2rpx;
}

.player-info {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 22rpx 28rpx;
}

.info-name {
	font-size: 30rpx;
	font-weight: 600;
	color: #1a1a1a;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	flex: 1;
	min-width: 0;
}

.info-status {
	display: flex;
	align-items: center;
	gap: 8rpx;
	flex-shrink: 0;
	margin-left: 16rpx;
}

.info-status-text {
	font-size: 24rpx;
	color: #666;
}

.status-dot {
	width: 14rpx;
	height: 14rpx;
	border-radius: 50%;
	flex-shrink: 0;

	&.is-on { background: #4caf50; }
	&.is-off { background: #9e9e9e; }
}

/* ══════════ 视角切换 ══════════ */
.section-title {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 64rpx;
	margin: 8rpx 24rpx 16rpx;
}

.section-title-text {
	font-size: 32rpx;
	font-weight: 700;
	color: #1a1a1a;
	padding-left: 20rpx;
	border-left: 8rpx solid #3d5bd9;
	line-height: 1;
}

.section-count {
	font-size: 24rpx;
	color: #999;
}

.cam-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 20rpx;
	padding: 0 24rpx;
}

.cam-card {
	position: relative;
	background: #fff;
	border: 2rpx solid transparent;
	border-radius: 20rpx;
	padding: 24rpx;
	box-sizing: border-box;
	box-shadow: 0 1rpx 8rpx rgba(0, 0, 0, 0.03);

	&.active {
		border-color: #3d5bd9;
		background: #eef2ff;
	}

	&.offline {
		opacity: 0.55;
	}
}

.cam-card-name {
	display: block;
	font-size: 28rpx;
	font-weight: 600;
	color: #1a1a1a;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.cam-card-status {
	display: flex;
	align-items: center;
	gap: 8rpx;
	margin-top: 14rpx;
}

.cam-card-status-text {
	font-size: 24rpx;
	color: #666;

	&.is-playing {
		color: #3d5bd9;
		font-weight: 600;
	}
}

.empty {
	text-align: center;
	padding: 80rpx 0;
	color: #ccc;
	font-size: 28rpx;
}
</style>
