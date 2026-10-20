<template>
	<view class="login-page">
		<view class="login-header">
			<view class="logo-box">
				<image class="logo-icon" src="/static/icon-shield-white.svg" mode="aspectFit" />
			</view>
			<text class="app-name">安为智消云</text>
			<text class="app-desc">智慧消防 · 实时守护</text>
		</view>

		<view class="login-card">
			<text class="card-title">账号登录</text>

			<view class="field" :class="{ focused: focusField === 'user' }">
				<image class="field-icon" src="/static/icon-user.svg" mode="aspectFit" />
				<input class="field-input" v-model="username" placeholder="请输入账号" placeholder-style="color:#b4bcc9"
					maxlength="16" @focus="focusField = 'user'" @blur="focusField = ''" />
				<!-- #ifdef MP-WEIXIN -->
				<button class="wx-phone-btn" open-type="getPhoneNumber" hover-class="wx-phone-btn-hover"
					:disabled="phoneLoading" @getphonenumber="onGetPhoneNumber">获取当前手机号</button>
				<!-- #endif -->
			</view>

			<view class="field" :class="{ focused: focusField === 'pwd' }">
				<image class="field-icon" src="/static/icon-lock.svg" mode="aspectFit" />
				<input class="field-input" v-model="password" :password="!showPassword" placeholder="请输入密码" placeholder-style="color:#b4bcc9"
					maxlength="16" confirm-type="done" @focus="focusField = 'pwd'" @blur="focusField = ''" @confirm="doLogin" />
				<view class="eye-btn" hover-class="eye-btn-hover" @tap="showPassword = !showPassword">
					<image class="eye-icon" :src="showPassword ? '/static/icon-eye-off.svg' : '/static/icon-eye.svg'" mode="aspectFit" />
				</view>
			</view>

			<button class="login-btn" type="primary" hover-class="btn-hover" :loading="loading" :disabled="loading" @tap="doLogin">登　录</button>

			<view class="agree-row">
				<view class="checkbox" :class="{ checked: agreed }" @tap="agreed = !agreed">
					<text v-if="agreed" class="check-mark">✓</text>
				</view>
				<text class="agree-txt" @tap="agreed = !agreed">已阅读并同意</text>
				<text class="agree-link" @tap="openAgreement('service')">《服务协议》</text>
				<text class="agree-txt">和</text>
				<text class="agree-link" @tap="openAgreement('privacy')">《隐私协议》</text>
			</view>

			<view class="remember" @tap="remember = !remember">
				<view class="checkbox" :class="{ checked: remember }">
					<text v-if="remember" class="check-mark">✓</text>
				</view>
				<text class="remember-txt">记住密码</text>
			</view>
		</view>

		<view class="page-footer">
			<view class="contact-block">
				<text class="contact-label">业务负责人：</text>
				<view class="contact-list">
					<view class="contact-row" hover-class="contact-hover" @tap="callPhone('18576897600')">
						<text class="contact-txt">胡经理18576897600</text>
					</view>
					<view class="contact-row" hover-class="contact-hover" @tap="callPhone('13430148074')">
						<text class="contact-txt">郭经理13430148074</text>
					</view>
				</view>
			</view>
			<text class="copyright">Copyright © 2025 广东安为科技有限公司</text>
			<text class="version">Version {{ version }}</text>
		</view>
	</view>
</template>

<script>
import { BASE_URL } from '@/utils/request'

function rawRequest(method, url, data) {
	return new Promise((resolve, reject) => {
		uni.request({
			url: BASE_URL + url,
			method,
			data,
			header: { 'tenant-id': '1' },
			success(res) {
				if (res.statusCode === 200 && res.data.code === 0) resolve(res.data.data)
				else reject(res.data)
			},
			fail: reject
		})
	})
}

const rawGet = (url, data) => rawRequest('GET', url, data)
const rawPost = (url, data) => rawRequest('POST', url, data)

