<template>
	<view class="page">
		<NavBar title="安为智消云" title-mode="left" :border="false">
			<view class="nav-brand">
				<image class="nav-logo" src="/static/logo.png" mode="aspectFit" />
				<text class="nav-title">安为智消云</text>
			</view>
		</NavBar>

		<view class="header">
			<text class="header-company">{{ companyName }}</text>
			<text class="header-user">{{ userName }}</text>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">今日警情</text>
			<text class="text-action" @click="goAlarmList('today')">查看全部</text>
		</view>
		<view class="card alarm-overview-card">
			<view class="kpis">
				<view class="kpi alert" @click="goAlarmList('today')">
					<text class="kpi-num">{{ stats.todayTotal }}</text>
					<text class="kpi-label">今日告警</text>
				</view>
				<view class="kpi alert" @click="goAlarmList('fire')">
					<text class="kpi-num">{{ stats.fire }}</text>
					<text class="kpi-label">火警</text>
				</view>
				<view class="kpi alert" @click="goAlarmList('fault')">
					<text class="kpi-num">{{ stats.fault }}</text>
					<text class="kpi-label">故障</text>
				</view>
				<view class="kpi alert" @click="goAlarmList('false_alarm')">
					<text class="kpi-num">{{ stats.falseAlarm }}</text>
					<text class="kpi-label">误报</text>
				</view>
				<view class="kpi warning" @click="goAlarmList('pending')">
					<text class="kpi-num">{{ stats.pending }}</text>
					<text class="kpi-label">待处理</text>
				</view>
				<view class="kpi info" @click="goAlarmList('handled')">
					<text class="kpi-num">{{ stats.handled }}</text>
					<text class="kpi-label">已处置</text>
				</view>
			</view>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">近 7 日告警趋势</text>
			<text class="text-action" @click="goStats">查看统计</text>
		</view>
		<view class="card chart-card">
			<view class="chart-head">
				<text class="chart-meta">近 7 天 · 单位：条</text>
				<view class="legend-row">
					<view class="lg-item" @click="toggleTrend(0)">
						<view class="lg-dot lg-dot--solid" :class="{ muted: !trendShow[0] }"></view>
						<text class="lg-txt" :class="{ muted: !trendShow[0] }">告警</text>
					</view>
					<view class="lg-item" @click="toggleTrend(1)">
						<view class="lg-dot lg-dot--hollow" :class="{ muted: !trendShow[1] }"></view>
						<text class="lg-txt" :class="{ muted: !trendShow[1] }">已处置</text>
					</view>
				</view>
			</view>
			<view class="chart-wrap">
				<qiun-data-charts
					type="area"
					:chartData="trendChartData"
					:opts="trendOpts"
					canvasId="homeTrendChart"
					canvas2d
					ontouch
					:animation="true"
					:errorShow="false"
					background="rgba(0,0,0,0)"
				/>
			</view>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">实时告警</text>
			<text class="text-action" @click="goAlarmList('all')">全部告警</text>
		</view>
		<view class="card alarm-card" v-for="a in alarmList" :key="a.id" @click="goAlarmDetail(a.id)">
			<view class="card-head">
				<text class="card-title">{{ a.title }}</text>
				<text class="status" :class="a.statusClass">{{ a.statusText }}</text>
			</view>
			<text class="alarm-number">告警编号：{{ a.alarmNo }}</text>
			<text class="card-sub">{{ a.subText }}</text>
			<text class="tiny">告警时间：{{ a.timeText }}</text>
		</view>
		<view v-if="!alarmList.length && !loading" class="empty">暂无告警</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">消防运行</text>
		</view>
		<view class="entry-grid">
			<view class="entry" v-for="e in todoList" :key="e.key" @click="onTodo(e)">
				<view class="entry-icon">
					<uni-icons :type="e.icon" size="24" color="#3d5bd9"></uni-icons>
				</view>
				<text class="entry-title">{{ e.title }}</text>
				<text class="entry-desc">{{ e.desc }}</text>
			</view>
		</view>
		<TabBar current="home" />
	</view>
</template>

<script>
import { get, post } from '@/utils/request'
import { getDutyRoomCameras } from '@/utils/videoApi.js'
import TabBar from '@/components/TabBar.vue'
import NavBar from '@/components/NavBar/NavBar.vue'
import qiunDataCharts from '@/components/qiun-data-charts/qiun-data-charts.vue'
import { toCard } from '@/utils/card'

const THEME = '#1e39b1'
const THEME_LIGHT = '#7b93e8'

