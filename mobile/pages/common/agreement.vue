<template>
	<view class="page">
		<NavBar :title="title" title-mode="center" :border="false" />

		<view class="doc">
			<text class="doc-h">{{ title }}</text>
			<text class="doc-intro">欢迎使用安为智消云（消防远程值守系统）。在使用前，请您仔细阅读并充分理解以下条款。</text>

			<!-- 服务协议 -->
			<block v-if="type === 'service'">
				<text class="doc-sub">一、服务说明</text>
				<text class="doc-p">本小程序为消防远程值守与告警管理提供移动端服务，包括告警接收与处置、设备状态查看、值班与巡检信息查询等功能。账号由您所在单位管理员统一开设，具体服务范围以您所在单位与本公司签订的服务内容为准。</text>

				<text class="doc-sub">二、账号与安全</text>
				<text class="doc-p">您应妥善保管账号和密码，不得转借他人使用。因账号保管不善造成的损失由用户自行承担；发现账号异常或密码泄露，请立即联系业务负责人或管理员处理。</text>

				<text class="doc-sub">三、使用规范</text>
				<text class="doc-p">您承诺仅将本服务用于合法的消防管理与值守工作，不得利用本服务从事任何违法违规活动，不得干扰、破坏系统正常运行，不得以任何方式窃取或篡改系统数据。</text>

				<text class="doc-sub">四、数据说明</text>
				<text class="doc-p">本服务中展示的设备、告警、值守等数据来源于您所在单位的消防设施与业务系统，数据权利归相应权利人所有；本公司在提供服务所必需的范围内使用和保护相关数据。</text>

				<text class="doc-sub">五、免责声明</text>
				<text class="doc-p">因不可抗力、网络故障、设备故障、第三方服务中断等非本公司原因导致服务暂停或数据延迟的，本公司在法律允许的范围内不承担责任。告警信息仅作为辅助手段，不能替代现场值守与法定消防安全职责。</text>

				<text class="doc-sub">六、协议的变更与终止</text>
				<text class="doc-p">本公司可能根据业务需要更新本协议，更新后在本页面公布，您继续使用本服务即视为接受更新后的协议。如您所在单位与本公司服务关系终止，本公司有权停止相应账号的服务。</text>

				<text class="doc-sub">七、联系我们</text>
				<text class="doc-p">如对本协议有任何疑问，可通过登录页公示的业务负责人电话与我们联系。</text>
			</block>

			<!-- 隐私协议 -->
			<block v-else>
				<text class="doc-sub">一、我们收集的信息</text>
				<text class="doc-p">为提供消防远程值守服务，我们会收集以下信息：账号信息（账号、加密存储的密码）；身份信息（姓名、手机号，由管理员开设账号时录入）；业务信息（您所在单位的设备信息、告警记录、处置记录、值班与巡检数据）；运行信息（登录时间、操作日志）。</text>

				<text class="doc-sub">二、信息的使用</text>
				<text class="doc-p">上述信息仅用于：身份识别与账号登录、告警的接收与推送、告警处置与工单流转、设备与值班状态展示、服务的安全审计与故障排查。我们不会将您的信息用于与消防值守服务无关的用途。</text>

				<text class="doc-sub">三、信息的存储与保护</text>
				<text class="doc-p">您的信息存储于中国大陆境内的服务器，密码等敏感信息加密存储，传输过程采用加密通道。我们采取访问控制、日志审计等合理措施保护您的信息安全。</text>

				<text class="doc-sub">四、信息的共享与披露</text>
				<text class="doc-p">除法律法规要求、司法机关或行政机关依法要求，以及为提供服务所必需（如向您所在单位展示值守与处置记录）外，我们不会向任何第三方提供您的个人信息。</text>

				<text class="doc-sub">五、您的权利</text>
				<text class="doc-p">您可以联系所在单位管理员或业务负责人，查询、更正您的个人信息；如需注销账号，同样可通过上述渠道申请，我们将在核实后处理。</text>

				<text class="doc-sub">六、联系我们</text>
				<text class="doc-p">如对本协议或个人信息保护有任何疑问、意见或建议，请通过登录页公示的业务负责人电话与我们联系。</text>
			</block>

			<text class="doc-foot">广东安为科技有限公司</text>
		</view>
	</view>
</template>

<script>
import NavBar from '@/components/NavBar/NavBar.vue'

export default {
	components: { NavBar },
	data() {
		return {
			type: 'service',
			title: '服务协议'
		}
	},
	onLoad(options) {
		this.type = options && options.type === 'privacy' ? 'privacy' : 'service'
		this.title = this.type === 'privacy' ? '隐私协议' : '服务协议'
	}
}
</script>

<style lang="scss" scoped>
.page {
	min-height: 100vh;
	background: #fff;
}

.doc {
	padding: 36rpx 44rpx 88rpx;
}

.doc-h {
	display: block;
	text-align: center;
	color: #16213a;
	font-size: 36rpx;
	font-weight: 700;
	letter-spacing: 2rpx;
}

.doc-intro {
	display: block;
	margin-top: 28rpx;
	color: #6b7484;
	font-size: 26rpx;
	line-height: 1.7;
}

.doc-sub {
	display: block;
	margin: 36rpx 0 14rpx;
	color: #16213a;
	font-size: 28rpx;
	font-weight: 600;
}

.doc-p {
	display: block;
	color: #494f5e;
	font-size: 27rpx;
	line-height: 1.75;
}

.doc-foot {
	display: block;
	margin-top: 48rpx;
	text-align: right;
	color: #a2aab8;
	font-size: 24rpx;
}
</style>
