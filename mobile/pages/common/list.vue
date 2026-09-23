<template>
	<view class="page">
		<NavBar :title="navTitle" title-mode="center" :border="false" />

		<view class="list">
			<view class="item-card" v-for="it in list" :key="it.id">
				<view class="card-top">
					<text class="card-title">{{ it.title }}</text>
					<text class="card-status" v-if="it.statusText">{{ it.statusText }}</text>
				</view>
				<text class="card-sub" v-if="it.subText">{{ it.subText }}</text>
				<text class="card-time" v-if="it.timeText">{{ it.timeText }}</text>
			</view>

			<view v-if="!list.length && !loading" class="empty">
				<text>暂无数据</text>
			</view>

			<view v-if="list.length && !hasMore" class="list-end">没有更多了</view>
		</view>

		<uni-load-more v-if="loading" :status="'loading'" />
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'
import { get } from '@/utils/request.js'
import { toListItem } from '@/utils/card'

const PAGE_SIZE = 20

// 白名单：type → 接口与字段映射（字段名核对自后端 *RespVO，均为小程序角色已授权的 query 接口）
const API_MAP = {
	patrol: {
		title: '巡检列表', api: '/eiot/firewatch/patrol-task/page',
		fields: { title: 'taskName', sub: ['taskNo', 'propertyName', 'patrolUserName'], status: 'statusName', time: 'planDate' }
	},
	hazard: {
		title: '隐患列表', api: '/eiot/firewatch/hazard/page',
		fields: { title: 'hazardDesc', sub: ['hazardNo', 'hazardLevelName', 'location'], status: 'statusName', time: 'foundDate' }
	},
	repair: {
		title: '维修工单', api: '/eiot/firewatch/repair-order/page',
		fields: { title: 'faultDescription', sub: ['orderNo', 'deviceName', 'propertyName'], status: 'orderStatusName', time: 'reportTime' }
	},
	plan: {
		title: '维保计划', api: '/eiot/firewatch/plan/page',
		fields: { title: 'planName', sub: ['planNo', 'propertyName', 'executePerson'], status: 'planStatusName', time: 'planDate' }
	},
	contract: {
		title: '维保合同', api: '/eiot/firewatch/contract/page',
		fields: { title: 'contractName', sub: ['contractNo', 'maintenanceUnit', 'propertyName'], status: 'contractStatusName', time: 'endDate' }
	},
	unit: {
		title: '维保单位', api: '/eiot/firewatch/maintenance-unit/page',
		fields: { title: 'unitName', sub: ['contactPerson', 'contactPhone', 'qualificationLevel'], status: null, time: 'createTime' }
	},
	// 企业档案：property/own 返回单对象（按当前租户），single 模式包装成单条列表
	own: {
		title: '企业档案', api: '/eiot/firewatch/property/own', single: true,
		fields: { title: 'name', sub: ['address', 'contactName', 'contactPhone', 'riskLevelName'], status: null, time: null }
	}
}

export default {
	components: { NavBar },
	data() {
		return {
			cfg: null,
			navTitle: '',
			list: [],
			pageNo: 1,
			hasMore: true,
			loading: false
		}
	},
	onLoad(options) {
		this.cfg = API_MAP[(options && options.type) || ''] || null
		if (!this.cfg) return
		this.navTitle = this.cfg.title
		this.fetchList(true)
	},
	onReachBottom() {
		if (this.cfg && !this.cfg.single && this.hasMore && !this.loading) {
			this.fetchList(false)
		}
	},
	onPullDownRefresh() {
		if (!this.cfg) {
			uni.stopPullDownRefresh()
			return
		}
		this.fetchList(true).finally(() => uni.stopPullDownRefresh())
	},
	methods: {
		async fetchList(reset) {
			if (!this.cfg || this.loading) return
			this.loading = true
			if (reset) {
				this.pageNo = 1
				this.hasMore = true
			}
			try {
				const d = await get(this.cfg.api, { pageNo: this.pageNo, pageSize: PAGE_SIZE })
				const raw = this.cfg.single
					? (d && d.id ? [d] : [])
					: ((d && d.list) || [])
				const items = raw.map(x => toListItem(x, this.cfg.fields))
				this.list = reset ? items : this.list.concat(items)
				this.hasMore = this.cfg.single ? false : this.list.length < ((d && d.total) || 0)
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

.list {
	padding-top: 200rpx;
}

.item-card {
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
	gap: 12rpx;
}

.card-title {
	flex: 1;
	min-width: 0;
	font-size: 30rpx;
	font-weight: 600;
	color: #1a1a1a;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.card-status {
	font-size: 24rpx;
	color: #1e39b1;
	flex-shrink: 0;
}

.card-sub {
	display: block;
	margin-top: 14rpx;
	font-size: 24rpx;
	color: #666;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.card-time {
	display: block;
	margin-top: 8rpx;
	font-size: 22rpx;
	color: #999;
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
