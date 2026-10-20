<template>
	<view class="page">
		<NavBar title="告警详情" title-mode="center" :back="true" :border="scrolled" :theme="navTheme" />

		<view v-if="loadError" class="detail-empty">加载失败，请返回重试</view>
		<view v-else-if="!alarm.id" class="detail-empty">加载中...</view>

		<block v-else>
			<!-- 深头：品牌蓝板块 -->
			<view class="alarm-hero">
				<view class="hero-main">
					<view class="hero-badge">
						<image class="badge-icon" :src="alarm.icon" mode="aspectFit" />
					</view>
					<view class="hero-info">
						<text class="hero-title">{{ alarm.typeName }}</text>
						<text v-if="alarm.subText" class="hero-sub">{{ alarm.subText }}</text>
					</view>
					<view class="hero-pill" :class="'pill-' + alarm.level">
						<text>{{ alarm.levelText }}</text>
					</view>
				</view>

				<view class="hero-strip">
					<view class="strip-cell">
						<text class="cell-label">工单编号</text>
						<text class="cell-value is-mono">{{ alarm.orderNo }}</text>
					</view>
					<view class="strip-cell">
						<text class="cell-label">发生时间</text>
						<text class="cell-value is-mono">{{ alarm.shortTime }}</text>
					</view>
					<view class="strip-cell">
						<text class="cell-label">处置状态</text>
						<view class="strip-pending">
							<view class="pending-dot"></view>
							<text class="pending-text">{{ alarm.statusText }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 事件信息 -->
			<view class="section-card">
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
			<view class="section-card" v-if="locationInfo.length">
				<view class="section-hd">
					<view class="section-dot"></view>
					<text class="section-title">设备定位</text>
				</view>
				<view class="kv-list">
					<view class="kv-row" v-for="item in locationInfo" :key="item.label">
						<text class="kv-label">{{ item.label }}</text>
						<view class="kv-value">
							<text v-if="item.sid" class="loc-pre">{{ item.pre }}</text>
							<text v-if="item.sid" class="loc-sid">{{ item.sid }}</text>
							<text v-if="!item.sid">{{ item.value }}</text>
						</view>
					</view>
				</view>
			</view>

			<!-- 设备信息 -->
			<view class="section-card">
				<view class="section-hd">
					<view class="section-dot"></view>
					<text class="section-title">设备信息</text>
				</view>
				<view class="kv-list">
					<view class="kv-row" v-for="item in deviceInfo" :key="item.label">
						<text class="kv-label">{{ item.label }}</text>
						<view v-if="item.chip" class="chip" :class="{ 'is-ok': item.ok }">
							<view class="chip-dot"></view>
							<text>{{ item.value }}</text>
						</view>
						<text v-else class="kv-value" :class="item.cls || ''">{{ item.value }}</text>
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
		</block>
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'
import { get } from '@/utils/request.js'
import { fmtTime } from '@/utils/card'

// 告警类型 → 头图图标（复用 static 已有 SVG，其余类型用告警工单图标兜底）
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

// 确认档位（仅一级事件有值）
const TIER_MAP = { 1: 'Ⅰ档', 2: 'Ⅱ档', 3: 'Ⅲ档', 4: 'Ⅳ档' }

// 原报摘要展示化：去"来源:"前缀、去设备长 ID、'|' 转分隔点
function prettyDesc(s) {
	if (!s) return ''
	return s.split('|')
		.map(t => t.trim().replace(/^来源[:：]\s*/, ''))
		.join(' · ')
		.replace(/\s*[-–—]\s*\d{12,}\s*/g, ' · ')
		.replace(/(\s*·\s*)+/g, ' · ')
		.replace(/^\s*·\s*|\s*·\s*$/g, '')
}

export default {
	components: { NavBar },
	data() {
		return {
			loadError: false,
			alarm: {},
			eventInfo: [],
			locationInfo: [],
			deviceInfo: [],
			handleRecords: [],
			scrolled: false,
			navTheme: 'light'
		}
	},
	onLoad(options) {
		if (options && options.id) {
			this.fetchDetail(options.id)
		} else {
			this.loadError = true
		}
	},
	onPageScroll(e) {
		const s = e.scrollTop > 180
		if (s !== this.scrolled) {
			this.scrolled = s
			this.syncNav()
		}
	},
	methods: {
		syncNav() {
			this.navTheme = this.alarm.id && !this.scrolled ? 'transparent' : 'light'
		},
		// 告警位置：'火警 - 99015381863555073280140' → 名称 + 编号分列展示
		buildLocation(loc) {
			if (!loc) return []
			const parts = String(loc).split(/\s*[-–—]\s*/)
			if (parts.length > 1 && /^\d{10,}$/.test(parts[1])) {
				return [{ label: '告警位置', pre: parts[0], sid: parts.slice(1).join('') }]
			}
			return [{ label: '告警位置', value: loc }]
		},
		async fetchDetail(id) {
			try {
				const d = await get('/eiot/firewatch/alarm-order/get', { id })
				if (!d || !d.id) {
					this.loadError = true
					return
				}
				const fullTime = fmtTime(d.alarmTime)
				const subParts = [d.deviceName, ALARM_CATEGORY_MAP[d.alarmCategory], TIER_MAP[d.tier]].filter(Boolean)

				this.alarm = {
					id: d.id,
					typeName: d.alarmTypeName || '未知告警',
					icon: TYPE_ICON_MAP[d.alarmType] || DEFAULT_ICON,
					level: d.alarmLevel || 'info',
					levelText: d.alarmLevelName || '-',
					orderNo: d.orderNo || '-',
					shortTime: fullTime.length >= 16 ? fullTime.slice(5, 16) : fullTime,
					statusText: d.orderStatusName || '-',
					subText: subParts.join(' · ')
				}

				this.eventInfo = [
					{ label: '工单编号', value: d.orderNo || '-', cls: 'is-mono' },
					{ label: '事件类型', value: d.alarmTypeName || '-', cls: 'val-alert' },
					{ label: '发生时间', value: fullTime },
					{ label: '事件分类', value: ALARM_CATEGORY_MAP[d.alarmCategory] || '主机级' }
				]
				const desc = prettyDesc(d.alarmDescription)
				if (desc) {
					this.eventInfo.push({ label: '原报摘要', value: desc })
				}

				this.locationInfo = this.buildLocation(d.alarmLocation)

				const st = d.orderStatus
				this.deviceInfo = [
					{ label: '设备名称', value: d.deviceName || '-' },
					{ label: '设备编号', value: d.deviceCode || '-', cls: d.deviceCode ? '' : 'val-muted' },
					{ label: '设备类型', value: d.deviceTypeName || '-', cls: d.deviceTypeName ? '' : 'val-muted' },
					{ label: '工单状态', value: d.orderStatusName || '-', chip: true, ok: st === 'handled' || st === 'closed' },
					{ label: '所属单位', value: d.propertyName || '-', cls: d.propertyName ? '' : 'val-muted' }
				]

				// 处置记录：后端已给的处置人/时间/备注，未处置则无记录
				if (d.handler) {
					this.handleRecords = [{
						text: `${d.handleType || '处置'}：${d.handleRemark || '无备注'}`,
						time: fmtTime(d.handleTime),
						type: 'handle'
					}]
				}

				this.syncNav()
			} catch (e) {
				this.loadError = true
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
	background: #f4f5f7;
	padding-bottom: 80rpx;
}

/* 空态 */
.detail-empty {
	padding: 120rpx 0;
	text-align: center;
	color: #bbb;
	font-size: 28rpx;
}

/* ══════════ 深头 ══════════ */
.alarm-hero {
	position: relative;
	padding: calc(var(--status-bar-height, 20px) + 44px + 8px) 44rpx 36rpx;
	background: #1e39b1;
	border-radius: 0 0 56rpx 56rpx;
}

.hero-main {
	display: flex;
	align-items: flex-start;
	gap: 28rpx;
}

.hero-badge {
	width: 100rpx;
	height: 100rpx;
	border-radius: 24rpx;
	background: rgba(255, 255, 255, 0.16);
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;

	.badge-icon {
		width: 56rpx;
		height: 56rpx;
	}
}

.hero-info {
	flex: 1;
	min-width: 0;
}

.hero-title {
	display: block;
	font-size: 56rpx;
	font-weight: 700;
	color: #fff;
	line-height: 1.2;
}

.hero-sub {
	display: block;
	margin-top: 12rpx;
	font-size: 25rpx;
	color: rgba(255, 255, 255, 0.66);
}

.hero-pill {
	flex-shrink: 0;
	margin-top: 6rpx;
	padding: 12rpx 26rpx;
	border-radius: 999rpx;
	font-size: 26rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	color: #fff;
	background: rgba(255, 255, 255, 0.16);

	&.pill-critical { background: #d84315; }
	&.pill-major { background: #e65100; }
}

.hero-strip {
	margin-top: 28rpx;
	display: flex;
	background: rgba(255, 255, 255, 0.08);
	border-radius: 20rpx;
	overflow: hidden;
}

.strip-cell {
	flex-grow: 1;
	flex-basis: 0;
	min-width: 0;
	padding: 20rpx 26rpx 22rpx;

	&:first-child { flex-grow: 1.12; }
	&:last-child { flex-grow: 0.9; }
	& + .strip-cell { border-left: 1rpx solid rgba(255, 255, 255, 0.14); }
}

.cell-label {
	display: block;
	font-size: 20rpx;
	color: rgba(255, 255, 255, 0.55);
	margin-bottom: 10rpx;
}

.cell-value {
	font-size: 26rpx;
	font-weight: 600;
	color: #fff;
	white-space: nowrap;

	&.is-mono {
		font-family: Menlo, Consolas, monospace;
		font-size: 24rpx;
	}
}

.strip-pending {
	display: flex;
	align-items: center;
	gap: 10rpx;
}

.pending-dot {
	width: 10rpx;
	height: 10rpx;
	border-radius: 50%;
	background: #ff8a80;
	flex-shrink: 0;
}

.pending-text {
	font-size: 25rpx;
	font-weight: 600;
	color: #fff;
	white-space: nowrap;
}

/* ══════════ 浅身 ══════════ */
.section-card {
	margin: 24rpx 32rpx 0;
	padding: 26rpx 32rpx 18rpx;
	background: #fff;
	border-radius: 32rpx;
	box-shadow: 0 2rpx 4rpx rgba(23, 26, 31, 0.03), 0 20rpx 56rpx -28rpx rgba(23, 26, 31, 0.12);
}

.section-hd {
	display: flex;
	align-items: center;
	gap: 16rpx;
	height: 52rpx;
	margin-bottom: 8rpx;
}

.section-dot {
	width: 6rpx;
	height: 28rpx;
	border-radius: 4rpx;
	background: #1e39b1;
}

.section-title {
	font-size: 29rpx;
	font-weight: 600;
	color: #1a1d26;
}

/* 键值行 */
.kv-list {
	display: flex;
	flex-direction: column;
}

.kv-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 24rpx;
	min-height: 76rpx;
	line-height: 1.5;

	& + .kv-row {
		border-top: 1rpx solid #f1f2f5;
	}
}

.kv-label {
	font-size: 26rpx;
	color: #7a8290;
	flex-shrink: 0;
}

.kv-value {
	flex: 1;
	font-size: 27rpx;
	font-weight: 500;
	color: #23262d;
	text-align: right;
	word-break: break-all;

	&.is-mono {
		font-family: Menlo, Consolas, monospace;
		font-size: 25rpx;
	}
}

.val-alert {
	color: #d84315;
	font-weight: 600;
}

.val-muted {
	color: #c3c8d1;
	font-weight: 400;
}

/* 位置：名称 + 编号分列 */
.loc-pre {
	font-weight: 500;
	color: #23262d;
}

.loc-sid {
	margin-left: 12rpx;
	font-family: Menlo, Consolas, monospace;
	font-size: 24rpx;
	color: #5c6474;
}

/* 状态标签 */
.chip {
	display: flex;
	align-items: center;
	gap: 10rpx;
	padding: 6rpx 22rpx;
	border-radius: 999rpx;
	font-size: 24rpx;
	font-weight: 600;
	color: #d84315;
	background: rgba(216, 67, 21, 0.08);
	border: 1rpx solid rgba(216, 67, 21, 0.18);
	flex-shrink: 0;

	&.is-ok {
		color: #2e7d32;
		background: rgba(46, 125, 50, 0.08);
		border-color: rgba(46, 125, 50, 0.2);

		.chip-dot { background: #43a047; }
	}
}

.chip-dot {
	width: 10rpx;
	height: 10rpx;
	border-radius: 50%;
	background: #e53935;
}

/* 处置记录时间线 */
.timeline {
	padding-left: 8rpx;
}

.tl-item {
	display: flex;
	align-items: flex-start;
	gap: 16rpx;
	padding-bottom: 28rpx;
	position: relative;

	&:last-child {
		padding-bottom: 0;
	}

	&::before {
		content: '';
		position: absolute;
		left: 9rpx;
		top: 24rpx;
		bottom: 0;
		width: 1rpx;
		background: #e8eaee;
	}

	&:last-child::before {
		display: none;
	}
}

.tl-dot {
	width: 20rpx;
	height: 20rpx;
	border-radius: 50%;
	margin-top: 4rpx;
	flex-shrink: 0;

	&.handle { background: #43a047; }
}

.tl-body {
	display: flex;
	flex-direction: column;
	gap: 4rpx;
}

.tl-text {
	font-size: 26rpx;
	color: #23262d;
}

.tl-time {
	font-size: 22rpx;
	color: #b0b5bf;
}
</style>
