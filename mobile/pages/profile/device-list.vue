<template>
	<view class="page">
		<NavBar title="设备列表" title-mode="center" :border="false" />

		<view class="device-list">
			<view class="device-card" v-for="d in deviceList" :key="d.id">
				<view class="card-top">
					<view class="device-name-row">
						<view class="dot" :style="{ background: stateMap[d.state]?.color }"></view>
						<text class="device-name">{{ d.name || '未命名设备' }}</text>
					</view>
					<text class="state-text" :style="{ color: stateMap[d.state]?.color }">{{ stateMap[d.state]?.text }}</text>
				</view>

				<view class="card-info">
					<view class="info-line" v-if="d.dn">
						<uni-icons type="compose" size="14" color="#999"></uni-icons>
						<text class="info-val">设备编号：{{ d.dn }}</text>
					</view>
					<view class="info-line" v-if="d.serialNo">
						<uni-icons type="paperclip" size="14" color="#999"></uni-icons>
						<text class="info-val">序列号：{{ d.serialNo }}</text>
					</view>
					<view class="info-line" v-if="d.addr">
						<image class="info-svg" src="/static/icon-location.svg" />
						<text class="info-val">{{ d.addr }}</text>
					</view>
					<view class="info-line" v-if="d.productName">
						<uni-icons type="home" size="14" color="#999"></uni-icons>
						<text class="info-val">{{ d.productName }}</text>
					</view>
				</view>
			</view>

			<view v-if="!deviceList.length && !loading" class="empty">
				<text>暂无设备</text>
			</view>

			<view v-if="deviceList.length && !hasMore" class="list-end">没有更多了</view>
		</view>

		<uni-load-more v-if="loading" :status="'loading'" />
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'
import { get } from '@/utils/request.js'

const PAGE_SIZE = 20

const STATE_MAP = {
	0: { text: '离线', color: '#9e9e9e' },
	1: { text: '在线', color: '#4caf50' },
	2: { text: '未激活', color: '#ff9800' },
	3: { text: '禁用', color: '#f44336' }
}

export default {
	components: { NavBar },
	data() {
		return {
			tenantId: null,
			deviceList: [],
			pageNo: 1,
			hasMore: true,
			loading: false,
			stateMap: STATE_MAP
		}
	},
	onLoad() {
		this.tenantId = uni.getStorageSync('tenantId') || null
		// 租户未知时先从用户信息接口解析，确保设备列表按当前登录用户所属租户过滤
		this.ensureTenant().finally(() => this.fetchList(true))
	},
	onReachBottom() {
		if (this.hasMore && !this.loading) {
			this.fetchList(false)
		}
	},
	onPullDownRefresh() {
		this.fetchList(true).finally(() => uni.stopPullDownRefresh())
	},
	methods: {
		async ensureTenant() {
			if (this.tenantId) return
			try {
				const data = await get('/system/user/profile/get')
				if (data && data.tenantId) {
					this.tenantId = data.tenantId
					uni.setStorageSync('tenantId', data.tenantId)
				}
			} catch (e) {}
		},
		async fetchList(reset) {
			if (this.loading) return
			this.loading = true
			if (reset) {
				this.pageNo = 1
				this.hasMore = true
			}
			try {
				const params = { pageNo: this.pageNo, pageSize: PAGE_SIZE }
				const d = await get('/eiot/device/page', params)
				const list = d.list || []
				this.deviceList = reset ? list : this.deviceList.concat(list)
				this.hasMore = this.deviceList.length < (d.total || 0)
				this.pageNo++
			} catch (e) {
			} finally {
				this.loading = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
::v-deep .aw-navbar__title-wrap.is-center {
	margin-left: 35px;
}

.page {
	min-height: 100vh;
	background: #f2f3f5;
}

.device-list {
	padding-top: 200rpx;
}

.device-card {
	background: #fff;
	border-radius: 16rpx;
	padding: 24rpx;
	margin: 0 24rpx 20rpx;
	box-sizing: border-box;
	box-shadow: 0 1rpx 6rpx rgba(0,0,0,.03);
}

.card-top {
	display: flex;
	justify-content: space-between;
	align-items: center;
}

.device-name-row {
	display: flex;
	align-items: center;
	gap: 12rpx;
	flex: 1;
	min-width: 0;
}

.dot {
	width: 16rpx;
	height: 16rpx;
	border-radius: 50%;
	flex-shrink: 0;
}

.device-name {
	flex: 1;
	min-width: 0;
	font-size: 30rpx;
	font-weight: 600;
	color: #1a1a1a;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.state-text {
	font-size: 24rpx;
	flex-shrink: 0;
	margin-left: 12rpx;
}

.card-info {
	margin-top: 18rpx;
}

.info-line {
	display: flex;
	align-items: center;
	gap: 8rpx;
	margin-bottom: 8rpx;
	&:last-child { margin-bottom: 0; }
}

.info-svg {
	width: 28rpx;
	height: 28rpx;
	flex-shrink: 0;
}

.info-val {
	font-size: 24rpx;
	color: #666;
	flex: 1;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.empty {
	text-align: center;
	padding: 120rpx 0;
	color: #ccc;
	font-size: 28rpx;
}

.list-end {
	text-align: center;
	padding: 20rpx 0 40rpx;
	color: #ccc;
	font-size: 24rpx;
}
</style>