export default {
	data() {
		return {
			username: '',
			password: '',
			remember: false,
			agreed: false,
			showPassword: false,
			phoneLoading: false,
			loading: false,
			focusField: '',
			version: '2.0.1'
		}
	},
	onLoad() {
		// 版本号：体验版/线上版取微信运行时版本号，开发版回退 manifest.json 的 versionName
		// #ifdef MP-WEIXIN
		try {
			this.version = uni.getAccountInfoSync().miniProgram.version || '2.0.1'
		} catch (e) {}
		// #endif
		if (uni.getStorageSync('token')) {
			uni.reLaunch({ url: '/pages/index/index' })
			return
		}
		const saved = uni.getStorageSync('rememberLoginForm')
		if (saved && saved.username) {
			this.username = saved.username
			this.password = saved.password || ''
			this.remember = true
		}
	},
	methods: {
		async doLogin() {
			if (this.loading) return
			if (!this.agreed) {
				uni.showToast({ title: '请先阅读并同意《服务协议》和《隐私协议》', icon: 'none', duration: 2500 })
				return
			}
			if (!/^[A-Za-z0-9]{4,16}$/.test(this.username)) {
				uni.showToast({ title: '账号为 4-16 位字母或数字', icon: 'none' })
				return
			}
			if (!this.password || this.password.length < 4 || this.password.length > 16) {
				uni.showToast({ title: '密码为 4-16 位', icon: 'none' })
				return
			}
			this.loading = true
			try {
				// 小程序专用端点：账号（手机号或用户名）跨租户定位，免验证码（Web 端滑块不受影响）
				const loginNonce = await rawGet('/system/auth/generate-login-nonce')
				const data = await rawPost('/system/auth/mobile-login',
					{ account: this.username, password: this.password, loginNonce })
				this.onLoginSuccess(data)
			} catch (e) {
				uni.showToast({ title: e?.msg || '登录失败，请重试', icon: 'none', duration: 2500 })
			} finally {
				this.loading = false
			}
		},

		async onGetPhoneNumber(e) {
			// 微信 getPhoneNumber 仅返回一次性 code，需服务端换取手机号后回填账号框
			if (this.phoneLoading) return
			const code = e?.detail?.code
			if (!code) {
				uni.showToast({ title: '已取消授权', icon: 'none' })
				return
			}
			this.phoneLoading = true
			try {
				const mobile = await rawPost('/system/auth/wx-ma-phone', { phoneCode: code })
				if (mobile) {
					this.username = mobile
					uni.showToast({ title: '手机号已填充', icon: 'none' })
				}
			} catch (err) {
				uni.showToast({ title: err?.msg || '获取手机号失败，请重试', icon: 'none', duration: 2500 })
			} finally {
				this.phoneLoading = false
			}
		},

		onLoginSuccess(data) {
			if (this.remember) {
				uni.setStorageSync('rememberLoginForm', { username: this.username, password: this.password })
			} else {
				uni.removeStorageSync('rememberLoginForm')
			}
			uni.setStorageSync('token', data.accessToken)
			uni.setStorageSync('refreshToken', data.refreshToken)
			uni.setStorageSync('userId', data.userId)
			if (data.tenantId) {
				uni.setStorageSync('tenantId', data.tenantId)
			}
			uni.showToast({ title: '登录成功', icon: 'success' })
			setTimeout(() => {
				uni.reLaunch({ url: '/pages/index/index' })
			}, 500)
		},

		openAgreement(type) {
			uni.navigateTo({ url: `/pages/common/agreement?type=${type}` })
		},

		callPhone(phone) {
			uni.makePhoneCall({
				phoneNumber: phone,
				fail: () => uni.showToast({ title: `请手动拨打 ${phone}`, icon: 'none' })
			})
		}
	}
}
</script>

<style lang="scss" scoped>
/* 主题变量：沿用全局蓝色系，仅在本页作用域内取用 */
$navy: #16213a;       // 主文字（替代纯黑，与蓝色主题呼应）
$muted: #6b7484;      // 次要文字（白底对比度约 4.9:1，达标 AA）
$faint: #a2aab8;      // 弱化文字（底部声明）
$brand: #3d5bd9;

@keyframes rise {
	from { opacity: 0; transform: translateY(24rpx); }
	to { opacity: 1; transform: translateY(0); }
}

