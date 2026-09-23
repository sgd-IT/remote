<template>
	<view class="page">
		<NavBar title="告警中心" title-mode="center" :border="false" />

		<view class="filter-area">
			<view class="tabs-row">
				<view ref="typeNav" class="tab-nav">
					<view ref="typeHl" class="hl"></view>
					<button class="tab" :class="{ active: curType === t.key }" v-for="t in typeFilters" :key="t.key"
						@click="onFilterClick('type', t.key)">{{ t.label }}</button>
				</view>
			</view>
			<view class="tabs-row">
				<view ref="statusNav" class="tab-nav">
					<view ref="statusHl" class="hl"></view>
					<button class="tab" :class="{ active: curStatus === s.key }" v-for="s in statusFilters" :key="s.key"
						@click="onFilterClick('status', s.key)">{{ s.label }}</button>
				</view>
			</view>
		</view>

		<view class="search-bar">
			<input class="search-input" placeholder="搜索告警编号或告警位置" v-model="keyword" />
			<text class="filter-btn" @click="applyFilter">筛选</text>
		</view>

		<scroll-view class="card-list" scroll-y>
			<view class="card" v-for="a in filteredList" :key="a.id" @click="goDetail(a)">
				<view class="card-head">
					<text class="card-title">{{ a.title }}</text>
					<text class="status" :class="a.statusClass">{{ a.statusText }}</text>
				</view>
				<text class="alarm-number">告警编号：{{ a.alarmNo }}</text>
				<text class="card-sub">{{ a.subText }}</text>
				<text class="card-time">告警时间：{{ a.timeText }}</text>
			</view>
			<text v-if="!filteredList.length" class="empty">暂无告警</text>
			<text v-if="filteredList.length" class="end-tip">{{ total > alarmList.length ? '仅显示前 ' + alarmList.length + ' 条告警' : '没有更多了 · 已显示全部告警' }}</text>
		</scroll-view>

		<TabBar current="alarm" />
	</view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import NavBar from '@/components/NavBar/NavBar.vue'
import { get } from '@/utils/request.js'
import { toCard } from '@/utils/card'

export default {
	components: { TabBar, NavBar },
	data() {
		return {
			keyword: '',
			curType: 'all',
			curStatus: 'all',
			loading: false,
			typeFilters: [
				{ key: 'all', label: '全部' },
				{ key: 'fire', label: '火警' },
				{ key: 'fault', label: '故障' },
				{ key: 'offline', label: '离线' },
				{ key: 'risk', label: '风险' },
				{ key: 'false_alarm', label: '误报' }
			],
			statusFilters: [
				{ key: 'all', label: '全部状态' },
				{ key: 'pending', label: '待处理' },
				{ key: 'handling', label: '处理中' },
				{ key: 'handled', label: '已处置' },
				{ key: 'closed', label: '已关闭' }
			],
			alarmList: [],
			total: 0
		}
	},
	onLoad(options) {
		// 支持从首页带入的筛选：filter=fire/fault/false_alarm/pending/handled 等
		if (options && options.filter) {
			const typeMap = { fire: 'fire', fault: 'fault', risk: 'risk', offline: 'offline', false_alarm: 'false_alarm' }
			const statusMap = { pending: 'pending', handling: 'handling', handled: 'handled', closed: 'closed' }
			if (typeMap[options.filter]) {
				this.curType = typeMap[options.filter]
			} else if (statusMap[options.filter]) {
				this.curStatus = statusMap[options.filter]
			}
		}
		this.fetchList()
	},
	onPullDownRefresh() {
		this.fetchList().finally(() => uni.stopPullDownRefresh())
	},
	mounted() {
		// 首屏：椭圆不带动画，直接吸附到默认选中项
		this.$nextTick(() => {
			this.layoutHighlight('type', false)
			this.layoutHighlight('status', false)
		})
		// 窗口尺寸变化时重新测量并校正椭圆位置（直接吸附，不播动画）
		if (typeof window !== 'undefined') {
			this._relayout = () => {
				this.layoutHighlight('type', false)
				this.layoutHighlight('status', false)
			}
			window.addEventListener('resize', this._relayout)
		}
	},
	beforeUnmount() {
		if (typeof window !== 'undefined' && this._relayout) {
			window.removeEventListener('resize', this._relayout)
		}
	},
	computed: {
		// 类型/状态已由后端查询过滤，这里仅做关键字本地过滤
		filteredList() {
			let list = this.alarmList
			if (this.keyword) {
				const kw = this.keyword.toLowerCase()
				list = list.filter(a =>
					(a.alarmNo || '').toLowerCase().includes(kw) ||
					(a.title || '').toLowerCase().includes(kw) ||
					(a.subText || '').toLowerCase().includes(kw) ||
					(a.deviceName || '').toLowerCase().includes(kw) ||
					(a.location || '').toLowerCase().includes(kw)
				)
			}
			return list
		}
	},
	methods: {
		async fetchList() {
			this.loading = true
			try {
				// 单页上限 100，超出部分由底部"仅显示前 N 条"提示引导缩小筛选；不用 mock 兜底，避免假警情
				const params = { pageNo: 1, pageSize: 100 }
				if (this.curType !== 'all') params.alarmType = this.curType
				if (this.curStatus !== 'all') params.orderStatus = this.curStatus
				const d = await get('/eiot/firewatch/alarm-order/page', params)
				this.alarmList = ((d && d.list) || []).map(item => toCard(item))
				this.total = (d && d.total) || this.alarmList.length
			} catch (e) {
				this.alarmList = []
				this.total = 0
			} finally {
				this.loading = false
			}
		},

		onFilterClick(row, key) {
			if (row === 'type') {
				if (this.curType === key) return
				this.curType = key
			} else {
				if (this.curStatus === key) return
				this.curStatus = key
			}
			// 等激活态（含字重变化）落到 DOM 后再测量，保证椭圆宽度和落点基于最新布局
			this.$nextTick(() => this.layoutHighlight(row, true))
			// 对接后端：按对应的类型/状态重新查询
			this.fetchList()
		},

		// 把高亮椭圆移动到当前激活 Tab：读取 offsetWidth / offsetLeft，滑动由 CSS transition 接管
		layoutHighlight(row, animate) {
			const navRef = this.$refs[row + 'Nav']
			const hlRef = this.$refs[row + 'Hl']
			// H5/App WebView 下内置组件 ref 是组件实例，取 $el 拿真实 DOM；小程序无 DOM 时静默降级（仅文字变色）
			const nav = navRef && navRef.$el ? navRef.$el : navRef
			const hl = hlRef && hlRef.$el ? hlRef.$el : hlRef
			if (!nav || !hl || typeof nav.querySelectorAll !== 'function') return

			const list = row === 'type' ? this.typeFilters : this.statusFilters
			const activeKey = row === 'type' ? this.curType : this.curStatus
			const idx = list.findIndex(item => item.key === activeKey)
			const tab = nav.querySelectorAll('.tab')[idx]
			if (!tab) return

			const paddingLeft = parseFloat(getComputedStyle(nav).paddingLeft) || 0
			if (!animate) hl.style.transition = 'none'
			hl.style.width = tab.offsetWidth + 'px'
			hl.style.transform = 'translateX(' + (tab.offsetLeft - paddingLeft) + 'px)'
			if (!animate) {
				void hl.offsetWidth // 强制回流，让初始定位立即生效后再恢复过渡
				hl.style.transition = ''
			}
		},

		applyFilter() {
			// 筛选按钮点击，当前已通过 computed 实时过滤
		},

		goDetail(a) {
			uni.navigateTo({ url: '/pages/alarm-detail/index?id=' + a.id })
		}
	}
}
</script>

