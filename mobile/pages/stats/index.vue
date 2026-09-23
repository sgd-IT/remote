<template>
	<view class="page">
		<NavBar title="数据报表" title-mode="center" :border="false" />

		<!-- KPI 指标卡片 -->
		<view class="kpi-row">
			<view class="kpi-card" v-for="k in kpiList" :key="k.label">
				<text class="kpi-label">{{ k.label }}</text>
				<view class="kpi-val-row">
					<text class="kpi-val">{{ k.value }}</text>
					<text class="kpi-unit">{{ k.unit }}</text>
				</view>
				<view class="kpi-decor"></view>
			</view>
		</view>

		<!-- 近七日告警趋势 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-tt">近七日告警趋势</text>
				<view class="legend-row">
					<view class="lg-item" @click="toggleTrend(0)">
						<view class="lg-dot lg-dot--solid" :class="{ muted: !trendShow[0] }"></view>
						<text class="lg-txt" :class="{ muted: !trendShow[0] }">告警次数</text>
					</view>
					<view class="lg-item" @click="toggleTrend(1)">
						<view class="lg-dot lg-dot--hollow" :class="{ muted: !trendShow[1] }"></view>
						<text class="lg-txt" :class="{ muted: !trendShow[1] }">已处理</text>
					</view>
				</view>
			</view>
			<view class="chart-wrap">
				<qiun-data-charts
					v-if="trendCategories.length"
					type="area"
					:chartData="trendChartData"
					:opts="trendOpts"
					canvasId="trendChart"
					canvas2d
					ontouch
					:animation="true"
					:errorShow="false"
					background="rgba(0,0,0,0)"
				/>
				<view v-else-if="!loading" class="chart-empty">暂无数据</view>
			</view>
		</view>

		<!-- 告警类型分布 -->
		<view class="card">
			<view class="card-hd">
				<text class="card-tt">告警类型分布</text>
			</view>
			<view class="chart-wrap">
				<qiun-data-charts
					v-if="barCategories.length"
					type="column"
					:chartData="barChartData"
					:opts="barOpts"
					canvasId="barChart"
					canvas2d
					ontouch
					:animation="true"
					:errorShow="false"
					background="rgba(0,0,0,0)"
				/>
				<view v-else-if="!loading" class="chart-empty">暂无数据</view>
			</view>
		</view>

		<!-- 底部导出 -->
		<view class="export-panel">
			<view class="exp-btn exp-btn--outline" @click="onExportReport">
				<text class="exp-ico">⤓</text>
				<text class="exp-txt">导出值守报告</text>
			</view>
			<view class="exp-btn exp-btn--solid" @click="onExportRisk">
				<text class="exp-ico exp-ico--w">⤓</text>
				<text class="exp-txt exp-txt--w">导出隐患清单</text>
			</view>
		</view>

		<TabBar current="workspace" />
	</view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import NavBar from '@/components/NavBar/NavBar.vue'
import { get } from '@/utils/request.js'

const THEME = '#1e39b1'
const THEME_LIGHT = '#7b93e8'

