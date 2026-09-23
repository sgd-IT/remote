<template>
	<view class="tab-bar">
		<view class="tab-item" v-for="t in items" :key="t.path" :class="{ active: current === t.key }" @click="switchTab(t)">
			<view class="icon-wrap">
				<uni-icons :type="t.icon" size="22" :color="current === t.key ? '#1e39b1' : '#999'"></uni-icons>
				<view v-if="t.badge" class="badge">
					<text class="badge-num">{{ t.badge }}</text>
				</view>
			</view>
			<text class="tab-label">{{ t.label }}</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'TabBar',
	props: {
		current: { type: String, default: 'home' }
	},
	data() {
		return {
			items: [
				{ key: 'home', label: '首页', icon: 'home', path: '/pages/index/index', badge: 0 },
				{ key: 'alarm', label: '告警', icon: 'notification', path: '/pages/alarm-center/index', badge: 0 },
				{ key: 'messages', label: '工作台', icon: 'bars', path: '/pages/messages/index', badge: 0 },
				{ key: 'profile', label: '我的', icon: 'person', path: '/pages/profile/index', badge: 0 }
			]
		}
	},
	methods: {
		switchTab(t) {
			if (t.key === this.current) return
			uni.redirectTo({ url: t.path })
		}
	}
}
</script>

<style lang="scss" scoped>
.tab-bar {
	display: flex;
	background: #fff;
	padding: 12rpx 0;
	padding-bottom: calc(12rpx + env(safe-area-inset-bottom));
	border-top: 1rpx solid #f0f0f0;
	position: fixed;
	bottom: 0;
	left: 0;
	right: 0;
	z-index: 999;
}
.tab-item {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 4rpx;
}
.icon-wrap {
	position: relative;
	width: 44rpx;
	height: 44rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.badge {
	position: absolute;
	top: -8rpx;
	right: -16rpx;
	background: #d84315;
	border-radius: 16rpx;
	min-width: 28rpx;
	height: 28rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	padding: 0 6rpx;
}
.badge-num {
	font-size: 18rpx;
	color: #fff;
	font-weight: 600;
}
.tab-label {
	font-size: 20rpx;
	color: #999;
}
.tab-item.active .tab-label {
	color: #1e39b1;
	font-weight: 500;
}
</style>