.login-page {
	position: relative;
	min-height: 100vh;
	box-sizing: border-box;
	background: linear-gradient(180deg, #edf1fa 0%, #f5f6f9 62%, #f2f3f7 100%);
	display: flex;
	flex-direction: column;

	/* 顶部品牌光晕，营造纵深 */
	&::before {
		content: '';
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		height: 520rpx;
		background: radial-gradient(ellipse 62% 100% at 50% -10%, rgba(61, 91, 217, 0.12), rgba(61, 91, 217, 0));
		pointer-events: none;
	}
}

.login-header {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding-top: calc(var(--status-bar-height, 44px) + 120rpx);
	padding-bottom: 88rpx;
	animation: rise 0.5s ease-out both;
}

.logo-box {
	width: 144rpx;
	height: 144rpx;
	border-radius: 36rpx;
	background: linear-gradient(140deg, #4a6ae0, #1e39b1);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 36rpx;
	box-shadow: 0 4rpx 10rpx rgba(30, 57, 177, 0.18), 0 16rpx 36rpx rgba(30, 57, 177, 0.28);
}

.logo-icon {
	width: 76rpx;
	height: 76rpx;
}

.app-name {
	color: $navy;
	font-size: 44rpx;
	font-weight: 700;
	letter-spacing: 3rpx;
}

.app-desc {
	color: $muted;
	font-size: 24rpx;
	letter-spacing: 8rpx;
	margin-top: 18rpx;
	margin-left: 8rpx; /* 抵消末字符字距，保证视觉居中 */
}

.login-card {
	position: relative;
	background: #fff;
	border: 1rpx solid rgba(230, 233, 240, 0.7);
	border-radius: 32rpx;
	box-shadow: 0 2rpx 8rpx rgba(22, 33, 58, 0.04), 0 16rpx 48rpx rgba(22, 33, 58, 0.08);
	margin: 0 44rpx;
	padding: 56rpx 44rpx 48rpx;
	animation: rise 0.5s 0.1s ease-out both;
}

.card-title {
	display: block;
	color: $navy;
	font-size: 34rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
	margin-bottom: 44rpx;
}

.field {
	display: flex;
	align-items: center;
	background: #f7f8fb;
	border: 2rpx solid transparent;
	border-radius: 20rpx;
	height: 104rpx;
	padding: 0 28rpx;
	margin-bottom: 28rpx;
	transition: background 0.2s, border-color 0.2s, box-shadow 0.2s;
	&.focused {
		background: #fff;
		border-color: $brand;
		box-shadow: 0 0 0 6rpx rgba(61, 91, 217, 0.1);
	}
}

.field-icon {
	width: 36rpx;
	height: 36rpx;
	margin-right: 20rpx;
	flex-shrink: 0;
}

.field-input {
	flex: 1;
	font-size: 30rpx;
	color: $navy;
}

/* 微信一键取号：账号框内右侧文字按钮，与手输互斥共存 */
.wx-phone-btn {
	flex-shrink: 0;
	margin-left: 16rpx;
	padding: 0 20rpx;
	height: 56rpx;
	line-height: 56rpx;
	font-size: 24rpx;
	color: $brand;
	background: rgba(61, 91, 217, 0.08);
	border: none;
	border-radius: 28rpx;
	&::after { border: none; }
}

.wx-phone-btn-hover {
	opacity: 0.7;
}

/* 取号进行中禁用态：维持原视觉，仅降低透明度 */
.wx-phone-btn[disabled] {
	color: $brand;
	background: rgba(61, 91, 217, 0.08);
	opacity: 0.6;
}

/* 密码可见性切换：睁眼=显示，闭眼=隐藏 */
.eye-btn {
	flex-shrink: 0;
	padding: 12rpx 0 12rpx 24rpx;
}

.eye-btn-hover {
	opacity: 0.6;
}

.eye-icon {
	display: block;
	width: 40rpx;
	height: 40rpx;
}

.login-btn {
	margin-top: 36rpx;
	background: linear-gradient(135deg, #4a6ae0, #1e39b1);
	color: #fff;
	font-size: 32rpx;
	font-weight: 700;
	letter-spacing: 6rpx;
	text-indent: 6rpx; /* 平衡字距，文字真居中 */
	height: 96rpx;
	line-height: 96rpx;
	border-radius: 48rpx;
	border: none;
	box-shadow: 0 10rpx 26rpx rgba(30, 57, 177, 0.32);
	transition: opacity 0.2s, box-shadow 0.2s;
	&::after { border: none; }
}

.btn-hover {
	opacity: 0.88;
	box-shadow: 0 4rpx 14rpx rgba(30, 57, 177, 0.28);
}

.login-btn[disabled] {
	background: #b9c4ea;
	color: rgba(255, 255, 255, 0.9);
	box-shadow: none;
}

.agree-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	margin-top: 32rpx;
}

.agree-txt {
	color: $muted;
	font-size: 24rpx;
}

.agree-link {
	color: $brand;
	font-size: 24rpx;
	padding: 8rpx 0; /* 扩大点击热区 */
}

.remember {
	display: flex;
	align-items: center;
	margin-top: 22rpx;
	padding: 8rpx 0; /* 扩大点击热区 */
}

.checkbox {
	width: 32rpx;
	height: 32rpx;
	border: 2rpx solid #cdd4e0;
	border-radius: 10rpx;
	margin-right: 14rpx;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	transition: background 0.15s, border-color 0.15s;
	&.checked {
		background: linear-gradient(135deg, #4a6ae0, #1e39b1);
		border-color: transparent;
	}
}

.check-mark {
	color: #fff;
	font-size: 22rpx;
	line-height: 1;
}

.remember-txt {
	color: $muted;
	font-size: 26rpx;
}

.page-footer {
	position: relative;
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: auto;
	padding-top: 56rpx; /* 内容较长时与卡片的最小间距 */
	padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 44rpx);
	animation: rise 0.5s 0.2s ease-out both;
}

/* 业务负责人：前缀为一列，联系人竖排一列，点击行拨号 */
.contact-block {
	display: flex;
	align-items: flex-start;
}

.contact-label {
	color: $muted;
	font-size: 24rpx;
	line-height: 1.7;
}

.contact-list {
	display: flex;
	flex-direction: column;
}

.contact-row {
	padding: 2rpx 8rpx;
}

.contact-hover {
	opacity: 0.6;
}

.contact-txt {
	color: $brand;
	font-size: 24rpx;
	line-height: 1.7;
}

.copyright {
	margin-top: 30rpx;
	color: $faint;
	font-size: 22rpx;
}

.version {
	margin-top: 8rpx;
	color: $faint;
	font-size: 22rpx;
	letter-spacing: 1rpx;
}
</style>