export default {
	components: { TabBar, NavBar },
	data() {
		return {
			loading: false,
			kpiList: [
				{ key: 'checkInRate', label: '本月签到率', value: '-', unit: '%' },
				{ key: 'rectificationRate', label: '隐患整改率', value: '-', unit: '%' },
				{ key: 'handleRate', label: '告警处置率', value: '-', unit: '%' }
			],
			trendCategories: [],
			trendSeries: [
				[],
				[]
			],
			trendShow: [true, true],
			barCategories: [],
			barSeries: []
		}
	},
	onLoad() {
		this.loadData()
	},
	onPullDownRefresh() {
		this.loadData().finally(() => uni.stopPullDownRefresh())
	},
	computed: {
		trendChartData() {
			const series = []
			if (this.trendShow[0]) {
				series.push({ name: '告警次数', data: this.trendSeries[0] })
			}
			if (this.trendShow[1]) {
				series.push({ name: '已处理', data: this.trendSeries[1] })
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
		},
		barChartData() {
			return {
				categories: this.barCategories,
				series: [{ name: '数量', data: this.barSeries }]
			}
		},
		barOpts() {
			return {
				color: [THEME],
				padding: [15, 15, 0, 15],
				dataLabel: true,
				xAxis: {
					disableGrid: true,
					fontColor: '#999999',
					fontSize: 10
				},
				yAxis: {
					data: [{ min: 0 }],
					disableGrid: true
				},
				legend: { show: false },
				extra: {
					column: {
						type: 'group',
						width: 28,
						barBorderRadius: [8, 8, 0, 0],
						activeBgColor: '#000000',
						activeBgOpacity: 0.08
					}
				}
			}
		}
	},
	methods: {
		// 全部为后端已有聚合接口（角色 167 已授权 query）：趋势/类型分布来自数据中心，三项 KPI 分别对应值班/隐患/告警统计
		async loadData() {
			if (this.loading) return
			this.loading = true
			try {
				const [trend, typeDist, hazardStats, alarmStats, dutyStats] = await Promise.all([
					get('/eiot/firewatch/data-center/alarm-trend', { days: 7 }),
					get('/eiot/firewatch/data-center/alarm-type-distribution'),
					get('/eiot/firewatch/data-center/hazard-stats'),
					get('/eiot/firewatch/alarm-order/statistics'),
					get('/eiot/firewatch/data-center/duty-stats')
				])
				if (trend && trend.length) {
					this.trendCategories = trend.map(t => (t.date || '').slice(5).replace('-', '/'))
					this.trendSeries = [
						trend.map(t => t.alarmCount || 0),
						trend.map(t => t.handleCount || 0)
					]
				}
				if (typeDist && typeDist.length) {
					this.barCategories = typeDist.map(t => t.typeName || '-')
					this.barSeries = typeDist.map(t => t.count || 0)
				}
				const s = {
					checkInRate: dutyStats && dutyStats.checkInRate,
					rectificationRate: hazardStats && hazardStats.rectificationRate,
					handleRate: alarmStats && alarmStats.handleRate
				}
				this.kpiList.forEach(k => {
					const v = s[k.key]
					k.value = (v === null || v === undefined) ? '-' : String(Math.round(v))
				})
			} catch (e) {} finally {
				this.loading = false
			}
		},
		toggleTrend(idx) {
			this.trendShow[idx] = !this.trendShow[idx]
		},
		onToday() {
			uni.showToast({ title: '功能开发中', icon: 'none' })
		},
		onExportReport() {
			uni.showToast({ title: '功能开发中', icon: 'none' })
		},
		onExportRisk() {
			uni.showToast({ title: '功能开发中', icon: 'none' })
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	background: #f2f4f8;
	padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

/* ===== 今日按钮 ===== */
.today-btn {
	background: #1e39b1;
	border-radius: 32rpx;
	padding: 12rpx 36rpx;

	&--press {
		opacity: 0.8;
	}
}
.today-btn-txt {
	font-size: 26rpx;
	color: #fff;
	font-weight: 500;
}

/* ===== KPI 卡片 ===== */
.kpi-row {
	display: flex;
	gap: 16rpx;
	padding: 24rpx 24rpx 0;
	margin-top: calc(var(--status-bar-height, 20px) + 64px);
}
.kpi-card {
	flex: 1;
	position: relative;
	overflow: hidden;
	border-radius: 16rpx;
	padding: 24rpx 20rpx 28rpx;
	background: linear-gradient(150deg, #4a6cf7, #1e39b1);
}
.kpi-label {
	display: block;
	font-size: 22rpx;
	color: rgba(255, 255, 255, .75);
}
.kpi-val-row {
	display: flex;
	align-items: baseline;
	gap: 4rpx;
	margin-top: 14rpx;
}
.kpi-val {
	font-size: 48rpx;
	font-weight: 700;
	color: #fff;
	line-height: 1;
}
.kpi-unit {
	font-size: 24rpx;
	color: rgba(255, 255, 255, .85);
}
.kpi-decor {
	position: absolute;
	right: -24rpx;
	bottom: -30rpx;
	width: 110rpx;
	height: 110rpx;
	border-radius: 50%;
	background: rgba(255, 255, 255, .12);
}

/* ===== 通用卡片 ===== */
.card {
	margin: 24rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	padding: 28rpx;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, .04);
}
.card-hd {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-bottom: 16rpx;
}
.card-tt {
	font-size: 28rpx;
	font-weight: 600;
	color: #333;
}

/* ===== 图例 ===== */
.legend-row {
	display: flex;
	align-items: center;
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
	border: 3rpx solid #7b93e8;
	background: #fff;
}
.lg-txt {
	font-size: 22rpx;
	color: #999;
}
.muted {
	opacity: .35;
}

/* ===== 图表容器 ===== */
.chart-wrap {
	width: 100%;
	height: 480rpx;
}
.chart-empty {
	height: 480rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	color: #ccc;
	font-size: 26rpx;
}

/* ===== 底部导出 ===== */
.export-panel {
	display: flex;
	gap: 20rpx;
	margin: 32rpx 24rpx 24rpx;
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, .04);
}
.exp-btn {
	flex: 1;
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 8rpx;
	padding: 22rpx 0;
	border-radius: 44rpx;
}
.exp-btn--outline {
	border: 2rpx solid #1e39b1;
	background: #fff;
}
.exp-btn--solid {
	background: #1e39b1;
}
.exp-ico {
	font-size: 28rpx;
	color: #1e39b1;
}
.exp-ico--w {
	color: #fff;
}
.exp-txt {
	font-size: 26rpx;
	color: #1e39b1;
	font-weight: 500;
}
.exp-txt--w {
	color: #fff;
}
</style>
