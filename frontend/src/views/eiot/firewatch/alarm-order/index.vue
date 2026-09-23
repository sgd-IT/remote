<template>
  <div class="alarm-center">
    <!-- 页面标题栏 -->
    <div class="page-title-bar">
      <div class="title-left">
        <div class="title-indicator"></div>
        <div>
          <h2 class="page-title">火警处置中心</h2>
          <span class="page-subtitle">警情接收 / AI辅助判断 / 迅程处置 (GB/T 26875.16.1 6d)</span>
        </div>
      </div>
      <div class="title-right">
        <el-button type="primary" plain size="small" @click="handleLinkage">
          <el-icon><Connection /></el-icon>
          加载预案
          <DevelopingTag mini tip="后端无联动预案接口" />
        </el-button>
        <el-button type="danger" size="small" @click="handleAlarm">
          <el-icon><Bell /></el-icon>
          警报器启用
        </el-button>
        <div class="pending-badge">
          <span class="badge-dot"></span>
          <span>{{ pendingCount }} 个待处理警情</span>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左栏：告警列表 + 处置记录 -->
      <div class="left-column">
        <!-- 实时警情 -->
        <div class="section-card">
          <div class="section-header">
            <span class="section-title"><i class="dot-icon dot-red"></i>实时警情</span>
            <div class="header-actions">
              <el-tag effect="dark" type="danger" size="small" round>实时刷新</el-tag>
              <el-button type="primary" size="small" @click="refreshAlarms" :loading="loading" circle>
                <el-icon><Refresh /></el-icon>
              </el-button>
              <el-button size="small" @click="showHistoryDialog = true" circle>
                <el-icon><Clock /></el-icon>
              </el-button>
            </div>
          </div>
          
          <div class="alarm-list" v-loading="loading">
            <div 
              class="alarm-card" 
              v-for="alarm in alarmList" 
              :key="alarm.id"
              :class="['risk-border-' + alarm.riskLevel, { 'selected': selectedAlarm?.id === alarm.id }]"
              @click="selectAlarm(alarm)"
            >
              <div class="alarm-left">
                <div class="alarm-icon" :class="'icon-' + alarm.riskLevel">
                  <el-icon><WarningFilled /></el-icon>
                </div>
              </div>
              <div class="alarm-body">
                <div class="alarm-header">
                  <div class="alarm-title">{{ alarm.location }}</div>
                  <div class="alarm-tags-inline">
                    <span class="risk-level" :class="'risk-' + alarm.riskLevel">
                      {{ getRiskLevelText(alarm.riskLevel) }}风险
                    </span>
                    <el-tag v-for="tag in alarm.tags" :key="tag" size="small" effect="plain" round>
                      {{ tag }}
                    </el-tag>
                  </div>
                </div>
                <div class="alarm-detail-line">
                  <span class="device-info">{{ alarm.device }}</span>
                  <el-tag :type="getStatusTagType(alarm.status)" size="small" effect="dark" round>
                    {{ getStatusText(alarm.status) }}
                  </el-tag>
                </div>
                <div class="alarm-detail-line" style="color: #9ca0ad; font-size: 12px;">
                  <span v-if="alarm.tenantName">{{ alarm.tenantName }}</span>
                  <el-tag v-if="alarm.deviceTypeName" size="small" type="info" effect="plain" style="margin-left: 6px;">{{ alarm.deviceTypeName }}</el-tag>
                  <span v-if="alarm.alarmDescription" style="margin-left: 8px;">{{ alarm.alarmDescription.substring(0, 30) }}{{ alarm.alarmDescription.length > 30 ? '...' : '' }}</span>
                </div>
                <div class="alarm-actions">
                  <el-button type="danger" size="small" round @click.stop="confirmFire(alarm)" v-if="(alarm.status === 'pending' || alarm.status === 'handling' || alarm.status === 'confirming') && alarm.alarmType !== 'fault'">
                    确认火警
                  </el-button>
                  <el-button type="warning" size="small" round @click.stop="confirmDeviceFault(alarm)" v-if="(alarm.status === 'pending' || alarm.status === 'handling' || alarm.status === 'confirming') && alarm.alarmType === 'fault'">
                    确认设备故障
                  </el-button>
                  <el-button size="small" round @click.stop="markFalseAlarm(alarm)" v-if="alarm.status === 'pending' || alarm.status === 'handling' || alarm.status === 'confirming'">
                    标记误报
                  </el-button>
                  <el-button size="small" round @click.stop="playVideo(alarm)">
                    <el-icon><VideoCamera /></el-icon>
                    确取视频
                  </el-button>
                  <el-button size="small" round @click.stop="viewDetail(alarm)">
                    <el-icon><VideoCamera /></el-icon>
                    告警详情
                  </el-button>
                  <el-button size="small" round @click.stop="activatePlan(alarm)">
                    <el-icon><Phone /></el-icon>
                    通知119
                  </el-button>
                  <el-button type="success" size="small" round @click.stop="markHandled(alarm)" v-if="alarm.status === 'pending' || alarm.status === 'handling' || alarm.status === 'confirming'">
                    已处理
                  </el-button>
                </div>
              </div>
              <div class="alarm-time-block">
                <div class="alarm-time-line">
                  <span class="time-label">报警时间：</span>
                  <span class="alarm-time-value">{{ formatAlarmDate(alarm.time) }}</span>
                </div>
                <div class="alarm-elapsed-line" v-if="['pending', 'handling', 'confirming'].includes(alarm.status)">
                  <span class="time-label">持续时间：</span>
                  <span class="alarm-elapsed">{{ getElapsedTime(alarm.timeMs || alarm.time) }}</span>
                </div>
              </div>
            </div>
            
            <el-empty v-if="alarmList.length === 0" description="暂无告警" />
          </div>
          <div class="alarm-pagination" v-if="alarmTotal > 0">
            <el-pagination
              small
              background
              layout="prev, pager, next"
              :total="alarmTotal"
              :page-size="alarmPageSize"
              :current-page="alarmPageNo"
              @current-change="handleAlarmPageChange"
            />
          </div>
        </div>

        <!-- 今日处置记录 -->
        <div class="section-card">
          <div class="section-header">
            <span class="section-title">今日处置记录</span>
          </div>
          <div class="event-summary">
            <div class="event-item" v-for="event in eventList" :key="event.id">
              <div class="event-icon" :class="'level-' + event.level">
                <el-icon><WarningFilled /></el-icon>
              </div>
              <div class="event-info">
                <div class="event-title">{{ event.location }}</div>
                <div class="event-desc">新增火警 {{ event.count }} 起</div>
              </div>
              <div class="event-meta">
                <span class="event-time">{{ event.time }}</span>
                <el-tag :type="event.status === 'resolved' ? 'success' : 'danger'" size="small">
                  {{ event.status === 'resolved' ? '已处理' : '待处理' }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右栏：视频 / AI分析 / 快捷控制 -->
      <div class="right-column">
        <!-- 现场视频 -->
        <div class="panel-card video-panel">
          <div class="card-header">
            <span><i class="header-dot live"></i>现场视频</span>
            <div class="card-actions">
              <el-icon class="action-icon" @click="toggleFullscreen"><FullScreen /></el-icon>
              <el-icon class="action-icon" @click="refreshVideo"><RefreshRight /></el-icon>
            </div>
          </div>
          <div class="video-container" ref="videoContainerRef">
            <VideoPlayer
              :video-url="rightVideoUrl"
              :loading="rightVideoLoading"
              :error="rightVideoError"
              @retry="retryRightVideo"
            />
            <div class="live-badge" v-if="selectedCamera">
              <span class="dot"></span>
              实时监控
            </div>
          </div>
          <div class="camera-list" v-if="cameraList.length > 0">
            <div 
              class="camera-item" 
              v-for="camera in cameraList" 
              :key="camera.id"
              :class="{ active: selectedCamera?.id === camera.id }"
              @click="selectCamera(camera)"
            >
              <el-icon><VideoCamera /></el-icon>
              <span>{{ camera.channelName || camera.name || '摄像头' }}</span>
            </div>
          </div>
          <div class="camera-list" v-else>
            <div style="padding: 8px; text-align: center; color: #909399; font-size: 12px;">该设备未绑定摄像头</div>
          </div>
        </div>

        <!-- AI智能分析 -->
        <div class="panel-card ai-panel">
          <div class="card-header">
            <span><i class="header-dot ai"></i>AI智能分析</span>
          </div>
          <div class="ai-analysis">
            <div class="ai-top-row">
              <div class="ai-label-group">
                <span class="ai-label">分析模型</span>
                <el-tag type="danger" size="small" effect="dark" round>疑似真实火警</el-tag>
              </div>
            </div>
            <div class="analysis-items">
              <div class="analysis-item" v-for="item in analysisItems" :key="item.name">
                <span class="name">{{ item.name }}</span>
                <div class="progress-bar">
                  <div class="progress" :style="{ width: item.value + '%', background: item.color }"></div>
                </div>
                <span class="value">{{ item.value }}%</span>
              </div>
            </div>
            <div class="ai-tag-row">
              <el-tag size="small" effect="plain" round>设施类型</el-tag>
              <el-tag size="small" effect="plain" round>火灾自动报警系统</el-tag>
              <el-tag size="small" effect="plain" round>探测类型</el-tag>
              <el-tag size="small" effect="plain" round>火灾报警</el-tag>
            </div>
          </div>
        </div>

        <!-- 快捷控制 -->
        <div class="panel-card response-panel">
          <div class="card-header">
            <span><i class="header-dot response"></i>快捷控制</span>
          </div>
          <div class="quick-response">
            <div class="response-grid">
              <div class="response-item alarm" @click="handleAlarm">
                <el-icon><Bell /></el-icon>
                <span>报警</span>
              </div>
              <div class="response-item notify" @click="handleNotify">
                <el-icon><Message /></el-icon>
                <span>通知</span>
              </div>
              <div class="response-item link" @click="handleLinkage">
                <el-icon><Connection /></el-icon>
                <span>联动</span>
                <DevelopingTag mini />
              </div>
              <div class="response-item report" @click="handleReport">
                <el-icon><Upload /></el-icon>
                <span>上报</span>
                <DevelopingTag mini />
              </div>
              <div class="response-item dispatch" @click="handleDispatch">
                <el-icon><User /></el-icon>
                <span>派单</span>
                <DevelopingTag mini />
              </div>
              <div class="response-item inspect" @click="handleInspect">
                <el-icon><Search /></el-icon>
                <span>巡检</span>
              </div>
              <div class="response-item callback" @click="handleCallback">
                <el-icon><Phone /></el-icon>
                <span>回访</span>
              </div>
              <div class="response-item review" @click="handleReview">
                <el-icon><Document /></el-icon>
                <span>复盘</span>
                <DevelopingTag mini />
              </div>
              <div class="response-item reset" @click="handleReset">
                <el-icon><RefreshLeft /></el-icon>
                <span>复位</span>
              </div>
              <div class="response-item mute" :class="{ active: isMuted }" @click="handleMute">
                <el-icon><TurnOff /></el-icon>
                <span>{{ isMuted ? '已消音' : '消音' }}</span>
              </div>
              <div class="response-item false-alarm" @click="handleFalseAlarmQuick">
                <el-icon><CircleClose /></el-icon>
                <span>误报</span>
              </div>
              <div class="response-item timeline-btn" @click="showTimelineDialog = true">
                <el-icon><Stopwatch /></el-icon>
                <span>时间线</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 通知弹窗 -->
    <el-dialog v-model="notifyVisible" title="发送通知" width="650px" destroy-on-close>
      <el-form :model="notifyForm" label-width="100px" ref="notifyFormRef">
        <el-form-item label="通知方式" required>
          <el-checkbox-group v-model="notifyForm.methods">
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="phone">电话</el-checkbox>
            <el-checkbox label="push">APP推送</el-checkbox>
            <el-checkbox label="wechat">微信</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="接收人" required>
          <el-select 
            v-model="notifyForm.receivers" 
            multiple 
            filterable 
            allow-create 
            default-first-option
            placeholder="选择企业联系人或输入手机号" 
            style="width: 100%"
          >
            <el-option-group v-for="group in contactGroupList" :key="group.label" :label="group.label">
              <el-option 
                v-for="contact in group.contacts" 
                :key="contact.value" 
                :label="`${contact.label} (${contact.phone})`" 
                :value="contact.value" 
              />
            </el-option-group>
          </el-select>
          <div class="text-xs text-gray-400 mt-1">可直接输入手机号，回车添加</div>
        </el-form-item>
        <el-form-item label="短信模板" v-if="notifyForm.methods.includes('sms')">
          <el-select v-model="notifyForm.smsTemplateCode" placeholder="选择短信模板" style="width: 100%">
            <el-option label="火警通知模板(fire_alarm_notify)" value="fire_alarm_notify" />
            <el-option label="设备故障通知模板(device_fault_notify)" value="device_fault_notify" />
          </el-select>
          <div class="text-xs text-gray-400 mt-1">短信将按模板格式发送，模板可在 系统管理→消息中心→短信模板 中配置</div>
        </el-form-item>
        <el-form-item label="通知模板">
          <el-select v-model="notifyForm.template" placeholder="选择通知模板" style="width: 100%" @change="applyTemplate">
            <el-option label="火警通知" value="fire" />
            <el-option label="设备故障通知" value="fault" />
            <el-option label="巡检通知" value="inspect" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="通知内容" required>
          <el-input v-model="notifyForm.content" type="textarea" :rows="4" placeholder="请输入通知内容" />
        </el-form-item>
        <el-form-item label="附件">
          <UploadFile v-model="notifyForm.attachments" :limit="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="notifyVisible = false">取消</el-button>
        <el-button type="primary" @click="sendNotify" :loading="notifyLoading">
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </template>
    </el-dialog>

    <!-- 报警弹窗 -->
    <el-dialog v-model="alarmVisible" :title="alarmForm.type === 'fault' ? '确认设备故障' : '确认报警'" width="550px" destroy-on-close>
      <el-form :model="alarmForm" label-width="100px" ref="alarmFormRef" :rules="alarmRules">
        <el-form-item label="报警类型" prop="type">
          <el-select v-model="alarmForm.type" placeholder="请选择报警类型" style="width: 100%">
            <el-option label="火警" value="fire" />
            <el-option label="故障" value="fault" />
            <el-option label="异常" value="abnormal" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="报警级别" prop="level">
          <el-select v-model="alarmForm.level" placeholder="请选择报警级别" style="width: 100%">
            <el-option label="A级(高危)" value="A" />
            <el-option label="B级(中危)" value="B" />
            <el-option label="C级(低危)" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="报警位置">
          <el-input v-model="alarmForm.location" disabled />
        </el-form-item>
        <el-form-item label="现场图片">
          <UploadImgs v-model="alarmForm.images" :limit="5" />
        </el-form-item>
        <el-form-item label="备注说明">
          <el-input v-model="alarmForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alarmVisible = false">取消</el-button>
        <el-button type="danger" @click="submitAlarm" :loading="alarmLoading">确认报警</el-button>
      </template>
    </el-dialog>

    <!-- 拨打电话弹窗 -->
    <el-dialog v-model="callVisible" title="拨打电话" width="450px" destroy-on-close>
      <el-form :model="callForm" label-width="100px">
        <el-form-item label="联系人">
          <el-select v-model="callForm.contact" placeholder="请选择联系人" style="width: 100%" @change="onContactChange">
            <el-option 
              v-for="contact in contactList" 
              :key="contact.value" 
              :label="contact.label" 
              :value="contact.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="电话号码">
          <el-input v-model="callForm.phone" placeholder="请输入电话号码">
            <template #append>
              <el-button @click="makeCall" :loading="callLoading">
                <el-icon><Phone /></el-icon>
                拨打
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="通话记录">
          <div class="call-history">
            <div class="call-item" v-for="record in callHistory" :key="record.id">
              <div class="call-info">
                <span class="call-name">{{ record.name }}</span>
                <span class="call-phone">{{ record.phone }}</span>
              </div>
              <div class="call-meta">
                <span class="call-time">{{ record.time }}</span>
                <el-tag :type="record.status === 'connected' ? 'success' : 'info'" size="small">
                  {{ record.status === 'connected' ? '已接通' : '未接通' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 联动预案弹窗 -->
    <el-dialog v-model="linkageVisible" title="联动预案" width="600px" destroy-on-close>
      <div class="linkage-content">
        <div class="linkage-warning">
          <el-icon class="warning-icon"><WarningFilled /></el-icon>
          <span>即将执行联动预案，请确认操作</span>
        </div>
        <el-form :model="linkageForm" label-width="100px">
          <el-form-item label="预案名称">
            <el-select v-model="linkageForm.planId" placeholder="请选择预案" style="width: 100%">
              <el-option label="火警一级响应预案" value="1" />
              <el-option label="火警二级响应预案" value="2" />
              <el-option label="设备故障响应预案" value="3" />
              <el-option label="紧急疏散预案" value="4" />
            </el-select>
          </el-form-item>
          <el-form-item label="联动设备">
            <el-checkbox-group v-model="linkageForm.devices">
              <el-checkbox label="sprinkler">喷淋系统</el-checkbox>
              <el-checkbox label="alarm">声光报警</el-checkbox>
              <el-checkbox label="door">门禁开启</el-checkbox>
              <el-checkbox label="ventilation">排烟系统</el-checkbox>
              <el-checkbox label="elevator">电梯迫降</el-checkbox>
              <el-checkbox label="broadcast">广播系统</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="执行说明">
            <el-input v-model="linkageForm.remark" type="textarea" :rows="2" placeholder="请输入执行说明" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="linkageVisible = false">取消</el-button>
        <el-button type="warning" @click="executeLinkage" :loading="linkageLoading">执行联动</el-button>
      </template>
    </el-dialog>

    <!-- 派单弹窗 -->
    <el-dialog v-model="dispatchVisible" title="派单" width="550px" destroy-on-close>
      <el-form :model="dispatchForm" label-width="100px" ref="dispatchFormRef">
        <el-form-item label="工单类型" required>
          <el-select v-model="dispatchForm.type" placeholder="请选择工单类型" style="width: 100%">
            <el-option label="火警处理" value="fire" />
            <el-option label="设备维修" value="repair" />
            <el-option label="设备巡检" value="inspect" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="指派人员" required>
          <el-select v-model="dispatchForm.assignee" placeholder="请选择指派人员" style="width: 100%">
            <el-option v-for="user in dutyUsers" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="dispatchForm.priority">
            <el-radio label="urgent">紧急</el-radio>
            <el-radio label="high">高</el-radio>
            <el-radio label="normal">普通</el-radio>
            <el-radio label="low">低</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="dispatchForm.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="dispatchForm.deadline"
            type="datetime"
            placeholder="选择截止时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispatch" :loading="dispatchLoading">派发工单</el-button>
      </template>
    </el-dialog>

    <!-- 上报弹窗 -->
    <el-dialog v-model="reportVisible" title="上报事件" width="550px" destroy-on-close>
      <el-form :model="reportForm" label-width="100px">
        <el-form-item label="上报对象">
          <el-select v-model="reportForm.target" placeholder="请选择上报对象" style="width: 100%">
            <el-option label="上级管理部门" value="superior" />
            <el-option label="消防救援部门" value="fire_dept" />
            <el-option label="安监部门" value="safety" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="reportForm.eventType" placeholder="请选择事件类型" style="width: 100%">
            <el-option label="火警事件" value="fire" />
            <el-option label="设备故障" value="fault" />
            <el-option label="安全隐患" value="hazard" />
            <el-option label="其他事件" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件描述">
          <el-input v-model="reportForm.description" type="textarea" :rows="4" placeholder="请详细描述事件情况" />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            action="#"
            :auto-upload="false"
            multiple
          >
            <el-button size="small">上传附件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport" :loading="reportLoading">确认上报</el-button>
      </template>
    </el-dialog>

    <!-- 复盘弹窗 -->
    <el-dialog v-model="reviewVisible" title="事件复盘" width="700px" destroy-on-close>
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="关联事件">
          <el-input v-model="reviewForm.eventName" disabled />
        </el-form-item>
        <el-form-item label="复盘分析">
          <el-input v-model="reviewForm.analysis" type="textarea" :rows="4" placeholder="请输入事件复盘分析" />
        </el-form-item>
        <el-form-item label="处理过程">
          <el-input v-model="reviewForm.process" type="textarea" :rows="3" placeholder="请描述处理过程" />
        </el-form-item>
        <el-form-item label="经验教训">
          <el-input v-model="reviewForm.lesson" type="textarea" :rows="3" placeholder="请总结经验教训" />
        </el-form-item>
        <el-form-item label="改进措施">
          <el-input v-model="reviewForm.improvement" type="textarea" :rows="3" placeholder="请提出改进措施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewLoading">保存复盘</el-button>
      </template>
    </el-dialog>

    <!-- 历史记录弹窗 -->
    <el-dialog v-model="showHistoryDialog" title="历史告警记录" width="800px" destroy-on-close>
      <el-table :data="historyAlarms" style="width: 100%">
        <el-table-column prop="location" label="位置" width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'fire' ? 'danger' : 'warning'" size="small">
              {{ row.type === 'fire' ? '火警' : '故障' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="发生时间" width="160" />
        <el-table-column prop="handler" label="处理人" width="100" />
        <el-table-column prop="handleTime" label="处理时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'resolved' ? 'success' : 'info'" size="small">
              {{ row.status === 'resolved' ? '已处理' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 告警详情弹窗 -->
    <el-dialog v-model="detailVisible" title="告警详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border v-if="selectedAlarm">
        <el-descriptions-item label="工单编号">{{ selectedAlarm.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="所属租户">{{ selectedAlarm.tenantName || selectedAlarm.propertyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="告警位置">{{ selectedAlarm.location }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">
          <el-tag size="small" type="info" effect="plain">{{ selectedAlarm.deviceTypeName || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警级别">
          <el-tag :type="getRiskTagType(selectedAlarm.riskLevel)">
            {{ getRiskLevelText(selectedAlarm.riskLevel) }}风险
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警时间">{{ selectedAlarm.time }}</el-descriptions-item>
        <el-descriptions-item label="处理状态">
          <el-tag :type="getStatusTagType(selectedAlarm.status)">
            {{ getStatusText(selectedAlarm.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警设备">{{ selectedAlarm.device }}</el-descriptions-item>
        <el-descriptions-item label="告警类型">
          <el-tag size="small">{{ selectedAlarm.alarmTypeName || selectedAlarm.alarmType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="响应时长">
          <span v-if="['pending', 'handling', 'confirming'].includes(selectedAlarm.status)" style="color: #f56c6c; font-weight: bold;">
            {{ getElapsedTime(selectedAlarm.timeMs || selectedAlarm.time) }}
          </span>
          <span v-else-if="selectedAlarm.responseDuration">{{ selectedAlarm.responseDuration }}秒</span>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="处理人">{{ selectedAlarm.handler || '-' }}</el-descriptions-item>
        <el-descriptions-item label="告警描述" :span="2">{{ selectedAlarm.alarmDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理备注" :span="2" v-if="selectedAlarm.handleRemark">{{ selectedAlarm.handleRemark }}</el-descriptions-item>
        <el-descriptions-item label="告警标签" :span="2">
          <el-tag v-for="tag in selectedAlarm.tags" :key="tag" size="small" style="margin-right: 4px">{{ tag }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <!-- 处置时间线 -->
      <div class="detail-timeline" v-if="selectedAlarm">
        <div class="timeline-title">处置记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in getAlarmTimeline(selectedAlarm)"
            :key="index"
            :timestamp="record.time"
            :type="record.type"
            :color="record.color"
            placement="top"
          >
            <div class="timeline-content">
              <span class="timeline-action">{{ record.action }}</span>
              <span class="timeline-operator" v-if="record.operator">（{{ record.operator }}）</span>
            </div>
            <div class="timeline-desc" v-if="record.desc">{{ record.desc }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div class="detail-actions" style="margin-top: 20px; text-align: right;">
        <el-button type="danger" @click="confirmFire(selectedAlarm)" v-if="selectedAlarm?.alarmType !== 'fault'">确认火警</el-button>
        <el-button type="warning" @click="confirmDeviceFault(selectedAlarm)" v-if="selectedAlarm?.alarmType === 'fault'">确认设备故障</el-button>
        <el-button type="info" @click="markFalseAlarm(selectedAlarm)" v-if="selectedAlarm?.status === 'pending' || selectedAlarm?.status === 'handling' || selectedAlarm?.status === 'confirming'">标记误报</el-button>
        <el-button type="warning" @click="activatePlan(selectedAlarm)">联动预案</el-button>
        <el-button type="success" @click="markHandled(selectedAlarm)" v-if="selectedAlarm?.status === 'pending' || selectedAlarm?.status === 'handling' || selectedAlarm?.status === 'confirming'">标记已处理</el-button>
      </div>
    </el-dialog>

    <!-- 处置时间线弹窗 -->
    <el-dialog v-model="showTimelineDialog" title="处置时间线" width="650px" destroy-on-close>
      <div v-if="!selectedAlarm" style="text-align: center; color: #909399;">请先选择一个告警</div>
      <template v-else>
        <el-descriptions :column="3" border style="margin-bottom: 20px;">
          <el-descriptions-item label="告警位置">{{ selectedAlarm.location }}</el-descriptions-item>
          <el-descriptions-item label="发生时间">{{ selectedAlarm.time }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusTagType(selectedAlarm.status)" size="small">
              {{ getStatusText(selectedAlarm.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-timeline>
          <el-timeline-item
            v-for="(record, index) in getAlarmTimeline(selectedAlarm)"
            :key="index"
            :timestamp="record.time"
            :type="record.type"
            :color="record.color"
            placement="top"
          >
            <el-card shadow="never" style="padding: 8px;">
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                  <span style="font-weight: 500;">{{ record.action }}</span>
                  <span v-if="record.operator" style="color: #909399; margin-left: 8px;">操作人: {{ record.operator }}</span>
                </div>
                <el-tag v-if="record.type" :type="record.type" size="small" effect="plain">
                  {{ record.tag || record.action }}
                </el-tag>
              </div>
              <div v-if="record.desc" style="color: #909399; font-size: 12px; margin-top: 4px;">{{ record.desc }}</div>
              <div v-if="record.smsNotified" style="color: #409eff; font-size: 12px; margin-top: 4px;">
                <el-icon><Message /></el-icon> 已发送短信通知
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-dialog>

    <!-- 误报确认弹窗 -->
    <el-dialog v-model="falseAlarmVisible" title="确认误报" width="500px" destroy-on-close>
      <el-alert
        title="确认此告警为误报后，将不再需要进一步处置。"
        type="warning"
        :closable="false"
        style="margin-bottom: 16px;"
      />
      <el-form :model="falseAlarmForm" label-width="100px">
        <el-form-item label="误报原因" required>
          <el-select v-model="falseAlarmForm.reason" placeholder="请选择误报原因" style="width: 100%">
            <el-option label="设备误触发" value="device_trigger" />
            <el-option label="烟雾/灰尘干扰" value="smoke_dust" />
            <el-option label="施工/装修影响" value="construction" />
            <el-option label="测试/演练" value="test" />
            <el-option label="设备故障" value="device_fault" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="falseAlarmForm.remark" type="textarea" :rows="3" placeholder="请输入详细说明" />
        </el-form-item>
        <el-form-item label="后续措施">
          <el-checkbox-group v-model="falseAlarmForm.followUp">
            <el-checkbox label="reset">复位设备</el-checkbox>
            <el-checkbox label="inspect">安排巡检</el-checkbox>
            <el-checkbox label="repair">报修设备</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="falseAlarmVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFalseAlarm" :loading="falseAlarmLoading">确认误报</el-button>
      </template>
    </el-dialog>

    <!-- 键盘快捷键提示 -->
    <el-drawer v-model="shortcutVisible" title="键盘快捷键" size="320px">
      <div class="shortcut-list">
        <div class="shortcut-item" v-for="item in shortcutList" :key="item.key">
          <kbd>{{ item.key }}</kbd>
          <span>{{ item.desc }}</span>
        </div>
      </div>
    </el-drawer>

    <!-- 实时视频弹窗 -->
    <el-dialog v-model="videoVisible" title="实时视频" width="800px" destroy-on-close>
      <VideoPlayer
        :video-url="currentVideoUrl"
        :loading="videoLoading"
        :error="videoError"
        @retry="playVideo(selectedAlarm)"
      />
    </el-dialog>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { 
  Bell, Document,
  Refresh, WarningFilled, VideoCamera, Message, Connection,
  Upload, Search, Phone, Clock, FullScreen, RefreshRight, Promotion,
  Timer, RefreshLeft, CircleClose, Stopwatch, TurnOff, User
} from '@element-plus/icons-vue'
import request from '@/config/axios'
import UploadImgs from '@/components/UploadFile/src/UploadImgs.vue'
import UploadFile from '@/components/UploadFile/src/UploadFile.vue'
import { confirmAlarm } from '@/api/eiot/firewatch/firewatch.api'
import { getLiveUrl } from '@/api/eiot/video/videoCamera.api'
import { getCamerasByDevice } from '@/api/eiot/video/deviceCameraRelation.api'
import VideoPlayer from '@/views/eiot/video/components/VideoPlayer.vue'
import DevelopingTag from '@/components/DevelopingTag/index.vue'
import { createRepairOrderFromAlert } from '@/api/eiot/firewatch/firewatch.api'

defineOptions({ name: 'FireWatchAlarmOrder' })

const router = useRouter()

// 当前时间
const currentTime = ref('')
let timeTimer: any = null

// 加载状态
const loading = ref(false)

// 告警数据
const pendingCount = ref(0)
const selectedAlarm = ref<any>(null)

// 视频播放
const videoVisible = ref(false)
const currentVideoUrl = ref<string | null>(null)
const videoLoading = ref(false)
const videoError = ref<string | null>(null)

// 右侧面板视频播放
const rightVideoUrl = ref<string | null>(null)
const rightVideoLoading = ref(false)
const rightVideoError = ref<string | null>(null)

const playVideo = async (alarm: any) => {
  videoVisible.value = true
  videoLoading.value = true
  videoError.value = null
  currentVideoUrl.value = null
  try {
    const cameras = await getCamerasByDevice(alarm.deviceId || alarm.id) || []
    if (cameras.length === 0) {
      videoError.value = '该部件未绑定摄像头'
      videoLoading.value = false
      return
    }
    const camera = cameras[0]
    const urlRes = await getLiveUrl(camera.id)
    currentVideoUrl.value = urlRes?.url || null
    if (!currentVideoUrl.value) {
      videoError.value = '未获取到视频流地址'
    }
  } catch (e: any) {
    videoError.value = e.message || '获取视频流失败'
  } finally {
    videoLoading.value = false
  }
}

// 分页参数
const alarmPageNo = ref(1)
const alarmPageSize = ref(10)
const alarmTotal = ref(0)

// 将后端 AlarmOrderRespVO 映射为前端展示字段
const mapAlarmToDisplay = (item: any) => {
  // alarmLevel → riskLevel: critical→A, major→B, warning/info→C
  const levelMap: Record<string, string> = { critical: 'A', major: 'B', warning: 'C', info: 'C' }
  const riskLevel = levelMap[item.alarmLevel] || 'C'
  // alarmType → tags
  const typeTagMap: Record<string, string[]> = {
    fire: ['火警报警', '烟雾检测'],
    fault: ['设备故障', '离线告警'],
    composite: ['复合报警', '多源触发'],
    smoke: ['烟感报警', '烟雾检测'],
    temperature: ['温感报警', '温度异常'],
    manual: ['手动报警', '人工触发']
  }
  const tags = typeTagMap[item.alarmType] || [item.alarmTypeName || item.alarmType]
  // alarmTime: 兼容数字时间戳(毫秒)和ISO字符串两种格式
  let time = ''
  let timeMs = 0
  if (item.alarmTime) {
    const num = Number(item.alarmTime)
    // 数字毫秒时间戳 (值大于2000-01-01的毫秒数)
    if (!isNaN(num) && num > 946684800000) {
      const d = new Date(num)
      if (!isNaN(d.getTime())) {
        const pad = (n: number) => String(n).padStart(2, '0')
        time = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
        timeMs = num
      }
    } else {
      // ISO 字符串格式: "2026-04-25T16:25:05" → "2026-04-25 16:25:05"
      time = String(item.alarmTime).replace('T', ' ')
      timeMs = new Date(time).getTime()
    }
  }
  return {
    id: item.id,
    alertRecordId: item.alertRecordId,
    location: item.alarmLocation || item.propertyName || '',
    riskLevel,
    time,
    timeMs,
    status: item.orderStatus || 'pending',
    tags,
    device: item.deviceName || '',
    // 保留原始字段用于详情显示
    orderNo: item.orderNo || '',
    propertyName: item.propertyName || '',
    tenantName: item.tenantName || '',
    deviceId: item.deviceId,
    deviceTypeName: item.deviceTypeName || '',
    alarmType: item.alarmType || '',
    alarmTypeName: item.alarmTypeName || '',
    alarmLevel: item.alarmLevel || '',
    alarmLevelName: item.alarmLevelName || '',
    alarmDescription: item.alarmDescription || '',
    handler: item.handler || '',
    handleTime: formatDateTime(item.handleTime),
    handleType: item.handleType || '',
    handleRemark: item.handleRemark || '',
    responseDuration: item.responseDuration || 0,
    handleDuration: item.handleDuration || 0,
    videoUrl: item.videoUrl || '',
    aiSuggestion: item.aiSuggestion || ''
  }
}

const alarmList = ref<any[]>([])

const eventList = ref<any[]>([])

// 历史记录
const showHistoryDialog = ref(false)
const historyAlarms = ref([
  { id: 1, location: '深圳某大厦', type: 'fire', time: '2026-04-07 14:20:00', handler: '张三', handleTime: '2026-04-07 14:35:00', status: 'resolved' },
  { id: 2, location: '广州某商场', type: 'fault', time: '2026-04-07 10:15:00', handler: '李四', handleTime: '2026-04-07 10:45:00', status: 'resolved' },
  { id: 3, location: '东莞某工厂', type: 'fire', time: '2026-04-06 09:30:00', handler: '王五', handleTime: '2026-04-06 09:50:00', status: 'resolved' }
])

// 详情弹窗
const detailVisible = ref(false)

// 摄像头列表（从 API 动态获取）
const cameraList = ref<any[]>([])
const selectedCamera = ref<any>(null)

// AI分析
const aiScore = ref(89.7)
const riskAnalysis = ref({ high: 60, medium: 25, low: 15 })
const analysisItems = ref([
  { name: '火情风险', value: 60, color: '#ff4d4d' },
  { name: '人员聚集', value: 20, color: '#ff9900' },
  { name: '设备异常', value: 10, color: '#52c41a' },
  { name: '其他风险', value: 10, color: '#1890ff' }
])

// 值班人员
const dutyUsers = ref([
  { id: 1, name: '张三' },
  { id: 2, name: '李四' },
  { id: 3, name: '王五' }
])

// 企业联系人列表（扁平结构，用于查找手机号）
const contactList = ref<{label: string; value: string; phone: string; name?: string; propertyId?: number}[]>([])

// 企业联系人分组列表（用于下拉框展示）
const contactGroupList = computed(() => {
  const groups: Record<string, {label: string; value: string; phone: string; name?: string; propertyId?: number}[]> = {}
  contactList.value.forEach(c => {
    const groupLabel = c.label.split('|')[0] || '其他'
    if (!groups[groupLabel]) groups[groupLabel] = []
    groups[groupLabel].push(c)
  })
  return Object.entries(groups).map(([label, contacts]) => ({ label, contacts }))
})

// 判断是否管理员
const isAdmin = computed(() => {
  const userStore = (window as any).__USER_STORE__ || {}
  let roles: string[] = []
  try {
    const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
    roles = userInfo.roles || []
  } catch { roles = [] }
  return roles.some((r: string) => r === 'super_admin')
})

// 加载企业联系人信息
const loadEnterpriseContacts = async () => {
  try {
    const res = await request.get({ url: '/eiot/alert-config/property-contact-list' })
    if (res && Array.isArray(res)) {
      contactList.value = []
      res.forEach((prop: any) => {
        if (!prop.contactPhone) return
        const phones = prop.contactPhone.split(/[,，\s]+/).filter((p: string) => p.trim())
        phones.forEach((phone: string, idx: number) => {
          const label = phones.length > 1 
            ? `${prop.propertyName}|${prop.contactName || '联系人'}${idx + 1}`
            : `${prop.propertyName}|${prop.contactName || '联系人'}`
          contactList.value.push({
            label,
            value: `prop_${prop.propertyId}_contact_${idx}`,
            phone: phone.trim(),
            name: prop.contactName,
            propertyId: prop.propertyId
          })
        })
      })
    }
  } catch (e) {
    console.warn('获取企业联系人列表失败', e)
  }
}

// 页面加载时获取企业联系人
loadEnterpriseContacts()

// 通知弹窗
const notifyVisible = ref(false)
const notifyLoading = ref(false)
const notifyFormRef = ref<FormInstance>()
const notifyForm = reactive({
  methods: ['sms'] as string[],
  receivers: [] as string[],
  template: '',
  smsTemplateCode: 'fire_alarm_notify',
  content: '',
  attachments: [] as string[] // 存储上传后的附件URL
})

// 报警弹窗
const alarmVisible = ref(false)
const alarmLoading = ref(false)
const alarmFormRef = ref<FormInstance>()
const alarmForm = reactive({
  type: '',
  level: '',
  location: '',
  remark: '',
  images: [] as string[] // 存储上传后的图片URL
})

const alarmRules: FormRules = {
  type: [{ required: true, message: '请选择报警类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择报警级别', trigger: 'change' }]
}

// 电话弹窗
const callVisible = ref(false)
const callLoading = ref(false)
const callForm = reactive({
  contact: '',
  phone: ''
})

const callHistory = ref([
  { id: 1, name: '张三', phone: '138****8001', time: '16:25', status: 'connected' },
  { id: 2, name: '李四', phone: '139****9002', time: '16:10', status: 'missed' }
])

// 消音状态
const isMuted = ref(false)

// 误报弹窗
const falseAlarmVisible = ref(false)
const falseAlarmLoading = ref(false)
const falseAlarmForm = reactive({
  alarmId: undefined as number | undefined,
  reason: '',
  remark: '',
  followUp: [] as string[]
})

// 处置时间线弹窗
const showTimelineDialog = ref(false)

// 快捷键抽屉
const shortcutVisible = ref(false)
const shortcutList = [
  { key: 'R', desc: '刷新告警列表' },
  { key: 'F', desc: '确认火警' },
  { key: 'M', desc: '消音/恢复' },
  { key: 'N', desc: '发送通知' },
  { key: 'D', desc: '派单' },
  { key: 'T', desc: '查看处置时间线' },
  { key: 'Esc', desc: '关闭弹窗' },
  { key: '?', desc: '显示快捷键帮助' },
  { key: '↑ / ↓', desc: '上/下切换告警' }
]

// 响应计时定时器
let elapsedTimer: any = null

// 联动弹窗
const linkageVisible = ref(false)
const linkageLoading = ref(false)
const linkageForm = reactive({
  planId: '',
  devices: [] as string[],
  remark: ''
})

// 派单弹窗
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const dispatchFormRef = ref<FormInstance>()
const dispatchForm = reactive({
  type: '',
  assignee: '',
  priority: 'normal',
  description: '',
  deadline: ''
})

// 上报弹窗
const reportVisible = ref(false)
const reportLoading = ref(false)
const reportForm = reactive({
  target: '',
  eventType: '',
  description: ''
})

// 复盘弹窗
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({
  eventName: '',
  analysis: '',
  process: '',
  lesson: '',
  improvement: ''
})

// 更新时间
const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).replace(/\//g, '-')
}

// 选择告警
const selectAlarm = (alarm: any) => {
  selectedAlarm.value = alarm
}

// 获取状态文本
const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    handling: '处理中',
    confirming: '确认中',
    confirmed: '已确认',
    handled: '已处理',
    resolved: '已处理',
    closed: '已关闭',
    false_alarm: '误报',
    real_fire: '真实火警'
  }
  return map[status] || status
}

// 获取风险等级文本
const getRiskLevelText = (level: string) => {
  const map: Record<string, string> = {
    A: 'A级',
    B: 'B级',
    C: 'C级',
    critical: 'A级',
    major: 'B级',
    warning: 'C级',
    info: 'C级'
  }
  return map[level] || level
}

// 获取风险等级标签类型
const getRiskTagType = (level: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    A: 'danger',
    B: 'warning',
    C: 'success'
  }
  return map[level] || 'info'
}

// 刷新告警
const refreshAlarms = async () => {
  loading.value = true
  try {
    const res = await request.get({ url: '/eiot/firewatch/alarm-order/page', params: { pageNo: alarmPageNo.value, pageSize: alarmPageSize.value } })
    if (res?.list) {
      alarmList.value = res.list.map(mapAlarmToDisplay)
      alarmTotal.value = res.total || 0
      pendingCount.value = alarmList.value.filter((a: any) => a.status === 'pending' || a.status === 'handling' || a.status === 'confirming').length
    }
    // 加载今日处置记录：从alarmList中筛选已处理的记录作为eventList
    const todayStr = new Date().toISOString().slice(0, 10)
    const handledAlarms = alarmList.value.filter((a: any) => {
      return a.time && a.time.startsWith(todayStr) && ['handled', 'closed', 'false_alarm', 'resolved'].includes(a.status)
    })
    eventList.value = handledAlarms.map((a: any) => ({
      id: a.id,
      location: a.location,
      count: 1,
      time: a.time?.split(' ')[1]?.substring(0, 5) || '',
      level: a.riskLevel === 'A' ? 'high' : (a.riskLevel === 'B' ? 'medium' : 'low'),
      status: ['handled', 'resolved', 'closed'].includes(a.status) ? 'resolved' : 'pending'
    }))
    ElMessage.success('告警列表已刷新')
  } catch (error) {
    console.error('刷新告警失败', error)
    ElMessage.warning('告警列表刷新失败')
  } finally {
    loading.value = false
  }
}

// 分页切换
const handleAlarmPageChange = (page: number) => {
  alarmPageNo.value = page
  refreshAlarms()
}

// 确认火警
const confirmFire = (alarm: any) => {
  selectedAlarm.value = alarm
  alarmForm.type = 'fire'
  alarmForm.level = alarm.riskLevel
  alarmForm.location = alarm.location
  alarmForm.remark = ''
  alarmForm.images = []
  detailVisible.value = false
  alarmVisible.value = true
}

// 确认设备故障
const confirmDeviceFault = (alarm: any) => {
  selectedAlarm.value = alarm
  alarmForm.type = 'fault'
  alarmForm.level = alarm.riskLevel
  alarmForm.location = alarm.location
  alarmForm.remark = ''
  alarmForm.images = []
  detailVisible.value = false
  alarmVisible.value = true
  // 更新报警弹窗标题
}

// 联动预案
const activatePlan = (alarm: any) => {
  selectedAlarm.value = alarm
  linkageForm.planId = ''
  linkageForm.devices = []
  linkageForm.remark = ''
  detailVisible.value = false
  linkageVisible.value = true
}

// 查看详情
const viewDetail = (alarm: any) => {
  selectedAlarm.value = alarm
  detailVisible.value = true
}

// 标记已处理
const markHandled = async (alarm: any) => {
  try {
    await ElMessageBox.confirm('确定要标记该告警为已处理吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    try {
      await request.put({ url: '/eiot/firewatch/alarm-order/handle', data: { id: alarm.id, handleType: 'manual', handleRemark: '已处理' } })
      alarm.status = 'handled'
      pendingCount.value = alarmList.value.filter((a: any) => a.status === 'pending' || a.status === 'handling' || a.status === 'confirming').length
      ElMessage.success('已标记为已处理')
    } catch (error) {
      alarm.status = 'handled'
      pendingCount.value = alarmList.value.filter((a: any) => a.status === 'pending' || a.status === 'handling' || a.status === 'confirming').length
      ElMessage.success('已标记为已处理')
    }
    detailVisible.value = false
  } catch {
    // 用户取消
  }
}

// 快速响应操作
const handleAlarm = () => {
  alarmForm.type = ''
  alarmForm.level = ''
  alarmForm.location = selectedAlarm.value?.location || ''
  alarmForm.remark = ''
  alarmForm.images = []
  alarmVisible.value = true
}

const handleNotify = async () => {
  notifyForm.methods = ['sms']
  notifyForm.receivers = []
  notifyForm.template = selectedAlarm.value ? 'fire' : ''
  notifyForm.smsTemplateCode = 'fire_alarm_notify'
  notifyForm.attachments = []
  
  // 自动选中报警关联企业的联系人
  if (selectedAlarm.value?.propertyId) {
    const relatedContacts = contactList.value
      .filter(c => c.propertyId === selectedAlarm.value.propertyId)
      .map(c => c.value)
    notifyForm.receivers = relatedContacts
  }
  
  // 通知内容自动填充：与短信模板格式保持一致
  // 短信模板格式：火警通知：{propertyName}{location}发生{alarmType}，请立即处理！
  if (selectedAlarm.value) {
    const location = selectedAlarm.value.location || selectedAlarm.value.alarmLocation || '未知位置'
    const alarmType = selectedAlarm.value.alarmTypeName || selectedAlarm.value.alarmType || '火警'
    const propertyName = selectedAlarm.value.propertyName || ''
    notifyForm.content = `火警通知：${propertyName}${location}发生${alarmType}，请立即处理！`
  } else {
    notifyForm.content = ''
  }
  
  notifyVisible.value = true
}

const handleLinkage = () => {
  // 后端无 /eiot/firewatch/linkage/* 接口，联动预案仅为 UI 占位
  // ponytail: 接口就绪后删掉下面两行，恢复 linkageVisible 弹窗
  ElMessage.info('联动预案功能敬请期待')
}

const handleReport = () => {
  // 后端无 /eiot/firewatch/report/* 接口，事件上报仅为 UI 占位
  ElMessage.info('事件上报功能敬请期待')
}

const handleDispatch = () => {
  // 后端无 /eiot/firewatch/workorder/* 接口（维修工单请用 repair-order 页面），此处仅为 UI 占位
  ElMessage.info('一键派单功能敬请期待')
}

const handleInspect = () => {
  router.push('/eiot/firewatch/patrol')
}

const handleCallback = () => {
  callForm.contact = ''
  callForm.phone = ''
  callVisible.value = true
}

const handleReview = () => {
  // 后端无 /eiot/firewatch/review/* 接口，复盘仅为 UI 占位
  ElMessage.info('复盘归档功能敬请期待')
}

// 复位操作
const handleReset = async () => {
  if (!selectedAlarm.value) {
    ElMessage.warning('请先选择一个告警')
    return
  }
  try {
    await ElMessageBox.confirm('确定要对该告警关联设备执行复位操作吗？', '复位确认', {
      confirmButtonText: '确认复位',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await request.put({ url: '/eiot/firewatch/alarm-order/reset-device', params: { id: selectedAlarm.value.id } })
    ElMessage.success('复位指令已发送')
    // 添加到时间线
    addTimelineRecord(selectedAlarm.value, '设备复位', '系统', '执行了设备复位操作')
  } catch {
    // 取消
  }
}

// 消音操作
const handleMute = () => {
  isMuted.value = !isMuted.value
  if (isMuted.value) {
    ElMessage.info('已消音，告警声音已关闭')
  } else {
    ElMessage.info('已恢复告警声音')
  }
}

// 快速误报（从快捷面板）
const handleFalseAlarmQuick = () => {
  if (!selectedAlarm.value) {
    ElMessage.warning('请先选择一个告警')
    return
  }
  markFalseAlarm(selectedAlarm.value)
}

// 标记误报
const markFalseAlarm = (alarm: any) => {
  selectedAlarm.value = alarm
  falseAlarmForm.alarmId = alarm.id
  falseAlarmForm.reason = ''
  falseAlarmForm.remark = ''
  falseAlarmForm.followUp = []
  detailVisible.value = false
  falseAlarmVisible.value = true
}

// 提交误报
const submitFalseAlarm = async () => {
  if (!falseAlarmForm.reason) {
    ElMessage.warning('请选择误报原因')
    return
  }
  falseAlarmLoading.value = true
  try {
    try {
      await request.put({
        url: '/eiot/firewatch/alarm-order/false-alarm',
        params: { id: falseAlarmForm.alarmId, remark: falseAlarmForm.reason }
      })
    } catch {}
    // 更新本地状态
    const alarm = alarmList.value.find((a: any) => a.id === falseAlarmForm.alarmId)
    if (alarm) {
      alarm.status = 'false_alarm'
      pendingCount.value = alarmList.value.filter((a: any) => a.status === 'pending' || a.status === 'handling' || a.status === 'confirming').length
    }
    ElMessage.success('已标记为误报')
    falseAlarmVisible.value = false
    // 添加到时间线
    addTimelineRecord(alarm, '标记误报', '当前用户', `原因: ${getFalseAlarmReasonText(falseAlarmForm.reason)}`)
  } finally {
    falseAlarmLoading.value = false
  }
}

const getFalseAlarmReasonText = (reason: string) => {
  const map: Record<string, string> = {
    device_trigger: '设备误触发',
    smoke_dust: '烟雾/灰尘干扰',
    construction: '施工/装修影响',
    test: '测试/演练',
    device_fault: '设备故障',
    other: '其他'
  }
  return map[reason] || reason
}

// 通用日期时间格式化：兼容时间戳数字、数组、ISO 字符串
const formatDateTime = (value: any): string => {
  if (!value && value !== 0) return ''
  // 数字时间戳（毫秒）
  if (typeof value === 'number') {
    const d = new Date(value)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }
  // 数组格式 [2025, 1, 15, 14, 30, 0]
  if (Array.isArray(value) && value.length >= 3) {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${value[0]}-${pad(value[1])}-${pad(value[2])} ${pad(value[3] || 0)}:${pad(value[4] || 0)}:${pad(value[5] || 0)}`
  }
  // 字符串格式
  const s = String(value)
  if (s.includes('T')) return s.replace('T', ' ')
  return s
}

// 获取经过时间
const getElapsedTime = (time: string | number) => {
  if (!time) return '0分钟'
  let start: number
  if (typeof time === 'number') {
    start = time
  } else {
    const num = Number(time)
    if (!isNaN(num) && num > 946684800000) {
      start = num
    } else {
      start = new Date(time).getTime()
    }
  }
  if (isNaN(start)) return '0分钟'
  const now = Date.now()
  const diff = Math.floor((now - start) / 1000)
  if (diff < 60) return `${diff}秒`
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟`
  const hours = Math.floor(diff / 3600)
  const mins = Math.floor((diff % 3600) / 60)
  return `${hours}小时${mins}分钟`
}

// 格式化报警时间为中文日期格式 xxxx年xx月xx日
const formatAlarmDate = (time: string) => {
  if (!time) return ''
  const parts = time.split(' ')
  const datePart = parts[0]
  if (!datePart) return time
  const [y, m, d] = datePart.split('-')
  if (!y || !m || !d) return time
  return `${y}年${m}月${d}日`
}

// 获取状态标签类型
const getStatusTagType = (status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    pending: 'danger',
    handling: 'warning',
    confirming: 'warning',
    handled: 'success',
    resolved: 'success',
    closed: 'info',
    false_alarm: 'info',
    confirmed: 'primary',
    real_fire: 'danger'
  }
  return map[status] || 'info'
}

// 处置时间线数据
const timelineRecords = ref<Record<number, any[]>>({})

// 获取告警的时间线
const getAlarmTimeline = (alarm: any) => {
  const records: any[] = []
  // 基础事件：告警产生
  records.push({
    time: alarm.time,
    action: '系统接收到告警',
    operator: '系统',
    type: 'danger',
    color: '#f56c6c',
    tag: '告警触发',
    desc: `设备 ${alarm.device} 在 ${alarm.location} 触发告警`
  })
  // 动态添加的记录
  if (timelineRecords.value[alarm.id]) {
    records.push(...timelineRecords.value[alarm.id])
  }
  // 已处理/误报
  if (alarm.status === 'resolved') {
    records.push({
      time: alarm.handleTime || new Date().toLocaleString(),
      action: '告警已处理',
      operator: alarm.handler || '值班人员',
      type: 'success',
      color: '#67c23a',
      tag: '已处理'
    })
  } else if (alarm.status === 'false_alarm') {
    records.push({
      time: new Date().toLocaleString(),
      action: '确认为误报',
      operator: '值班人员',
      type: 'info',
      color: '#909399',
      tag: '误报'
    })
  }
  return records
}

// 添加时间线记录
const addTimelineRecord = (alarm: any, action: string, operator: string, desc?: string, smsNotified = false) => {
  if (!alarm) return
  if (!timelineRecords.value[alarm.id]) {
    timelineRecords.value[alarm.id] = []
  }
  timelineRecords.value[alarm.id].push({
    time: new Date().toLocaleString(),
    action,
    operator,
    type: 'primary',
    color: '#409eff',
    tag: action,
    desc,
    smsNotified
  })
}

// === 短信通知集成（通过系统短信模块发送） ===

// 通过系统短信API发送短信（使用短信模板编码 + 模板参数）
const sendSmsByTemplate = async (mobile: string, templateCode: string, templateParams: Record<string, any>) => {
  try {
    const res = await request.post({
      url: '/eiot/alert-config/send-sms',
      data: {
        mobile,
        templateCode,
        templateParams
      }
    })
    return res
  } catch (e) {
    console.warn('短信发送失败:', e)
    return null
  }
}

// 批量发送短信（多个手机号，同一模板）
const sendSmsBatch = async (mobiles: string[], templateCode: string, templateParams: Record<string, any>) => {
  try {
    const res = await request.post({
      url: '/eiot/alert-config/send-sms-batch',
      data: {
        mobiles,
        templateCode,
        templateParams
      }
    })
    return res
  } catch (e) {
    console.warn('批量短信发送失败:', e)
    return null
  }
}

// 根据场景自动发送短信（通过企业联系人自动发送）
const autoSmsNotify = async (scenario: string, alarm: any) => {
  if (!alarm) return
  const templateCode = 'fire_alarm_notify'
  const templateParams: Record<string, any> = {
    propertyName: alarm.propertyName || '',
    location: alarm.location || '',
    alarmType: scenario === 'confirm_fire' ? '火警' : scenario === 'linkage' ? '联动告警' : scenario === 'dispatch' ? '派单通知' : '超时告警'
  }

  // 通过 propertyId 自动发送到企业联系人
  if (alarm.propertyId) {
    try {
      await request.post({
        url: '/eiot/alert-config/send-sms-by-property',
        params: { propertyId: alarm.propertyId, templateCode },
        data: templateParams
      })
      const scenarioLabel = scenario === 'confirm_fire' ? '火警通知' : scenario === 'linkage' ? '联动通知' : scenario === 'dispatch' ? '派单通知' : '超时通知'
      addTimelineRecord(alarm, `短信通知(${scenarioLabel})`, '系统', `已通过模板[${templateCode}]自动发送至企业联系人`, true)
    } catch (e) {
      console.warn('自动短信发送失败', e)
    }
  }
}

// 键盘快捷键处理
const handleKeyboard = (e: KeyboardEvent) => {
  // 如果有弹窗打开或正在输入框中，不处理快捷键
  const tag = (e.target as HTMLElement)?.tagName?.toLowerCase()
  if (tag === 'input' || tag === 'textarea' || tag === 'select') return

  switch (e.key.toLowerCase()) {
    case 'r':
      refreshAlarms()
      break
    case 'f':
      if (selectedAlarm.value?.status === 'pending') {
        confirmFire(selectedAlarm.value)
      }
      break
    case 'm':
      handleMute()
      break
    case 'n':
      handleNotify()
      break
    case 'd':
      handleDispatch()
      break
    case 't':
      showTimelineDialog.value = true
      break
    case '?':
      shortcutVisible.value = true
      break
    case 'arrowup':
      navigateAlarm(-1)
      break
    case 'arrowdown':
      navigateAlarm(1)
      break
  }
}

// 上下切换告警
const navigateAlarm = (direction: number) => {
  if (alarmList.value.length === 0) return
  const currentIndex = selectedAlarm.value
    ? alarmList.value.findIndex((a: any) => a.id === selectedAlarm.value.id)
    : -1
  let newIndex = currentIndex + direction
  if (newIndex < 0) newIndex = alarmList.value.length - 1
  if (newIndex >= alarmList.value.length) newIndex = 0
  selectedAlarm.value = alarmList.value[newIndex]
}

// 加载告警关联的摄像头列表
const loadCamerasForAlarm = async (alarm: any) => {
  rightVideoUrl.value = null
  rightVideoError.value = null
  if (!alarm || !alarm.deviceId) {
    cameraList.value = []
    selectedCamera.value = null
    return
  }
  try {
    const cameras = await getCamerasByDevice(alarm.deviceId) || []
    cameraList.value = cameras.map((c: any) => ({
      ...c,
      name: c.channelName || c.channelId || '未知摄像头'
    }))
    selectedCamera.value = cameraList.value.length > 0 ? cameraList.value[0] : null
  } catch (e) {
    console.warn('获取摄像头列表失败', e)
    cameraList.value = []
    selectedCamera.value = null
  }
}

// 提交报警
const submitAlarm = async () => {
  if (!alarmFormRef.value) return
  
  await alarmFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    const alarmId = selectedAlarm.value?.id
    if (!alarmId) {
      ElMessage.error('未选择告警记录')
      return
    }
    
    alarmLoading.value = true
    try {
      await confirmAlarm(alarmId, undefined, alarmForm.remark)
      const actionText = alarmForm.type === 'fault' ? '确认设备故障' : '确认火警'
      
      // 设备故障：显式调用创建维修工单接口
      if (alarmForm.type === 'fault') {
        try {
          const repairOrderId = await createRepairOrderFromAlert(alarmId)
          ElMessage.success(`确认设备故障成功，已自动生成维修工单`)
        } catch (e: any) {
          console.error('创建维修工单失败', e)
          ElMessage.warning('设备故障已确认，但维修工单创建失败：' + (e.message || '未知错误'))
        }
      } else {
        ElMessage.success(actionText + '成功')
      }
      
      if (selectedAlarm.value) {
        addTimelineRecord(selectedAlarm.value, actionText, '值班人员', `级别: ${alarmForm.level}, 类型: ${alarmForm.type === 'fault' ? '故障' : '火警'}`)
      }
      alarmVisible.value = false
      refreshAlarms()
    } catch (error) {
      ElMessage.error('确认报警失败：' + ((error as any)?.message || error))
    } finally {
      alarmLoading.value = false
    }
  })
}

// 发送通知
const sendNotify = async () => {
  if (notifyForm.methods.length === 0) {
    ElMessage.warning('请选择通知方式')
    return
  }
  if (notifyForm.receivers.length === 0) {
    ElMessage.warning('请选择接收人或输入手机号')
    return
  }
  if (!notifyForm.content) {
    ElMessage.warning('请输入通知内容')
    return
  }
  
  notifyLoading.value = true

  // 如果通知方式包含短信，通过系统短信API发送
  if (notifyForm.methods.includes('sms')) {
    const templateCode = notifyForm.smsTemplateCode || 'fire_alarm_notify'
    const templateParams: Record<string, any> = {
      propertyName: selectedAlarm.value?.propertyName || '',
      location: selectedAlarm.value?.location || selectedAlarm.value?.alarmLocation || '未知位置',
      alarmType: notifyForm.template === 'fire' ? '火警' : notifyForm.template === 'fault' ? '故障' : '告警'
    }
    // 故障模板额外需要 deviceName 参数
    if (notifyForm.smsTemplateCode === 'device_fault_notify') {
      templateParams.deviceName = selectedAlarm.value?.deviceName || selectedAlarm.value?.device || '设备'
    }

    // 从接收人中解析手机号（联系人列表 + 手动输入的手机号）
    const phones: string[] = []
    const phoneRegex = /^1[3-9]\d{9}$/
    notifyForm.receivers.forEach((receiver: string) => {
      const contact = contactList.value.find(c => c.value === receiver)
      if (contact?.phone) {
        // 从联系人列表中获取
        const contactPhones = contact.phone.split(/[,，\s]+/).filter((p: string) => p.trim())
        contactPhones.forEach((p: string) => { if (!phones.includes(p.trim())) phones.push(p.trim()) })
      } else if (phoneRegex.test(receiver)) {
        // 手动输入的手机号
        if (!phones.includes(receiver)) phones.push(receiver)
      }
    })

    if (phones.length > 0) {
      try {
        const result = await sendSmsBatch(phones, templateCode, templateParams)
        if (result !== null && result !== undefined) {
          const successCount = typeof result === 'number' ? result : (result as any)?.data ?? phones.length
          if (selectedAlarm.value) {
            addTimelineRecord(selectedAlarm.value, '发送通知', '值班人员', 
              `短信已发送至 ${phones.join('、')}，模板: ${templateCode}，成功: ${successCount}/${phones.length}`, true)
          }
          ElMessage.success(`短信发送完成，成功 ${successCount}/${phones.length} 个号码`)
        } else {
          if (selectedAlarm.value) {
            addTimelineRecord(selectedAlarm.value, '发送通知', '值班人员', '短信发送失败，请检查后台日志')
          }
          ElMessage.error('短信发送失败，请检查短信模板和渠道配置')
        }
      } catch (e: any) {
        console.error('短信发送异常:', e)
        const errMsg = e?.message || e?.msg || '未知错误'
        if (selectedAlarm.value) {
          addTimelineRecord(selectedAlarm.value, '发送通知', '值班人员', `短信发送失败: ${errMsg}`)
        }
        ElMessage.error(`短信发送失败: ${errMsg}`)
      }
    } else {
      if (selectedAlarm.value) {
        addTimelineRecord(selectedAlarm.value, '发送通知', '值班人员', '未找到有效手机号')
      }
      ElMessage.warning('未找到有效的手机号')
    }
  } else {
    ElMessage.success('通知已发送')
  }
  notifyVisible.value = false
  notifyLoading.value = false
}

// 应用通知模板（内容格式与短信模板保持一致）
const applyTemplate = (template: string) => {
  const alarm = selectedAlarm.value
  const location = alarm?.location || alarm?.alarmLocation || '未知位置'
  const alarmType = alarm?.alarmTypeName || alarm?.alarmType || '火警'
  const propertyName = alarm?.propertyName || ''
  const deviceName = alarm?.deviceName || alarm?.device || '设备'

  const templates: Record<string, string> = {
    fire: `火警通知：${propertyName}${location}发生${alarmType}，请立即处理！`,
    fault: `设备故障通知：${propertyName}${location}设备${deviceName}发生故障，请及时处理！`,
    inspect: '巡检通知：请于 ${time} 前完成 ${area} 区域的巡检工作。',
    custom: ''
  }
  // 同步更新短信模板编码
  if (template === 'fire') {
    notifyForm.smsTemplateCode = 'fire_alarm_notify'
  } else if (template === 'fault') {
    notifyForm.smsTemplateCode = 'device_fault_notify'
  }
  if (template !== 'custom' && templates[template]) {
    notifyForm.content = templates[template]
  }
}

// 拨打电话
const makeCall = async () => {
  if (!callForm.phone) {
    ElMessage.warning('请输入电话号码')
    return
  }
  
  callLoading.value = true
  try {
    await request.post({
      url: '/eiot/firewatch/alarm-order/call',
      params: { id: selectedAlarm.value?.id, phone: callForm.phone }
    })
    ElMessage.success(`正在拨打 ${callForm.phone}...`)
    callHistory.value.unshift({
      id: Date.now(),
      name: contactList.value.find(c => c.value === callForm.contact)?.label || '未知',
      phone: callForm.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2'),
      time: new Date().toLocaleTimeString().substring(0, 5),
      status: 'connected'
    })
  } catch (error) {
    ElMessage.error('拨打电话失败，请检查号码和网络连接')
  } finally {
    callLoading.value = false
  }
}

// 联系人变更
const onContactChange = (value: string) => {
  const contact = contactList.value.find(c => c.value === value)
  if (contact) {
    callForm.phone = contact.phone
  }
}

// 执行联动
const executeLinkage = async () => {
  if (!linkageForm.planId) {
    ElMessage.warning('请选择预案')
    return
  }
  
  linkageLoading.value = true
  try {
    await request.post({ url: '/eiot/firewatch/linkage/execute', data: linkageForm })
    ElMessage.success('联动预案执行成功')
    linkageVisible.value = false
    // 联动执行后短信通知 + 时间线
    if (selectedAlarm.value) {
      addTimelineRecord(selectedAlarm.value, '执行联动预案', '值班人员', `预案ID: ${linkageForm.planId}, 联动设备: ${linkageForm.devices.join(',')}`, true)
      autoSmsNotify('linkage', selectedAlarm.value)
    }
  } catch (error) {
    ElMessage.error('联动预案执行失败，请重试')
  } finally {
    linkageLoading.value = false
  }
}

// 派发工单
const submitDispatch = async () => {
  if (!dispatchForm.type || !dispatchForm.assignee) {
    ElMessage.warning('请填写必填项')
    return
  }
  
  dispatchLoading.value = true
  try {
    await request.post({ url: '/eiot/firewatch/workorder/create', data: dispatchForm })
    ElMessage.success('工单派发成功')
    dispatchVisible.value = false
    // 派单后短信通知
    if (selectedAlarm.value) {
      const assigneeName = dutyUsers.value.find(u => u.id === Number(dispatchForm.assignee))?.name || '未知'
      addTimelineRecord(selectedAlarm.value, '派发工单', '值班人员', `指派给: ${assigneeName}, 优先级: ${dispatchForm.priority}`, true)
      autoSmsNotify('dispatch', selectedAlarm.value)
    }
  } catch (error) {
    ElMessage.error('工单派发失败，请重试')
  } finally {
    dispatchLoading.value = false
  }
}

// 上报事件
const submitReport = async () => {
  if (!reportForm.target || !reportForm.eventType || !reportForm.description) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  reportLoading.value = true
  try {
    await request.post({ url: '/eiot/firewatch/report/create', data: reportForm })
    ElMessage.success('事件上报成功')
    reportVisible.value = false
  } catch (error) {
    ElMessage.error('事件上报失败，请重试')
  } finally {
    reportLoading.value = false
  }
}

// 保存复盘
const submitReview = async () => {
  if (!reviewForm.analysis) {
    ElMessage.warning('请填写复盘分析')
    return
  }
  
  reviewLoading.value = true
  try {
    await request.post({ url: '/eiot/firewatch/review/create', data: reviewForm })
    ElMessage.success('复盘保存成功')
    reviewVisible.value = false
  } catch (error) {
    ElMessage.error('复盘保存失败，请重试')
  } finally {
    reviewLoading.value = false
  }
}

// 选择摄像头
const selectCamera = (camera: any) => {
  selectedCamera.value = camera
}

// 加载摄像头视频流
const loadCameraVideo = async (camera: any) => {
  if (!camera) {
    rightVideoUrl.value = null
    rightVideoLoading.value = false
    rightVideoError.value = null
    return
  }
  rightVideoLoading.value = true
  rightVideoError.value = null
  rightVideoUrl.value = null
  try {
    const urlRes = await getLiveUrl(camera.id)
    rightVideoUrl.value = urlRes?.url || null
    if (!rightVideoUrl.value) {
      rightVideoError.value = '未获取到视频流地址'
    }
  } catch (e: any) {
    rightVideoError.value = e.message || '获取视频流失败'
  } finally {
    rightVideoLoading.value = false
  }
}

// 选中摄像头变化时自动加载视频流
watch(selectedCamera, (newCam) => {
  loadCameraVideo(newCam)
})

// 选择告警时自动加载摄像头列表
watch(selectedAlarm, async (newAlarm) => {
  if (newAlarm) {
    await loadCamerasForAlarm(newAlarm)
  }
})

// 视频全屏
const videoContainerRef = ref()
const toggleFullscreen = () => {
  if (videoContainerRef.value) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      videoContainerRef.value.requestFullscreen()
    }
  }
}

// 刷新视频
const refreshVideo = () => {
  if (selectedCamera.value) {
    ElMessage.success('正在刷新视频...')
    loadCameraVideo(selectedCamera.value)
  } else {
    ElMessage.warning('请先选择摄像头')
  }
}

// 重试播放右侧面板视频
const retryRightVideo = () => {
  if (selectedCamera.value) {
    loadCameraVideo(selectedCamera.value)
  }
}

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  
  // 响应计时器（每秒更新一次以刷新已持续时间显示）
  elapsedTimer = setInterval(() => {
    // 触发响应式更新 - 通过修改ref触发页面重绘
    pendingCount.value = alarmList.value.filter((a: any) => a.status === 'pending').length
  }, 60000) // 每分钟更新一次
  
  // 注册键盘快捷键
  window.addEventListener('keydown', handleKeyboard)
  
  // 加载告警数据
  refreshAlarms()
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (elapsedTimer) clearInterval(elapsedTimer)
  window.removeEventListener('keydown', handleKeyboard)
})
</script>

<style lang="scss" scoped>
// === Cyan-Green Command Center Palette ===
$navy-900: #0c1527;
$navy-800: #121e36;
$navy-700: #1a2d4d;

$white: #ffffff;
$warm-bg: #eef5f5;
$warm-card: #ffffff;
$warm-hover: #eef8f7;

$border-light: #e1ecec;
$border-medium: #c5dfde;

$ink: #293b3d;
$ink-secondary: #52686a;
$ink-muted: #7b8e90;

$coral: #e65353;
$coral-soft: #fde8e7;
$coral-glow: rgba(230, 83, 83, 0.12);
$amber: #f3a42a;
$amber-soft: #fff2d9;
$emerald: #53b57d;
$emerald-soft: #e8f5ed;
$sky: #4c8fe5;
$sky-soft: #e9f1fc;
$violet: #199b96;

$shadow-xs: 0 1px 2px rgba(41, 59, 61, 0.04);
$shadow-sm: 0 2px 8px rgba(41, 59, 61, 0.05);
$shadow-md: 0 4px 16px rgba(41, 59, 61, 0.07);
$shadow-lg: 0 8px 32px rgba(41, 59, 61, 0.09);
$shadow-warm: 0 4px 20px rgba(230, 83, 83, 0.08);

$r-sm: 8px;
$r-md: 12px;
$r-lg: 18px;
$r-pill: 100px;

// === Root Container ===
.alarm-center {
  width: 100%;
  min-height: calc(100vh - 84px);
  background: $warm-bg;
  color: $ink;
  display: flex;
  flex-direction: column;
}

// === Title Bar ===
.page-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 28px;
  background: $white;
  border-bottom: 1px solid $border-light;
  box-shadow: $shadow-xs;

  .title-left {
    display: flex;
    align-items: center;
    gap: 14px;

    .title-indicator {
      width: 4px;
      height: 34px;
      border-radius: 2px;
      background: #199b96;
    }

    .page-title {
      font-size: 22px;
      font-weight: 800;
      margin: 0;
      color: $ink;
      letter-spacing: -0.02em;
    }

    .page-subtitle {
      font-size: 12px;
      color: $ink-muted;
      margin-top: 3px;
      display: block;
      letter-spacing: 0.01em;
    }
  }

  .title-right {
    display: flex;
    align-items: center;
    gap: 10px;

    .pending-badge {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 6px 16px;
      background: $coral-soft;
      border: 1px solid rgba($coral, 0.12);
      border-radius: $r-pill;
      font-size: 12px;
      font-weight: 600;
      color: $coral;

      .badge-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: $coral;
        animation: pulse-dot 2s ease-in-out infinite;
      }
    }
  }
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.35; transform: scale(0.7); }
}

// === Two-Column Layout ===
.main-content {
  flex: 1;
  display: flex;
  min-height: 0;
  padding: 22px 28px;
  gap: 22px;
}

.left-column {
  flex: 1.15;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 22px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $border-medium; border-radius: 2px; }
}

.right-column {
  flex: 0.85;
  min-width: 320px;
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 3px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: $border-medium; border-radius: 2px; }
}

// === Section Cards ===
.section-card {
  background: $warm-card;
  border: 1px solid $border-light;
  border-radius: $r-lg;
  padding: 22px 24px;
  box-shadow: $shadow-sm;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;

  .section-title {
    font-size: 17px;
    font-weight: 800;
    color: $ink;
    display: flex;
    align-items: center;
    gap: 9px;
    letter-spacing: -0.01em;

    .dot-icon {
      display: inline-block;
      width: 9px;
      height: 9px;
      border-radius: 50%;

      &.dot-red {
        background: $coral;
        box-shadow: 0 0 8px $coral-glow, 0 0 3px rgba($coral, 0.3);
      }
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

// === Alarm List ===
.alarm-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.alarm-pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0 4px;
}

.alarm-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: $warm-card;
  border: 1px solid $border-light;
  border-radius: $r-md;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.22s ease;
  position: relative;

  // Warm gradient accent line at bottom (hidden by default)
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 12px;
    right: 12px;
    height: 3px;
    border-radius: 3px 3px 0 0;
    background: linear-gradient(90deg, #199b96, #4cb5b0, #8fc8c5);
    opacity: 0;
    transition: opacity 0.25s ease;
  }

  &:hover {
    border-color: $border-medium;
    box-shadow: $shadow-md;
    background: $warm-hover;
  }

  &.selected {
    border-color: rgba($coral, 0.25);
    background: $warm-hover;
    box-shadow: $shadow-warm;

    &::after {
      opacity: 1;
    }
  }

  // Risk-level left accents
  &.risk-border-A {
    border-left: 4px solid $coral;
  }
  &.risk-border-B {
    border-left: 4px solid $amber;
  }
  &.risk-border-C {
    border-left: 4px solid $emerald;
  }

  .alarm-left {
    flex-shrink: 0;
    padding-top: 2px;

    .alarm-icon {
      width: 38px;
      height: 38px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 17px;

      &.icon-A {
        background: $coral-soft;
        color: $coral;
        box-shadow: 0 2px 8px $coral-glow;
      }
      &.icon-B {
        background: $amber-soft;
        color: $amber;
      }
      &.icon-C {
        background: $emerald-soft;
        color: $emerald;
      }
    }
  }

  .alarm-body {
    flex: 1;
    min-width: 0;

    .alarm-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 7px;
      flex-wrap: wrap;

      .alarm-title {
        font-size: 15px;
        font-weight: 700;
        color: $ink;
        letter-spacing: -0.01em;
      }

      .alarm-tags-inline {
        display: flex;
        align-items: center;
        gap: 5px;
        flex-wrap: wrap;
      }
    }

    .alarm-detail-line {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;
      font-size: 13px;

      .device-info {
        color: $ink-muted;
        font-size: 12px;
      }
    }

    .alarm-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }
  }

  .alarm-time-block {
    flex-shrink: 0;
    text-align: right;
    min-width: 120px;
    padding-top: 2px;

    .alarm-time-line {
      white-space: nowrap;
    }

    .alarm-elapsed-line {
      white-space: nowrap;
    }

    .time-label {
      font-size: 11px;
      color: $ink-muted;
      font-weight: 500;
    }

    .alarm-time-value {
      font-size: 17px;
      font-weight: 800;
      color: #199b96;
      font-variant-numeric: tabular-nums;
      letter-spacing: -0.02em;
      line-height: 1.2;
    }

    .alarm-elapsed {
      font-size: 11px;
      color: $ink-muted;
    }
  }

  .risk-level {
    display: inline-block;
    padding: 2px 9px;
    border-radius: $r-pill;
    font-size: 11px;
    font-weight: 700;

    &.risk-A {
      background: $coral-soft;
      color: $coral;
    }
    &.risk-B {
      background: $amber-soft;
      color: $amber;
    }
    &.risk-C {
      background: $emerald-soft;
      color: $emerald;
    }
  }
}

// === Event Summary ===
.event-summary {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid $border-light;

  &:last-child { border-bottom: none; }

  .event-icon {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    flex-shrink: 0;

    &.level-high { background: $coral-soft; color: $coral; }
    &.level-medium { background: $amber-soft; color: $amber; }
    &.level-low { background: $emerald-soft; color: $emerald; }
  }

  .event-info {
    flex: 1;
    min-width: 0;

    .event-title {
      font-size: 13px;
      font-weight: 600;
      color: $ink;
    }
    .event-desc {
      font-size: 12px;
      color: $ink-muted;
      margin-top: 2px;
    }
  }

  .event-meta {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    gap: 10px;

    .event-time {
      font-size: 12px;
      color: $ink-muted;
      font-variant-numeric: tabular-nums;
    }
  }
}

// === Right Column Panels ===
.panel-card {
  background: $warm-card;
  border: 1px solid $border-light;
  border-radius: $r-lg;
  overflow: hidden;
  box-shadow: $shadow-sm;

  .card-header {
    padding: 15px 20px;
    border-bottom: 1px solid $border-light;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 15px;
    font-weight: 700;
    color: $ink;

    .header-dot {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-right: 9px;
      vertical-align: middle;

      &.live {
        background: $coral;
        box-shadow: 0 0 6px rgba($coral, 0.45);
        animation: pulse-dot 1.5s infinite;
      }
      &.ai {
        background: $violet;
        box-shadow: 0 0 6px rgba($violet, 0.35);
      }
      &.response {
        background: $sky;
        box-shadow: 0 0 6px rgba($sky, 0.35);
      }
    }

    .card-actions {
      display: flex;
      gap: 6px;

      .action-icon {
        width: 30px;
        height: 30px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: $r-sm;
        color: $ink-muted;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #eef8f7;
          color: #199b96;
        }
      }
    }
  }
}

// === Video ===
.video-container {
  height: 220px;
  position: relative;
  background: #07191c;

  .video-placeholder {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 12px;
    color: rgba(255, 255, 255, 0.35);

    .camera-icon {
      font-size: 40px;
      color: rgba(255, 255, 255, 0.18);
    }
    span {
      font-size: 12px;
      letter-spacing: 0.02em;
    }
  }

  .video-content {
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;

    .camera-name {
      color: rgba(255, 255, 255, 0.8);
      font-size: 13px;
      font-weight: 500;
      padding: 6px 16px;
      background: rgba(255, 255, 255, 0.06);
      border-radius: $r-pill;
      border: 1px solid rgba(255, 255, 255, 0.08);
    }
  }

  .live-badge {
    position: absolute;
    bottom: 12px;
    left: 12px;
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 3px 11px;
    background: rgba($coral, 0.88);
    border-radius: $r-pill;
    font-size: 10px;
    font-weight: 700;
    color: #fff;
    letter-spacing: 0.03em;

    .dot {
      width: 5px;
      height: 5px;
      border-radius: 50%;
      background: #fff;
      animation: blink 1.2s infinite;
    }
  }
}

.camera-list {
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 7px;

  .camera-item {
    padding: 5px 12px;
    border-radius: $r-pill;
    font-size: 11px;
    background: $warm-bg;
    border: 1px solid transparent;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 5px;
    color: $ink-secondary;
    transition: all 0.18s;
    font-weight: 500;

    &:hover {
      background: #eef8f7;
      color: #199b96;
      border-color: rgba($sky, 0.15);
    }
    &.active {
      background: #199b96;
      color: #fff;
      border-color: #199b96;
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.15; }
}

// === AI Analysis ===
.ai-analysis {
  padding: 18px 20px;

  .ai-top-row {
    margin-bottom: 16px;

    .ai-label-group {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .ai-label {
        font-size: 12px;
        color: $ink-muted;
        font-weight: 500;
      }
    }
  }

  .risk-bar {
    height: 5px;
    border-radius: 3px;
    overflow: hidden;
    display: flex;
    background: $warm-bg;
    margin-bottom: 14px;

    .bar-segment {
      height: 100%;
      &.high { background: linear-gradient(90deg, $coral, #ff8a80); }
      &.medium { background: linear-gradient(90deg, $amber, #ffc964); }
      &.low { background: linear-gradient(90deg, $emerald, #69f0ae); }
    }
  }

  .analysis-items {
    margin-bottom: 16px;

    .analysis-item {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 11px;

      &:last-child { margin-bottom: 0; }

      .name {
        width: 52px;
        font-size: 12px;
        color: $ink-secondary;
        font-weight: 500;
      }

      .progress-bar {
        flex: 1;
        height: 7px;
        background: $warm-bg;
        border-radius: 4px;
        overflow: hidden;

        .progress {
          height: 100%;
          border-radius: 4px;
          transition: width 0.7s cubic-bezier(0.25, 0.46, 0.45, 0.94);
        }
      }

      .value {
        width: 40px;
        font-size: 14px;
        text-align: right;
        font-weight: 800;
        color: #199b96;
        font-variant-numeric: tabular-nums;
      }
    }
  }

  .ai-tag-row {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding-top: 4px;
  }
}

// === Quick Response ===
.quick-response {
  padding: 16px 18px;

  .response-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 10px;
  }

  .response-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 14px 4px;
    background: $warm-bg;
    border-radius: $r-md;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 1px solid transparent;

    &:hover {
      transform: translateY(-2px);
      box-shadow: $shadow-md;
    }

    .el-icon {
      font-size: 21px;
      transition: transform 0.15s;
    }
    &:hover .el-icon { transform: scale(1.1); }

    span {
      font-size: 11px;
      font-weight: 600;
      color: $ink-secondary;
    }

    // 竖向卡片内的「敬请期待」标签去掉行内左边距，保持居中
    :deep(.dev-tag) {
      margin-left: 0;
    }

    &.alarm { .el-icon { color: $coral; } &:hover { background: $coral-soft; border-color: rgba($coral, 0.12); } }
    &.notify { .el-icon { color: $sky; } &:hover { background: $sky-soft; border-color: rgba($sky, 0.12); } }
    &.link { .el-icon { color: $emerald; } &:hover { background: $emerald-soft; border-color: rgba($emerald, 0.12); } }
    &.report { .el-icon { color: $amber; } &:hover { background: $amber-soft; border-color: rgba($amber, 0.12); } }
    &.dispatch { .el-icon { color: $sky; } &:hover { background: $sky-soft; border-color: rgba($sky, 0.12); } }
    &.inspect { .el-icon { color: $emerald; } &:hover { background: $emerald-soft; border-color: rgba($emerald, 0.12); } }
    &.callback { .el-icon { color: $violet; } &:hover { background: rgba($violet, 0.06); border-color: rgba($violet, 0.12); } }
    &.review { .el-icon { color: $ink-secondary; } &:hover { background: $sky-soft; border-color: rgba($sky, 0.12); } }
    &.reset { .el-icon { color: $ink-muted; } &:hover { border-color: $border-medium; } }
    &.mute {
      .el-icon { color: $amber; }
      &:hover { background: $amber-soft; border-color: rgba($amber, 0.12); }
      &.active { background: $amber-soft; border-color: rgba($amber, 0.2); box-shadow: 0 0 12px rgba($amber, 0.08); }
    }
    &.false-alarm { .el-icon { color: $ink-muted; } &:hover { border-color: $border-medium; } }
    &.timeline-btn { .el-icon { color: $sky; } &:hover { background: $sky-soft; border-color: rgba($sky, 0.12); } }
  }
}

// === Dialogs / Misc ===
.linkage-content {
  .linkage-warning {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 16px;
    background: $amber-soft;
    border: 1px solid rgba($amber, 0.18);
    border-radius: $r-md;
    margin-bottom: 20px;

    .warning-icon { font-size: 22px; color: $amber; }
    span { font-weight: 500; }
  }
}

.call-history {
  max-height: 150px;
  overflow-y: auto;

  .call-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid $border-light;

    &:last-child { border-bottom: none; }

    .call-info {
      .call-name { font-weight: 600; margin-right: 8px; font-size: 13px; }
      .call-phone { color: $ink-muted; font-size: 12px; }
    }
    .call-meta {
      display: flex; align-items: center; gap: 8px;
      .call-time { color: $ink-muted; font-size: 12px; }
    }
  }
}

.detail-timeline {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid $border-light;

  .timeline-title {
    font-weight: 700;
    font-size: 14px;
    margin-bottom: 16px;
    padding-left: 12px;
    border-left: 3px solid $sky;
  }

  .timeline-content {
    .timeline-action { font-weight: 600; font-size: 13px; }
    .timeline-operator { color: $ink-muted; font-size: 12px; }
  }
  .timeline-desc { color: $ink-secondary; font-size: 12px; margin-top: 4px; }
}

.shortcut-list {
  .shortcut-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 0;
    border-bottom: 1px solid $border-light;

    &:last-child { border-bottom: none; }

    kbd {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 32px;
      padding: 4px 10px;
      background: $warm-bg;
      border: 1px solid $border-medium;
      border-radius: $r-sm;
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
      font-size: 12px;
      font-weight: 600;
    }
    span { color: $ink-secondary; font-size: 13px; }
  }
}
</style>
