<template>
	<view class="aw-navbar" :class="['theme-' + theme, { 'is-fixed': fixed, 'no-border': !border }]">
		<view class="aw-navbar__status" :style="{ height: statusBarHeight + 'px' }"></view>
		<view class="aw-navbar__body" :style="{ height: navHeight + 'px' }">
			<view
				v-if="back"
				class="aw-navbar__back"
				hover-class="aw-navbar__back--press"
				:hover-stay-time="100"
				@click="onBack"
			>
				<uni-icons type="left" :size="20" :color="theme === 'light' ? '#1a1a1a' : '#ffffff'"></uni-icons>
				<text v-if="backText" class="aw-navbar__back-text">{{ backText }}</text>
			</view>
			<view v-else class="aw-navbar__placeholder"></view>

			<view class="aw-navbar__title-wrap" :class="titleMode === 'left' ? 'is-left' : 'is-center'">
				<slot>
					<text class="aw-navbar__title" :class="'tt-' + titleMode">{{ title }}</text>
				</slot>
			</view>

			<view class="aw-navbar__right">
				<slot name="right"></slot>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'NavBar',
	props: {
		title: { type: String, default: '' },
		back: { type: Boolean, default: false },
		backText: { type: String, default: '' },
		// center: 标准居中标题 | left: 大标题左对齐
		titleMode: { type: String, default: 'center' },
		// light: 白底黑字 | dark: 深蓝底白字 | transparent: 透明叠底
		theme: { type: String, default: 'light' },
		fixed: { type: Boolean, default: true },
		border: { type: Boolean, default: true }
	},
	data() {
		return {
			statusBarHeight: 20,
			navHeight: 44
		}
	},
	mounted() {
		try {
			const sys = uni.getSystemInfoSync()
			if (sys.statusBarHeight) this.statusBarHeight = sys.statusBarHeight
		} catch (e) {}
		// #ifdef MP-WEIXIN
		try {
			// 胶囊对齐公式：导航内容高度 = (胶囊top - 状态栏高度) × 2 + 胶囊高度
			const rect = uni.getMenuButtonBoundingClientRect()
			this.navHeight = (rect.top - this.statusBarHeight) * 2 + rect.height
		} catch (e) {}
		// #endif
	},
	methods: {
		onBack() {
			const pages = getCurrentPages()
			if (pages.length > 1) {
				uni.navigateBack({ delta: 1 })
			} else {
				uni.redirectTo({ url: '/pages/alarm-center/index' })
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.aw-navbar {
	position: relative;
	background: #fff;
	z-index: 900;

	&.is-fixed {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
	}
	&::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1rpx;
		background: rgba(0, 0, 0, 0.05);
	}
	&.no-border::after {
		display: none;
	}

	&.theme-dark {
		background: #16255f;
		&::after { background: rgba(255, 255, 255, 0.08); }
	}
	&.theme-transparent {
		background: transparent;
		&::after { display: none; }
	}
}

.aw-navbar__body {
	display: flex;
	align-items: center;
	position: relative;
	padding: 0 12rpx;
}

.aw-navbar__back {
	display: flex;
	align-items: center;
	gap: 2rpx;
	min-width: 84rpx;
	height: 64rpx;
	padding: 0 14rpx;
	border-radius: 32rpx;
	z-index: 2;

	&--press {
		background: rgba(0, 0, 0, 0.05);
	}
}
.theme-dark .aw-navbar__back--press,
.theme-transparent .aw-navbar__back--press {
	background: rgba(255, 255, 255, 0.15);
}
.aw-navbar__back-text {
	font-size: 28rpx;
	color: #1a1a1a;
}
.theme-dark .aw-navbar__back-text,
.theme-transparent .aw-navbar__back-text {
	color: #fff;
}

.aw-navbar__placeholder {
	width: 24rpx;
	flex-shrink: 0;
}

.aw-navbar__title-wrap {
	flex: 1;
	min-width: 0;
	display: flex;

	&.is-center {
		justify-content: center;
	}
	&.is-left {
		padding-left: 12rpx;
	}
}
.aw-navbar__title {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;

	&.tt-center {
		font-size: 38rpx;
		font-weight: 700;
		color: #1a1a1a;
		letter-spacing: 1rpx;
		max-width: 420rpx;
	}
	&.tt-left {
		font-size: 38rpx;
		font-weight: 700;
		color: #1a1a1a;
	}
}
.theme-dark .aw-navbar__title,
.theme-transparent .aw-navbar__title {
	color: #fff;
}

.aw-navbar__right {
	display: flex;
	align-items: center;
	min-width: 84rpx;
	justify-content: flex-end;
	padding-right: 8rpx;
	z-index: 2;
}
</style>
