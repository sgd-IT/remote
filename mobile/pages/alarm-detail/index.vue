<template>
	<view class="page">
		<NavBar title="告警详情" title-mode="center" :back="true" :border="false" />
		<!-- 顶部告警横幅 -->
		<view class="alarm-banner" :class="'level-' + alarm.level" v-if="alarm.id">
			<view class="banner-left">
				<view class="banner-icon">
					<image class="icon-svg" :src="alarm.icon" mode="aspectFit" />
				</view>
				<view class="banner-info">
					<text class="banner-title">{{ alarm.typeName }}</text>
					<text class="banner-sub">{{ alarm.deviceName }}</text>
				</view>
			</view>
			<view class="banner-badge">
				<text class="badge-text">{{ alarm.levelText }}</text>
			</view>
		</view>

		<view v-if="loadError" class="detail-empty">加载失败，请返回重试</view>
		<view v-else-if="!alarm.id" class="detail-empty">加载中...</view>

		<!-- 事件信息 -->
		<view class="section-card" v-if="alarm.id">
			<view class="section-hd">
				<view class="section-dot"></view>
				<text class="section-title">事件信息</text>
			</view>
			<view class="kv-list">
				<view class="kv-row" v-for="item in eventInfo" :key="item.label">
					<text class="kv-label">{{ item.label }}</text>
					<text class="kv-value" :class="item.cls || ''">{{ item.value }}</text>
				</view>
			</view>
		</view>

		<!-- 设备定位 -->
		<view class="section-card" v-if="alarm.id">
			<view class="section-hd">
				<view class="section-dot"></view>
				<text class="section-title">设备定位</text>
			</view>
			<view class="kv-list">
				<view class="kv-row" v-for="item in locationInfo" :key="item.label">
					<text class="kv-label">{{ item.label }}</text>
					<text class="kv-value" :class="item.cls || ''">{{ item.value }}</text>
				</view>
			</view>
		</view>

		<!-- 设备信息 -->
		<view class="section-card" v-if="alarm.id">
			<view class="section-hd">
				<view class="section-dot"></view>
				<text class="section-title">设备信息</text>
			</view>
			<view class="kv-list">
				<view class="kv-row" v-for="item in deviceInfo" :key="item.label">
					<text class="kv-label">{{ item.label }}</text>
					<text class="kv-value" :class="item.cls || ''">{{ item.value }}</text>
				</view>
			</view>
		</view>

		<!-- 处置记录（如有） -->
		<view class="section-card" v-if="handleRecords.length">
			<view class="section-hd">
				<view class="section-dot"></view>
				<text class="section-title">处置记录</text>
			</view>
			<view class="timeline">
				<view class="tl-item" v-for="(r, i) in handleRecords" :key="i">
					<view class="tl-dot" :class="r.type"></view>
					<view class="tl-body">
						<text class="tl-text">{{ r.text }}</text>
						<text class="tl-time">{{ r.time }}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'
import { get } from '@/utils/request.js'
import { fmtTime } from '@/utils/card'

// 告警类型 → 横幅图标（复用 static 已有 SVG，其余类型用告警工单图标兜底）
const TYPE_ICON_MAP = {
	fire: '/static/icon-fire.svg',
	fault: '/static/icon-fault.svg'
}
const DEFAULT_ICON = '/static/icon-alarm-order.svg'

// alarmCategory（后端 AlarmOrderRespVO：0=主机 1=高温 2=热成像 3=烟火 4=离岗 5=通道占用）
const ALARM_CATEGORY_MAP = {
	0: '主机级',
	1: '高温事件',
	2: '热成像事件',
	3: '烟火事件',
	4: '离岗事件',
	5: '通道占用事件'
}