export default {
	components: { TabBar, NavBar, qiunDataCharts },
	data() {
		return {
      loading: false,
      timer: null,
      companyName: '',
      userName: '',
      stats: { todayTotal: 0, fire: 0, fault: 0, falseAlarm: 0, pending: 0, handled: 0 },
			alarmList: [],
			todoList: [
				{ key: 'V01', icon: 'videocam-filled', title: '消控室监控', desc: '加载中...' },
				{ key: 'D01', icon: 'camera-filled', title: '设备总览', desc: '加载中...' },
				{ key: 'E01', icon: 'home-filled', title: '值班管理', desc: '加载中...' },
				{ key: 'F01', icon: 'navigate-filled', title: '巡检任务', desc: '加载中...' },
				{ key: 'I01', icon: 'flag-filled', title: '隐患管理', desc: '加载中...' },
				{ key: 'R01', icon: 'gear-filled', title: '维修工单', desc: '加载中...' }
			],
			trendCategories: [],
			trendSeries: [[], []],
			trendShow: [true, true]
		}
	},
	computed: {
		trendChartData() {
			const series = []
			if (this.trendShow[0]) {
				series.push({ name: '告警', data: this.trendSeries[0] })
			}
			if (this.trendShow[1]) {
				series.push({ name: '已处置', data: this.trendSeries[1] })
			}
			return { categories: this.trendCategories, series }
		},
		trendOpts() {
			return {
				color: [THEME, THEME_LIGHT],
				padding: [15, 15, 0, 15],
				dataLabel: false,
				dataPointShape: false,
				xAxis: {
					disableGrid: true,
					boundaryGap: 'center',
					fontColor: '#999999',
					fontSize: 10
				},
				yAxis: {
					data: [{ min: 0 }],
					gridType: 'dash',
					dashLength: 3,
					disableGrid: false,
					splitNumber: 4,
					gridColor: '#eeeeee',
					fontColor: '#bbbbbb',
					fontSize: 10
				},
				legend: { show: false },
				extra: {
					area: {
						type: 'curve',
						opacity: 0.2,
						addLine: true,
						width: 2,
						gradient: false,
						activeType: 'none'
					}
				}
			}
		}
	},
  onLoad() {
    if (!uni.getStorageSync('token')) {
      uni.reLaunch({ url: '/pages/login/index' })
      return
    }
    this.fetchProfile()
	},
  onShow() {
    if (this.timer) clearInterval(this.timer)
    // 未登录时不启动轮询：避免泄漏的定时器在登录页被反复 401 踢出
    if (!uni.getStorageSync('token')) return
    this.pullData()
    this.timer = setInterval(() => this.fetchAlerts(), 30000)
  },
  onHide() {
    clearInterval(this.timer)
  },
  // reLaunch 关闭页面只触发 onUnload 不触发 onHide，必须在此兜底清理定时器
  onUnload() {
    clearInterval(this.timer)
  },
	onPullDownRefresh() {
		this.pullData().finally(() => uni.stopPullDownRefresh())
	},
	methods: {
		async fetchProfile() {
			try {
				// 优先从缓存读取，避免首屏闪烁
				const cachedName = uni.getStorageSync('userName')
				const cachedCompany = uni.getStorageSync('companyName')
				if (cachedName) this.userName = cachedName
				if (cachedCompany) this.companyName = cachedCompany

				const data = await get('/system/user/profile/get')
				if (data.nickname) {
					this.userName = data.nickname
					uni.setStorageSync('userName', data.nickname)
				}
				if (data.tenantName) {
					this.companyName = data.tenantName
					uni.setStorageSync('companyName', data.tenantName)
				}  else {
					// 则显示未知租户名称
					this.companyName = '未知租户名称'
					uni.setStorageSync('companyName', '未知租户名称')
				}
			} catch (e) {}
		},
		async pullData() {
      this.loading = true
			try {
				const d = await get('/eiot/firewatch/dashboard/statistics')
				if (!d) return
				const ta = d.todayAlarm || {}
				const m = {}
				if (d.alarmTypeDistribution?.length) {
					d.alarmTypeDistribution.forEach(x => m[x.alarmType] = x.count)
				}
				this.stats = {
					todayTotal: ta.totalAlarms || 0,
					fire: m.fire || 0,
					fault: m.fault || 0,
					falseAlarm: m.false_alarm || 0,
					pending: ta.pendingCount || 0,
					handled: ta.handledCount || (ta.totalAlarms || 0) - (ta.pendingCount || 0)
				}

				// 接口已返回近 7 日真实趋势（后端补零对齐），直接映射，不再用假数据
				const trend = d.alarmTrend || []
				if (trend.length) {
					this.trendCategories = trend.map(t => (t.date || '').slice(5).replace('-', '/'))
					this.trendSeries = [
						trend.map(t => t.alarmCount || 0),
						trend.map(t => t.handleCount || 0)
					]
				}

				// 企业待办描述接真实数据：statistics 响应已含设备/巡检/隐患汇总，无需额外请求
				const ds = d.deviceStatus || {}
				const ps = d.patrolSummary || {}
				const eD = this.todoList.find(t => t.key === 'D01')
				if (eD) eD.desc = `${ds.online || 0} 台在线，${ds.fault || 0} 台故障`
				const eF = this.todoList.find(t => t.key === 'F01')
				if (eF) eF.desc = `进行中/待执行 ${ps.pending || 0} 个`
				const eI = this.todoList.find(t => t.key === 'I01')
				if (eI) eI.desc = `待整改 ${d.pendingIssues || 0} 个`
			} catch (e) {} finally {
				this.loading = false
				this.fetchAlerts()
				this.fetchDutyTodo()
				this.fetchMaintenanceTodo()
				this.fetchVideoTodo()
			}
		},

		// 待办“值班管理”描述接真实排班：/current-duty 返回今日 scheduled|on_duty 班次（后端已按租户物业过滤）
		async fetchDutyTodo() {
			try {
				const list = (await get('/eiot/firewatch/duty-schedule/current-duty')) || []
				const myId = uni.getStorageSync('userId')
				const mine = list.filter(x => String(x.dutyUserId) === String(myId))
				const onDuty = mine.filter(x => x.status === 'on_duty').length
				const e = this.todoList.find(t => t.key === 'E01')
				if (!e) return
				e.desc = onDuty ? `本人值班中 ${onDuty} 班`
					: mine.length ? `本人待签到 ${mine.length} 班`
					: list.length ? `企业今日 ${list.length} 班` : '今日无排班'
			} catch (err) {}
		},
	// 维修待办描述：聚合接口取待处理维修工单数（维保提醒入口已移至工作台，不在首页展示）
		async fetchMaintenanceTodo() {
			try {
				const d = await get('/eiot/firewatch/maintenance/dashboard')
				if (!d) return
				const eR = this.todoList.find(t => t.key === 'R01')
				if (eR) eR.desc = `待处理工单 ${d.pendingOrders || 0} 个`
			} catch (err) {}
		},

		// 消控室监控入口描述：按"值班室"关键词取本租户摄像头在线情况
		async fetchVideoTodo() {
			try {
				const d = await getDutyRoomCameras()
				const list = d.list || []
				const e = this.todoList.find(t => t.key === 'V01')
				if (!e) return
				if (!list.length) {
					e.desc = '未配置'
					return
				}
				const online = list.filter(c => c.status === 1).length
				e.desc = online === list.length ? `${list.length} 路在线` : `${online}/${list.length} 路在线`
			} catch (err) {}
		},

		onTodo(e) {
			// 消控室监控：直达视频页（只展示本租户的值班室摄像头）
			if (e.key === 'V01') {
				uni.navigateTo({ url: '/pages/video-monitor/index' })
				return
			}
			// 设备总览：按当前登录用户所属租户展示设备
			if (e.key === 'D01') {
				uni.navigateTo({ url: '/pages/profile/device-list' })
				return
			}
			if (e.key === 'E01') {
				uni.navigateTo({ url: '/pages/duty/index' })
				return
			}
			uni.showToast({ title: `${e.title}（待接入）`, icon: 'none' })
		},

		async fetchAlerts() {
			// 无 token 时跳过请求：防止残留轮询在登录页触发 401 后被反复踢回
			if (!uni.getStorageSync('token')) return
			try {
				const d = await post('/alert/selectAlertRecordPage', { pageNo: 1, pageSize: 5 })
				this.alarmList = (d.list || []).map(item => toCard(item))
			} catch (e) {}
		},
		goAlarmList(filter) {
			uni.navigateTo({ url: `/pages/alarm-center/index?filter=${filter}` })
		},
		goAlarmDetail(id) {
			uni.navigateTo({ url: `/pages/alarm-detail/index?id=${id}` })
		},
		toggleTrend(idx) {
			this.trendShow[idx] = !this.trendShow[idx]
		},
		goStats() {
			uni.navigateTo({ url: '/pages/stats/index' })
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/card.scss';

.page {
	min-height: 100vh;
	background: #f2f3f5;
	padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.nav-brand { display: flex; align-items: center; gap: 12rpx; margin-left: -24rpx; }
.nav-logo { width: 52rpx; height: 52rpx; }
.nav-title { font-size: 38rpx; font-weight: 700; color: #1a1a1a; }

.header {
	background:
		radial-gradient(ellipse at 80% 10%, rgba(255,255,255,.10), transparent 55%),
		linear-gradient(160deg, #3d5bd9, #1e39b1 55%, #132a7e);
	padding: calc(var(--status-bar-height, 20px) + 54px) 32rpx 44rpx;
	min-height: 180rpx;
	border-radius: 0 0 32rpx 32rpx;
	display: flex;
	flex-direction: column;
	justify-content: center;
	gap: 16rpx;
}
.header-company { color: #fff; font-size: 38rpx; font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-top: 18px; }
.header-user { color: rgba(255,255,255,.8); font-size: 32rpx; }


.card-hd {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 28rpx;
}
.card-hd-left { display: flex; align-items: center; gap: 8rpx; }
.card-tt { font-size: 28rpx; font-weight: 600; color: #333; }
.card-more { font-size: 24rpx; color: #999; }

.section-title {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 72rpx;
	margin-bottom: 16rpx;
}
.section-title-standalone {
	margin: 24rpx 24rpx 0;
}
.section-title-text {
	font-size: 32rpx;
	font-weight: 700;
	color: #1a1a1a;
	padding-left: 20rpx;
	border-left: 8rpx solid #3d5bd9;
	line-height: 1;
}
.text-action {
	font-size: 24rpx;
	color: #3d5bd9;
}
.kpis {
	display: grid;
	grid-template-columns: repeat(3, 1fr);
	gap: 10px;
}
.alarm-overview-card {
	background: transparent;
	box-shadow: none;
	padding: 0 24rpx;
}
.kpi {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 24rpx 0;
	border-radius: 16rpx;
	background: #fff;
	box-shadow: 0 2rpx 8rpx rgba(0,0,0,.06);
}
.kpi-num {
	font-size: 40rpx;
	font-weight: 700;
	line-height: 1.2;
}
.kpi-label {
	font-size: 22rpx;
	margin-top: 6rpx;
}
.kpi.alert .kpi-num { color: #e53935; }
.kpi.alert .kpi-label { color: #666; }
.kpi.warning {
	background: #fff;
}
.kpi.warning .kpi-num { color: #f57c00; }
.kpi.warning .kpi-label { color: #666; }
.kpi.info {
	background: #fff;
}
.kpi.info .kpi-num { color: #1565c0; }
.kpi.info .kpi-label { color: #666; }

.empty { text-align: center; padding: 60rpx 0; color: #ccc; font-size: 28rpx; }

.chart-card {
	padding: 28rpx 28rpx 16rpx;
}
.chart-head {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16rpx;
}
.chart-meta {
	font-size: 22rpx;
	color: #999;
}
.legend-row {
	display: flex;
	gap: 20rpx;
}
.lg-item {
	display: flex;
	align-items: center;
	gap: 8rpx;
}
.lg-dot {
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
}
.lg-dot--solid {
	background: #1e39b1;
}
.lg-dot--hollow {
	background: transparent;
	border: 2rpx solid #7b93e8;
}
.lg-dot.muted {
	opacity: 0.3;
}
.lg-txt {
	font-size: 22rpx;
	color: #666;
}
.lg-txt.muted {
	color: #ccc;
}
.chart-wrap {
	width: 100%;
	height: 420rpx;
}

.entry-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 20rpx;
	padding: 0 24rpx;
}
.entry {
	background: #fff;
	border-radius: 16rpx;
	padding: 28rpx 24rpx;
	box-shadow: 0 1rpx 8rpx rgba(0,0,0,.04);
	display: flex;
	flex-direction: column;
	align-items: flex-start;
	gap: 12rpx;
}
.entry-icon {
	width: 56rpx;
	height: 56rpx;
	border-radius: 12rpx;
	background: #e8ecfa;
	color: #3d5bd9;
	font-size: 28rpx;
	font-weight: 700;
	display: flex;
	align-items: center;
	justify-content: center;
}
.entry-title {
	font-size: 28rpx;
	font-weight: 600;
	color: #1a1a1a;
}
.entry-desc {
	font-size: 22rpx;
	color: #999;
}
</style>
