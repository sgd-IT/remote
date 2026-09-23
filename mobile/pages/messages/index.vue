<template>
	<view class="page">
		<NavBar title="工作台" title-mode="center" :border="false">
			<template #right>
				<view class="nav-refresh" @click="onRefresh">
					<text>↻</text>
				</view>
			</template>
		</NavBar>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">现场执行</text>
		</view>
		<view class="entry-grid">
			<view class="entry" v-for="e in fieldList" :key="e.key" @click="onEntry(e)">
				<view class="entry-icon">
					<uni-icons :type="e.icon" size="24" color="#3d5bd9"></uni-icons>
				</view>
				<text class="entry-title">{{ e.title }}</text>
				<text class="entry-desc">{{ e.desc }}</text>
			</view>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">维护管理</text>
		</view>
		<view class="entry-grid">
			<view class="entry" v-for="e in maintainList" :key="e.key" @click="onEntry(e)">
				<view class="entry-icon">
					<uni-icons :type="e.icon" size="24" color="#3d5bd9"></uni-icons>
				</view>
				<text class="entry-title">{{ e.title }}</text>
				<text class="entry-desc">{{ e.desc }}</text>
			</view>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">消防资产</text>
		</view>
		<view class="entry-grid">
			<view class="entry" v-for="e in assetList" :key="e.key" @click="onEntry(e)">
				<view class="entry-icon">
					<uni-icons :type="e.icon" size="24" color="#3d5bd9"></uni-icons>
				</view>
				<text class="entry-title">{{ e.title }}</text>
				<text class="entry-desc">{{ e.desc }}</text>
			</view>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">经营查看</text>
		</view>
		<view class="entry-grid">
			<view class="entry" v-for="e in bizList" :key="e.key" @click="onEntry(e)">
				<view class="entry-icon">
					<uni-icons :type="e.icon" size="24" color="#3d5bd9"></uni-icons>
				</view>
				<text class="entry-title">{{ e.title }}</text>
				<text class="entry-desc">{{ e.desc }}</text>
			</view>
		</view>

		<view class="section-title section-title-standalone">
			<text class="section-title-text">企业信息</text>
		</view>
		<view class="entry-grid">
			<view class="entry" v-for="e in companyList" :key="e.key" @click="onEntry(e)">
				<view class="entry-icon">
					<uni-icons :type="e.icon" size="24" color="#3d5bd9"></uni-icons>
				</view>
				<text class="entry-title">{{ e.title }}</text>
				<text class="entry-desc">{{ e.desc }}</text>
			</view>
		</view>

		<TabBar current="messages" />
	</view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import NavBar from '@/components/NavBar/NavBar.vue'

// 工作台入口跳转表：均为后端已授权 query 接口对应页面；T09 维保报告后端无接口，保持待接入
const ENTRY_ROUTES = {
	F01: '/pages/common/list?type=patrol',
	I01: '/pages/common/list?type=hazard',
	R01: '/pages/common/list?type=repair',
	T02: '/pages/common/list?type=repair',
	T03: '/pages/common/list?type=plan',
	T13: '/pages/common/list?type=plan',
	T06: '/pages/common/list?type=contract',
	T10: '/pages/common/list?type=unit',
	G01: '/pages/stats/index',
	P01: '/pages/common/list?type=own'
}

export default {
	components: { TabBar, NavBar },
	data() {
		return {
			fieldList: [
				{ key: 'E01', icon: 'home-filled', title: '值班首页', desc: '班次与记录' },
				{ key: 'F01', icon: 'navigate-filled', title: '巡检列表', desc: '任务与点位' },
				{ key: 'I01', icon: 'flag-filled', title: '隐患列表', desc: '整改与验收' },
				{ key: 'R01', icon: 'gear-filled', title: '维修列表', desc: '工单执行' }
			],
			maintainList: [
				{ key: 'T02', icon: 'list', title: '维保工单列表', desc: '创建、执行与评价' },
				{ key: 'T03', icon: 'calendar-filled', title: '维保计划列表', desc: '计划管理' },
				{ key: 'T06', icon: 'paperclip', title: '维保合同列表', desc: '合同管理' },
				{ key: 'T09', icon: 'info-filled', title: '维保报告', desc: '已完成维保工单报告' },
				{ key: 'T10', icon: 'shop-filled', title: '维保单位列表', desc: '单位档案' },
				{ key: 'T13', icon: 'notification-filled', title: '维保风险提醒', desc: '风险事项' }
			],
			assetList: [
				{ key: 'D01', icon: 'camera-filled', title: '设备列表', desc: '设备状态与详情' }
			],
			bizList: [
				{ key: 'G01', icon: 'bars', title: '告警统计', desc: '构成与趋势' }
			],
			companyList: [
				{ key: 'P01', icon: 'auth-filled', title: '企业档案', desc: '当前企业档案' }
			]
		}
	},
	methods: {
		onEntry(e) {
			// 值班首页
			if (e.key === 'E01') {
				uni.navigateTo({ url: '/pages/duty/index' })
				return
			}
			// 消防资产 - 设备列表：按当前登录用户所属租户展示设备
			if (e.key === 'D01') {
				uni.navigateTo({ url: '/pages/profile/device-list' })
				return
			}
			const route = ENTRY_ROUTES[e.key]
			if (route) {
				uni.navigateTo({ url: route })
				return
			}
			uni.showToast({ title: `${e.title}（待接入）`, icon: 'none' })
		},
		onRefresh() {
			uni.showToast({ title: '功能开发中', icon: 'none' })
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	background: #f2f3f5;
	padding-top: calc(var(--status-bar-height, 20px) + 44px);
	padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

.nav-refresh {
	padding: 10rpx 16rpx;
	text { font-size: 32rpx; color: #1a1a1a; }
}

::v-deep .aw-navbar__title-wrap.is-center {
	margin-left: 35px;
}

.section-title {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 72rpx;
	margin-bottom: 16rpx;
}
.section-title-standalone {
	margin: calc(24rpx + 20px) 24rpx 0;
}
.section-title-text {
	font-size: 32rpx;
	font-weight: 700;
	color: #1a1a1a;
	padding-left: 20rpx;
	border-left: 8rpx solid #3d5bd9;
	line-height: 1;
}

.entry-grid {
	display: grid;
	grid-template-columns: repeat(2, 1fr);
	gap: 20rpx;
	padding: 0 24rpx;
	margin-top: 10px;
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