<style lang="scss" scoped>
@import '@/styles/card.scss';

page {
	--brand-900: #132a7e;
	--brand-700: #1e39b1;
	--brand-600: #3d5bd9;
	--brand-100: #e8ecfa;
	--page: #f2f3f5;
	--card: #fff;
	--text: #1a1a1a;
	--regular: #333;
	--muted: #666;
	--soft: #595959;
	--border: #f0f0f0;
	--danger: #c62828;
	--warning: #a63d00;
	--success: #2e7d32;
	--info: #1565c0;
	--neutral: #666;
}

.page {
	min-height: 100vh;
	background: var(--page);
	font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
	color: var(--text);
	box-sizing: border-box;
	padding: 12px 12px calc(88px + env(safe-area-inset-bottom));
}

::v-deep .aw-navbar__title-wrap.is-center {
	margin-left: 35px;
}

.filter-area {
	margin-top: calc(var(--status-bar-height, 20px) + 44px);
	background: var(--page);
	padding: 12px 0;
}

.tabs-row {
	overflow-x: auto;
	margin: 0 -12px 5px -40px;
	padding: 0 12px;
}

.tab-nav {
	position: relative;
	display: inline-flex;
	align-items: center;
	gap: 7px;
	padding: 4px 12px;
	white-space: nowrap;
}

/* 滑动高亮椭圆：绝对定位、置于文字下层，宽度与位移由 JS 按激活项测量后驱动 */
.hl {
	position: absolute;
	left: 12px; /* 与 .tab-nav 的 padding-left 保持一致 */
	top: 4px; /* 与 .tab-nav 的 padding-top 保持一致 */
	height: calc(100% - 8px); /* 减去上下 padding */
	border-radius: 999px;
	background: var(--brand-100);
	z-index: 0;
	pointer-events: none;
	transition: transform 0.38s cubic-bezier(0.34, 1.2, 0.4, 1),
		width 0.38s cubic-bezier(0.34, 1.2, 0.4, 1);
}

.tab {
	position: relative;
	z-index: 1;
	display: inline-flex;
	align-items: center;
	justify-content: center;
	margin: 0;
	padding: 6px 14px;
	border: none;
	border-radius: 16px;
	background: transparent;
	font-size: 14px;
	font-family: inherit;
	line-height: 1.5;
	color: var(--muted);
	flex-shrink: 0;
	transition: color 0.2s ease;

	&::after {
		border: none; /* 清除 uni-app button 默认描边 */
	}

	&.active {
		color: #3d5bd9;
		font-weight: 600;
	}
}

.search-bar {
	display: flex;
	align-items: center;
	background: #fff;
	border-radius: 10px;
	padding: 10px 12px;
	margin-top: -20px;
	margin-bottom: 12px;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.search-input {
	flex: 1;
	background: transparent;
	border: none;
	font-size: 14px;
	color: var(--text);
	padding: 4px 0;
}

.filter-btn {
	font-size: 14px;
	color: var(--brand-700);
	padding: 4px 8px;
	flex-shrink: 0;
}

.card-list {
	flex: 1;
}

.empty {
	text-align: center;
	padding: 60px 0;
	color: #ccc;
	font-size: 14px;
	display: block;
}

.end-tip {
	text-align: center;
	padding: 16px 0;
	color: var(--muted);
	font-size: 12px;
	display: block;
}
</style>