export default {
	components: { NavBar },
	data() {
		return {
			loading: false,
			loadError: false,
			alarm: {},
			eventInfo: [],
			locationInfo: [],
			deviceInfo: [],
			handleRecords: []
		}
	},
	onLoad(options) {
		if (options && options.id) {
			this.fetchDetail(options.id)
		} else {
			this.loadError = true
		}
	},
	methods: {
		async fetchDetail(id) {
			this.loading = true
			try {
				const d = await get('/eiot/firewatch/alarm-order/get', { id })
				if (!d || !d.id) {
					this.loadError = true
					return
				}
				this.alarm = {
					id: d.id,
					typeName: d.alarmTypeName || '未知告警',
					icon: TYPE_ICON_MAP[d.alarmType] || DEFAULT_ICON,
					level: d.alarmLevel || 'info',
					levelText: d.alarmLevelName || '-',
					deviceName: d.deviceName || '-',
					deviceCode: d.deviceCode || '-',
					statusText: d.orderStatusName || '-'
				}
				this.eventInfo = [
					{ label: '工单编号', value: d.orderNo || '-' },
					{ label: '事件类型', value: d.alarmTypeName || '-' },
					{ label: '发生时间', value: fmtTime(d.alarmTime) },
					{ label: '事件分类', value: ALARM_CATEGORY_MAP[d.alarmCategory] || '主机级' }
				]
				this.locationInfo = d.alarmLocation
					? [{ label: '告警位置', value: d.alarmLocation }]
					: []
				this.deviceInfo = [
					{ label: '设备名称', value: d.deviceName || '-' },
					{ label: '设备编号', value: d.deviceCode || '-' },
					{ label: '设备类型', value: d.deviceTypeName || '-' },
					{ label: '工单状态', value: d.orderStatusName || '-', cls: d.orderStatus === 'handled' || d.orderStatus === 'closed' ? 'val-success' : '' },
					{ label: '所属单位', value: d.propertyName || '-' }
				]
				// 处置记录：后端已给的处置人/时间/备注，未处置则无记录
				if (d.handler) {
					this.handleRecords = [{
						text: `${d.handleType || '处置'}：${d.handleRemark || '无备注'}`,
						time: fmtTime(d.handleTime),
						type: 'handle'
					}]
				}
			} catch (e) {
				this.loadError = true
			} finally {
				this.loading = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
::v-deep .aw-navbar__title-wrap.is-center {
	margin-left: -10px;
}

.page {
	min-height: 100vh;
	background: #f2f3f5;
}

/* 空态 */
.detail-empty {
	padding: 120rpx 0;
	text-align: center;
	color: #bbb;
	font-size: 28rpx;
}

/* 顶部横幅 */
.alarm-banner {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 40rpx 32rpx 36rpx;
	padding-top: calc(var(--status-bar-height, 20px) + 44px + 30px);
	background: linear-gradient(135deg, #d84315, #ff6e40);
}
.alarm-banner.level-critical { background: linear-gradient(135deg, #b71c1c, #e53935); }
.alarm-banner.level-major { background: linear-gradient(135deg, #e65100, #ff9800); }
.alarm-banner.level-warning { background: linear-gradient(135deg, #1565c0, #42a5f5); }
.alarm-banner.level-info { background: linear-gradient(135deg, #546e7a, #90a4ae); }

.banner-left { display: flex; align-items: center; gap: 20rpx; }
.banner-icon {
	width: 80rpx; height: 80rpx;
	background: rgba(255,255,255,.2);
	border-radius: 20rpx;
	display: flex; align-items: center; justify-content: center;
}
.icon-svg { width: 44rpx; height: 44rpx; }
.banner-info { display: flex; flex-direction: column; gap: 6rpx; }
.banner-title { font-size: 36rpx; font-weight: 700; color: #fff; }
.banner-sub { font-size: 24rpx; color: rgba(255,255,255,.8); }

.banner-badge {
	background: rgba(255,255,255,.25);
	padding: 8rpx 20rpx;
	border-radius: 8rpx;
}
.badge-text { font-size: 24rpx; font-weight: 600; color: #fff; }

/* 分组卡片 */
.section-card {
	background: #fff;
	border-radius: 16rpx;
	margin: 24rpx 24rpx 0;
	padding: 28rpx;
}
.section-hd {
	display: flex;
	align-items: center;
	gap: 12rpx;
	margin-bottom: 24rpx;
}
.section-dot {
	width: 8rpx; height: 28rpx;
	background: #1e39b1;
	border-radius: 4rpx;
}
.section-title { font-size: 28rpx; font-weight: 600; color: #1a1a1a; }

/* 键值行 */
.kv-list { display: flex; flex-direction: column; }
.kv-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 18rpx 0;
	border-bottom: 1rpx solid #f5f5f5;
	&:last-child { border-bottom: none; }
}
.kv-label { font-size: 26rpx; color: #999; flex-shrink: 0; width: 160rpx; }
.kv-value { font-size: 26rpx; color: #333; text-align: right; flex: 1; }
.val-danger { color: #d84315; font-weight: 600; }
.val-warn { color: #e65100; font-weight: 500; }
.val-success { color: #2e7d32; }

/* 时间线 */
.timeline { padding-left: 8rpx; }
.tl-item {
	display: flex;
	align-items: flex-start;
	gap: 16rpx;
	padding-bottom: 28rpx;
	position: relative;
	&:last-child { padding-bottom: 0; }
	&::before {
		content: '';
		position: absolute;
		left: 9rpx; top: 24rpx; bottom: 0;
		width: 2rpx;
		background: #e0e0e0;
	}
	&:last-child::before { display: none; }
}
.tl-dot {
	width: 20rpx; height: 20rpx;
	border-radius: 50%;
	margin-top: 4rpx;
	flex-shrink: 0;
	&.handle { background: #43a047; }
}
.tl-body { display: flex; flex-direction: column; gap: 4rpx; }
.tl-text { font-size: 26rpx; color: #333; }
.tl-time { font-size: 22rpx; color: #bbb; }

</style>
