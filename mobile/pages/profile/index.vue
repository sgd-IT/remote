<template>
	<view class="page">
		<NavBar title="我的" title-mode="center" :border="false" />
		<view class="user-header">
			<button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
				<view class="avatar-wrap">
					<image v-if="userInfo.avatar" class="avatar-img" :src="userInfo.avatar" mode="aspectFill" />
					<uni-icons v-else type="person" size="36" color="#fff"></uni-icons>
				</view>
			</button>
			<view class="user-info">
				<input class="user-name-input" type="nickname" :value="userInfo.name" placeholder="点击设置昵称" @blur="onNicknameBlur" />
				<text class="user-role">{{ userInfo.role }}</text>
				<text class="user-phone">{{ userInfo.mobile }}</text>
			</view>
		</view>



		<view class="menu-card">
			<view class="menu-item" v-for="m in menuGroup2" :key="m.key" @click="onMenuTap(m)">
				<view class="menu-left">
					<view class="menu-icon-wrap" :style="{ background: m.iconBg }">
						<uni-icons :type="m.icon" size="18" :color="m.iconColor"></uni-icons>
					</view>
					<text class="menu-label">{{ m.label }}</text>
				</view>
				<view class="menu-right">
					<text v-if="m.extra" class="menu-extra">{{ m.extra }}</text>
					<uni-icons type="right" size="14" color="#ccc"></uni-icons>
				</view>
			</view>
		</view>

		<button class="logout-btn" @tap="onLogout">退出登录</button>

		<text class="version-text">远程消防业主端 v1.0.0</text>

		<TabBar current="profile" />
	</view>
</template>

<script>
import TabBar from '@/components/TabBar.vue'
import NavBar from '@/components/NavBar/NavBar.vue'
import request, { get, BASE_URL } from '@/utils/request'

