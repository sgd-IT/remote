<template>
	<view class="page">
		<NavBar title="值班首页" title-mode="center" :border="false" />

		<!-- 统计概览 -->
		<view class="stat-row">
			<view class="stat-card" v-for="s in statCards" :key="s.label" @click="onStatClick(s.key)">
				<text class="stat-num" :style="{ color: s.color }">{{ s.value }}</text>
				<text class="stat-label">{{ s.label }}</text>
			</view>
		</view>

		<!-- 筛选栏 -->
		<view class="filter-bar">
			<view class="filter-chips">
				<view class="chip" :class="{ active: curStatus === 'all' }" @click="setStatus('all')">全部</view>
				<view class="chip" :class="{ active: curStatus === s.key }" v-for="s in statusOptions" :key="s.key" @click="setStatus(s.key)">{{ s.label }}</view>
			</view>
		</view>

		<!-- 排班列表 -->
		<scroll-view class="schedule-list" scroll-y @scrolltolower="loadMore">
			<view class="schedule-card" v-for="item in list" :key="item.id">
				<!-- 异常班次水印：缺勤 / 早退 / 无签到 -->
				<view v-if="watermarkText(item)" class="watermark">{{ watermarkText(item) }}</view>
				<view class="card-top">
					<view class="card-title-row">
						<text class="shift-tag" :class="'shift-' + item.shiftType">{{ item.shiftTypeName || shiftLabel(item.shiftType) }}</text>
						<text class="status-tag" :class="'status-' + cardStatus(item)">{{ statusLabel(cardStatus(item)) }}</text>
					</view>
					<text class="card-date">{{ item.scheduleDate }}</text>
				</view>
				<view class="card-body">
					<view class="info-row" v-if="item.propertyName">
						<text class="info-label">所属单位</text>
						<text class="info-val">{{ item.propertyName }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">值班人</text>
						<text class="info-val">{{ item.dutyUserName || '-' }}</text>
					</view>
					<view class="info-row">
						<text class="info-label">时间</text>
						<text class="info-val">{{ dutyTimeText(item) }}</text>
					</view>
					<view class="info-row" v-if="item.dutyUserPhone">
						<text class="info-label">电话</text>
						<text class="info-val">{{ item.dutyUserPhone }}</text>
					</view>
					<view class="info-row" v-if="item.checkInTime">
						<text class="info-label">签到</text>
						<text class="info-val">{{ fmtTime(item.checkInTime) }}</text>
					</view>
					<view class="info-row" v-if="item.checkOutTime">
						<text class="info-label">签退</text>
						<text class="info-val">{{ fmtTime(item.checkOutTime) }}</text>
					</view>
				</view>
				<view class="card-actions" v-if="item.status === 'scheduled' || item.status === 'on_duty'">
					<!-- 异常班次（缺勤/早退）置灰禁用，可用性统一由 cardStatus 推导 -->
					<view v-if="item.status === 'scheduled'" class="action-btn btn-checkin" :class="{ disabled: !canCheckIn(item) }" @click="onCheckIn(item)">签到</view>
					<view v-if="item.status === 'on_duty'" class="action-btn btn-checkout" :class="{ disabled: !canCheckOut(item) }" @click="onCheckOut(item)">签退</view>
				</view>
			</view>
			<view v-if="!list.length && !loading && !loadError" class="empty-tip">暂无排班记录</view>
			<view v-if="!list.length && loadError" class="empty-tip">加载失败，请下拉重试</view>
			<view v-if="loading" class="loading-tip">加载中...</view>
			<view v-if="!loading && list.length && list.length >= total" class="end-tip">没有更多了</view>
		</scroll-view>
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'
import { get, put } from '@/utils/request.js'
import { fmtTime } from '@/utils/card'

export default {
	components: { NavBar },
	data() {
		return {
			statCards: [
				{ key: 'scheduled', label: '已排班', value: 0, color: '#909399' },
				{ key: 'on_duty', label: '值班中', value: 0, color: '#67c23a' },
				{ key: 'completed', label: '已完成', value: 0, color: '#409eff' },
				{ key: 'absent', label: '缺勤', value: 0, color: '#f56c6c' }
			],
			statusOptions: [
				{ key: 'scheduled', label: '已排班' },
				{ key: 'on_duty', label: '值班中' },
				{ key: 'completed', label: '已完成' },
				{ key: 'absent', label: '缺勤' },
				{ key: 'early_leave', label: '早退' }
			],
			curStatus: 'all',
			list: [],
			total: 0,
			pageNo: 1,
			pageSize: 20,
			loading: false,
			loadError: false
		}
	},
	onLoad() {
		this.loadStats()
		this.loadList()
	},
	onPullDownRefresh() {
		this.refresh().finally(() => {
			uni.stopPullDownRefresh()
		})
	},
	methods: {
		fmtTime,
		async loadStats() {
			try {
				const res = await get('/eiot/firewatch/duty-schedule/statistics')
				const s = (res && res.statusStatistics) || {}
				this.statCards.forEach(c => { c.value = s[c.key] || 0 })
			} catch (e) {
				// 统计加载失败不影响列表，失败提示已由 request.js 统一弹出
			}
		},
		async loadList() {
			if (this.loading) return
			this.loading = true
			try {
				const params = {
					pageNo: this.pageNo,
					pageSize: this.pageSize
				}
				if (this.curStatus !== 'all') {
					params.status = this.curStatus
				}
				const res = await get('/eiot/firewatch/duty-schedule/page', params)
				const newList = (res && res.list) || []
				this.list = this.pageNo === 1 ? newList : this.list.concat(newList)
				this.total = (res && res.total) || 0
				this.loadError = false
			} catch (e) {
				// request.js 已统一 toast 后端返回的 msg，这里只记录失败态（避免双弹窗，与 PC 一致）
				if (this.pageNo === 1) this.loadError = true
			} finally {
				this.loading = false
			}
		},
		loadMore() {
			if (this.loading || this.list.length >= this.total) return
			this.pageNo++
			this.loadList()
		},
		// 切状态筛选必须重置分页，否则会加载新筛选的第 N 页并拼接脏数据
		setStatus(key) {
			this.curStatus = key
			this.refresh()
		},
		onStatClick(key) {
			this.setStatus(this.curStatus === key ? 'all' : key)
		},
		refresh() {
			this.pageNo = 1
			this.list = []
			this.loadError = false
			return Promise.all([this.loadStats(), this.loadList()])
		},
		onCheckIn(item) {
			if (!this.canCheckIn(item)) return
			this.confirmAction(
				'签到确认',
				`确认【${item.dutyUserName}】签到？`,
				() => put(`/eiot/firewatch/duty-schedule/check-in?id=${item.id}&checkInType=manual`),
				'签到成功'
			)
		},
		onCheckOut(item) {
			if (!this.canCheckOut(item)) return
			this.confirmAction(
				'签退确认',
				`确认【${item.dutyUserName}】签退？`,
				() => put(`/eiot/firewatch/duty-schedule/check-out?id=${item.id}`),
				'签退成功'
			)
		},
		// 后端 check-in / check-out 均为 PUT，参数走 query string（@RequestParam）
		confirmAction(title, content, action, okText) {
			uni.showModal({
				title,
				content,
				success: async (res) => {
					if (!res.confirm) return
					try {
						await action()
						uni.showToast({ title: okText, icon: 'success' })
						this.refresh()
					} catch (e) {
						// 失败提示由 request.js 统一弹出
					}
				}
			})
		},
		shiftLabel(type) {
			const map = { day: '白班', night: '夜班', wholeday: '全天班' }
			return map[type] || type
		},
		// 状态文案与后端 FireDutyScheduleServiceImpl#getStatusName 保持同一份映射
		statusLabel(status) {
			const map = {
				scheduled: '已排班', on_duty: '值班中', completed: '已完成', absent: '缺勤',
				early_leave: '早退', no_check_in: '无签到记录'
			}
			return map[status] || status
		},
		// 历史数据已按同一口径落库为 absent/early_leave，此处同时兼容尚未转态的记录：
		// 班次结束后 scheduled 未签到 = 缺勤、on_duty 未签退 = 早退（口径见后端 Mapper.SHIFT_ENDED）
		cardStatus(item) {
			if (item.status === 'completed') return item.checkInTime ? 'completed' : 'no_check_in'
			if (!this.isEnded(item)) return item.status
			if (item.status === 'scheduled') return 'absent'
			if (item.status === 'on_duty') return 'early_leave'
			return item.status
		},
		watermarkText(item) {
			const s = this.cardStatus(item)
			return s === 'absent' || s === 'early_leave' || s === 'no_check_in' ? this.statusLabel(s) : ''
		},
		// 签到/签退可用性统一走 cardStatus：班次一结束就推导为 absent/early_leave，按钮随之置灰，
		// 避开 UI 可点但接口已不该调的脏数据（重复签到/事后签退）
		canCheckIn(item) {
			return this.cardStatus(item) === 'scheduled'
		},
		canCheckOut(item) {
			return this.cardStatus(item) === 'on_duty'
		},
		isEnded(item) {
			if (!item.scheduleDate || !item.endTime) return false
			const p = n => String(n).padStart(2, '0')
			// 夜班（start_time >= end_time）跨天到次日结束，与后端 TIMESTAMP(IF(start_time >= end_time, ...)) 同口径
			let date = item.scheduleDate
			if (item.startTime && item.endTime <= item.startTime) {
				const d = new Date(String(item.scheduleDate).replace(/-/g, '/'))
				d.setDate(d.getDate() + 1)
				date = d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate())
			}
			// 字符串比较避开小程序 Date 字符串解析兼容问题
			const now = new Date()
			const nowStr = `${now.getFullYear()}-${p(now.getMonth() + 1)}-${p(now.getDate())} ${p(now.getHours())}:${p(now.getMinutes())}`
			return `${date} ${item.endTime}` < nowStr
		},
		// 跨天班次（如全天班 08:00→08:00、夜班 18:00→08:00）展示时标注"次日"，避免看不出结束日期
		dutyTimeText(item) {
			if (!item.startTime || !item.endTime) return `${item.startTime || ''} - ${item.endTime || ''}`
			return `${item.startTime} - ${item.endTime <= item.startTime ? '次日 ' : ''}${item.endTime}`
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	background: #f2f3f5;
	padding-top: calc(var(--status-bar-height, 20px) + 44px + 30px);
	padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

/* 统计概览 */
.stat-row {
	display: grid;
	grid-template-columns: repeat(4, 1fr);
	gap: 16rpx;
	padding: 20rpx 24rpx;
}
.stat-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 20rpx 12rpx;
	text-align: center;
	box-shadow: 0 1rpx 8rpx rgba(0,0,0,.04);
}
.stat-num {
	font-size: 36rpx;
	font-weight: 700;
	display: block;
}
.stat-label {
	font-size: 22rpx;
	color: #999;
	margin-top: 8rpx;
	display: block;
}

/* 筛选栏 */
.filter-bar {
	padding: 0 24rpx;
	margin-bottom: 16rpx;
}
.filter-chips {
	display: flex;
	gap: 16rpx;
	overflow-x: auto;
}
.chip {
	flex-shrink: 0;
	padding: 12rpx 24rpx;
	border-radius: 32rpx;
	font-size: 24rpx;
	color: #666;
	background: #fff;
	border: 1rpx solid #e8e8e8;
}
.chip.active {
	color: #3d5bd9;
	background: #e8ecfa;
	border-color: #3d5bd9;
	font-weight: 600;
}

/* 排班列表 */
.schedule-list {
	height: calc(100vh - var(--status-bar-height, 20px) - 44px - 140rpx - 80rpx - 120rpx - env(safe-area-inset-bottom));
	padding: 0 24rpx;
	box-sizing: border-box;
}
.schedule-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin-bottom: 16rpx;
	box-shadow: 0 1rpx 8rpx rgba(0,0,0,.04);
	position: relative;
	overflow: hidden;
}
.card-top {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16rpx;
}
.card-title-row {
	display: flex;
	gap: 12rpx;
	align-items: center;
}
.shift-tag {
	font-size: 22rpx;
	padding: 4rpx 16rpx;
	border-radius: 8rpx;
	font-weight: 600;
}
.shift-day { background: #e8f5e9; color: #43a047; }
.shift-night { background: #e3f2fd; color: #1e88e5; }
.shift-wholeday { background: #fff3e0; color: #ef6c00; }
.status-tag {
	font-size: 22rpx;
	padding: 4rpx 16rpx;
	border-radius: 8rpx;
}
.status-scheduled { background: #f5f5f5; color: #909399; }
.status-on_duty { background: #e8f5e9; color: #43a047; }
.status-completed { background: #e3f2fd; color: #1e88e5; }
.status-absent { background: #ffebee; color: #e53935; }
.status-early_leave { background: #fff3e0; color: #ef6c00; }
.status-no_check_in { background: #f3e5f5; color: #8e24aa; }
.card-date {
	font-size: 24rpx;
	color: #999;
}
.card-body {
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}
.info-row {
	display: flex;
	justify-content: space-between;
	align-items: center;
}
.info-label {
	font-size: 24rpx;
	color: #999;
}
.info-val {
	font-size: 26rpx;
	color: #1a1a1a;
	font-weight: 500;
}
.card-actions {
	display: flex;
	gap: 16rpx;
	margin-top: 16rpx;
	padding-top: 16rpx;
	border-top: 1rpx solid #f0f0f0;
}
.action-btn {
	flex: 1;
	text-align: center;
	padding: 16rpx 0;
	border-radius: 12rpx;
	font-size: 26rpx;
	font-weight: 600;
}
.btn-checkin {
	background: #e8f5e9;
	color: #43a047;
}
.btn-checkout {
	background: #fff3e0;
	color: #ef6c00;
}
/* 后置声明 + 双类选择器，优先级高于 btn-checkin/btn-checkout，无需 !important */
.action-btn.disabled {
	background: #f5f5f5;
	color: #b8b9bf;
}

.empty-tip, .loading-tip, .end-tip {
	text-align: center;
	font-size: 26rpx;
	color: #999;
	padding: 60rpx 0;
}
.end-tip {
	padding: 30rpx 0;
}

::v-deep .aw-navbar__title-wrap.is-center {
	margin-left: 35px;
}

/* 异常班次水印 */
.watermark {
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%) rotate(-30deg);
	font-size: 72rpx;
	font-weight: 700;
	color: rgba(245, 108, 108, 0.15);
	pointer-events: none;
	z-index: 10;
	letter-spacing: 8rpx;
	white-space: nowrap;
}
</style>