export default {
	components: { TabBar, NavBar },
	data() {
		return {
			tenantId: null,
			userInfo: { name: '', role: '', mobile: '', avatar: '' },

			menuGroup2: [
				{ key: 'project', label: '项目切换', icon: 'map-filled', iconColor: '#1565c0', iconBg: '#e3f2fd', extra: '万达广场' },
				{ key: 'feedback', label: '问题反馈', icon: 'compose', iconColor: '#e65100', iconBg: '#fff3e0' },
				{ key: 'settings', label: '系统设置', icon: 'gear-filled', iconColor: '#666', iconBg: '#f5f5f5' },
				{ key: 'help', label: '帮助与反馈', icon: 'help-filled', iconColor: '#c62828', iconBg: '#fbe9e7' }
			]
		}
	},
	onShow() {
		this.fetchProfile()
	},
	methods: {
		async fetchProfile() {
			try {
				const data = await get('/system/user/profile/get')
				this.tenantId = data.tenantId || null
				if (this.tenantId) {
					uni.setStorageSync('tenantId', this.tenantId)
				}
				this.userInfo.name = data.nickname || ''
				this.userInfo.avatar = data.avatar || ''
				this.userInfo.mobile = data.mobile
					? data.mobile.slice(0, 3) + '****' + data.mobile.slice(-4)
					: ''
				if (data.roles && data.roles.length > 0) {
					this.userInfo.role = data.roles.map(r => r.name).join('、')
				} else if (data.dept) {
					this.userInfo.role = data.dept.name || ''
				}
			} catch (e) {}
		},
		onChooseAvatar(e) {
			const tempUrl = e.detail.avatarUrl
			if (!tempUrl) return
			this.userInfo.avatar = tempUrl
			uni.uploadFile({
				url: BASE_URL + '/system/user/profile/update-avatar',
				filePath: tempUrl,
				name: 'avatarFile',
				header: {
					'tenant-id': uni.getStorageSync('tenantId') || '1',
					'Authorization': `Bearer ${uni.getStorageSync('token')}`
				},
				success(res) {
					const parsed = JSON.parse(res.data)
					if (parsed.code === 0) {
						uni.showToast({ title: '头像已更新', icon: 'success' })
					}
				},
				fail() {}
			})
		},
		async onNicknameBlur(e) {
			const nickname = e.detail.value
			if (!nickname || nickname === this.userInfo.name) return
			this.userInfo.name = nickname
			try {
				await request({
					url: '/system/user/profile/update',
					method: 'PUT',
					data: { nickname }
				})
				uni.showToast({ title: '昵称已更新', icon: 'success' })
			} catch (e) {}
		},
		onMenuTap(m) {
			uni.showToast({ title: m.label, icon: 'none' })
		},
		onLogout() {
			uni.showModal({
				title: '提示',
				content: '确定要退出登录吗？',
				confirmColor: '#c62828',
				success: (res) => {
					if (!res.confirm) return
					// 清除所有认证相关信息
					const keys = ['token', 'refreshToken', 'userId', 'tenantId']
					keys.forEach(k => uni.removeStorageSync(k))
					uni.showToast({ title: '已退出登录', icon: 'success' })
					setTimeout(() => {
						uni.reLaunch({ url: '/pages/login/index' })
					}, 500)
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	background: #f2f3f5;
	padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
}

::v-deep .aw-navbar__title-wrap.is-center {
	margin-left: 35px;
}

.user-header {
	background:
		radial-gradient(ellipse at 80% 10%, rgba(255,255,255,.10), transparent 55%),
		linear-gradient(160deg, #3d5bd9, #1e39b1 55%, #132a7e);
	padding: calc(var(--status-bar-height, 20px) + 74px + 30rpx) 32rpx 48rpx;
	display: flex;
	align-items: center;
	gap: 24rpx;
}
.avatar-btn {
	padding: 0;
	margin: 0;
	border: none;
	background: transparent;
	line-height: normal;
	&::after { border: none; }
}
.avatar-wrap {
	width: 100rpx;
	height: 100rpx;
	border-radius: 50%;
	background: rgba(255,255,255,.2);
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
}
.avatar-img {
	width: 100rpx;
	height: 100rpx;
	border-radius: 50%;
}
.user-info { display: flex; flex-direction: column; }
.user-name-input {
	color: #fff;
	font-size: 34rpx;
	font-weight: 700;
	padding: 0;
	height: auto;
}
.user-role { color: rgba(255,255,255,.75); font-size: 24rpx; margin-top: 6rpx; }
.user-phone { color: rgba(255,255,255,.55); font-size: 22rpx; margin-top: 4rpx; }

.menu-card {
	margin: 24rpx 24rpx 0;
	background: #fff;
	border-radius: 16rpx;
	overflow: hidden;
	box-shadow: 0 1rpx 6rpx rgba(0,0,0,.03);
}
.menu-item {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 28rpx 24rpx;
	border-bottom: 1rpx solid #f5f5f5;
	&:last-child { border-bottom: none; }
}
.menu-left { display: flex; align-items: center; gap: 16rpx; }
.menu-icon-wrap {
	width: 56rpx;
	height: 56rpx;
	border-radius: 12rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
.menu-label { font-size: 28rpx; color: #333; }
.menu-right { display: flex; align-items: center; gap: 12rpx; }
.menu-badge {
	padding: 2rpx 14rpx;
	border-radius: 6rpx;
	font-size: 22rpx;
}
.menu-extra { font-size: 24rpx; color: #999; }

.logout-btn {
	margin: 48rpx 24rpx 0;
	height: 88rpx;
	line-height: 88rpx;
	background: #fff;
	color: #c62828;
	font-size: 30rpx;
	font-weight: 500;
	border-radius: 16rpx;
	border: none;
	box-shadow: 0 1rpx 6rpx rgba(0,0,0,.03);
	&::after { border: none; }
	&:active { background: #fef2f2; }
}

.version-text {
	display: block;
	text-align: center;
	color: #ccc;
	font-size: 22rpx;
	margin-top: 40rpx;
	padding-bottom: 20rpx;
}
</style>
