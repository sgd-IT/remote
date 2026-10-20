<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="70px" class="search-form">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="queryParams.propertyId" placeholder="请选择单位" clearable style="width: 170px">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="隐患等级" prop="hazardLevel">
        <el-select v-model="queryParams.hazardLevel" placeholder="请选择" clearable style="width: 120px">
          <el-option label="一般隐患" value="general" />
          <el-option label="较大隐患" value="major" />
          <el-option label="重大隐患" value="critical" />
        </el-select>
      </el-form-item>
      <el-form-item label="隐患状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable style="width: 120px">
          <el-option label="待整改" value="pending" />
          <el-option label="整改中" value="rectifying" />
          <el-option label="待验收" value="reviewing" />
          <el-option label="已闭环" value="closed" />
          <el-option label="已关闭" value="cancelled" />
        </el-select>
      </el-form-item>
      <el-form-item label="发现日期" prop="foundDate">
        <el-date-picker
          v-model="queryParams.foundDate"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item label="关键字" prop="keyword" class="keyword-item">
        <el-input v-model="queryParams.keyword" placeholder="隐患描述/设备名称" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item class="search-btns">
        <el-button type="primary" @click="handleQuery"><Icon icon="ep:search" class="mr-5px" />搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" />重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 操作栏 -->
  <ContentWrap>
    <el-row :gutter="10" class="mb-10px">
      <el-col :span="1.5">
        <el-button type="primary" @click="handleCreate"><Icon icon="ep:plus" class="mr-5px" />上报隐患</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="handleStatistics"><Icon icon="ep:data-analysis" class="mr-5px" />统计分析</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button @click="handleExport"><Icon icon="ep:download" class="mr-5px" />导出</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button @click="handleImport"><Icon icon="ep:upload" class="mr-5px" />导入</el-button>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tableData" stripe border>
      <el-table-column label="隐患编号" prop="hazardNo" width="150" />
      <el-table-column label="所属单位" prop="propertyName" min-width="130" show-overflow-tooltip />
      <el-table-column label="隐患等级" width="90">
        <template #default="{ row }">
          <el-tag :type="getLevelTag(row.hazardLevel)" size="small">{{ row.hazardLevelName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="隐患描述" prop="hazardDesc" min-width="200" show-overflow-tooltip />
      <el-table-column label="关联设备" prop="deviceName" min-width="120" show-overflow-tooltip />
      <el-table-column label="发现人" prop="foundUserName" width="90" />
      <el-table-column label="发现日期" width="100">
        <template #default="{ row }">
          <span>{{ formatDateArray(row.foundDate) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="整改期限" width="100">
        <template #default="{ row }">
          <span v-if="row.deadline" :class="{ 'text-danger': isOverdue(row.deadline) && row.status !== 'closed' && row.status !== 'cancelled' }">{{ formatDateArray(row.deadline) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="整改进度" width="120">
        <template #default="{ row }">
          <el-progress
            :percentage="row.rectificationProgress || 0"
            :status="getProgressStatus(row)"
            :stroke-width="14"
            :text-inside="true"
          />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="getStatusTag(row.status)" size="small">{{ row.statusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'pending'" link type="warning" @click="handleRectify(row)">整改</el-button>
          <el-button v-if="row.status === 'rectifying'" link type="primary" @click="handleRectifyProgress(row)">进度</el-button>
          <el-button v-if="row.status === 'reviewing'" link type="success" @click="handleReview(row)">验收</el-button>
          <el-button v-if="row.status === 'pending'" link type="primary" @click="handleUpdate(row)">编辑</el-button>
          <el-button v-if="row.status === 'pending' || row.status === 'rectifying'" link type="info" @click="handleClose(row)">关闭</el-button>
          <el-button v-if="row.status === 'pending'" link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <Pagination v-model:page="queryParams.pageNo" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ContentWrap>

  <!-- 新增/编辑对话框 -->
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" append-to-body>
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="所属单位" prop="propertyId">
        <el-select v-model="formData.propertyId" placeholder="请选择单位" style="width: 100%" @change="handlePropertyChange">
          <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="隐患等级" prop="hazardLevel">
        <el-radio-group v-model="formData.hazardLevel">
          <el-radio label="general">一般隐患</el-radio>
          <el-radio label="major">较大隐患</el-radio>
          <el-radio label="critical">重大隐患</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="隐患描述" prop="hazardDesc">
        <el-input v-model="formData.hazardDesc" type="textarea" placeholder="请详细描述隐患情况" :rows="3" />
      </el-form-item>
      <el-form-item label="隐患位置" prop="location">
        <el-input v-model="formData.location" placeholder="请输入隐患具体位置" />
      </el-form-item>
      <el-form-item label="关联设备">
        <el-select v-model="formData.deviceId" placeholder="请选择关联设备" style="width: 100%" :loading="deviceLoading" clearable filterable>
          <el-option v-for="item in deviceList" :key="item.id" :label="`${item.name} (${item.dn || ''})`" :value="item.id">
            <span>{{ item.name }}</span>
            <span class="ml-5px text-gray">[{{ item.addr || '位置未知' }}]</span>
          </el-option>
        </el-select>
      </el-form-item>
      <el-row>
        <el-col :span="12">
          <el-form-item label="发现人" prop="foundUserName">
            <el-input v-model="formData.foundUserName" placeholder="请输入发现人" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="发现日期" prop="foundDate">
            <el-date-picker v-model="formData.foundDate" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="整改期限" prop="deadline">
        <el-date-picker v-model="formData.deadline" type="date" placeholder="请选择整改截止日期" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="整改建议" prop="suggestion">
        <el-input v-model="formData.suggestion" type="textarea" placeholder="请输入整改建议" :rows="2" />
      </el-form-item>
      <el-form-item label="现场照片">
        <el-upload
          ref="photoUploadRef"
          v-model:file-list="photoFileList"
          :auto-upload="false"
          list-type="picture-card"
          accept="image/*"
          multiple
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" placeholder="请输入备注" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">确定</el-button>
    </template>
  </el-dialog>

  <!-- 隐患详情对话框 -->
  <el-dialog v-model="detailVisible" title="隐患详情" width="850px" append-to-body top="30px">
    <el-descriptions :column="3" border>
      <el-descriptions-item label="隐患编号">{{ detailData.hazardNo }}</el-descriptions-item>
      <el-descriptions-item label="所属单位">{{ detailData.propertyName }}</el-descriptions-item>
      <el-descriptions-item label="隐患等级">
        <el-tag :type="getLevelTag(detailData.hazardLevel)" size="small">{{ detailData.hazardLevelName }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="发现人">{{ detailData.foundUserName }}</el-descriptions-item>
      <el-descriptions-item label="发现日期">{{ formatDateArray(detailData.foundDate) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="整改期限">
        <span v-if="detailData.deadline" :class="{ 'text-danger': isOverdue(detailData.deadline) && detailData.status !== 'closed' && detailData.status !== 'cancelled' }">{{ formatDateArray(detailData.deadline) }}</span>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="关联设备" :span="1">{{ detailData.deviceName || '无' }}</el-descriptions-item>
      <el-descriptions-item label="隐患位置" :span="2">{{ detailData.location || '-' }}</el-descriptions-item>
      <el-descriptions-item label="状态" :span="1">
        <el-tag :type="getStatusTag(detailData.status)" size="small">{{ detailData.statusName }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="整改进度" :span="1">
        <el-progress :percentage="detailData.rectificationProgress || 0" style="width: 150px" />
      </el-descriptions-item>
      <el-descriptions-item label="整改人" :span="1">{{ detailData.rectificationUserName || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-descriptions :column="1" border class="mt-15px">
      <el-descriptions-item label="隐患描述" :contentStyle="{ whiteSpace: 'pre-wrap' }">{{ detailData.hazardDesc }}</el-descriptions-item>
      <el-descriptions-item v-if="detailData.suggestion" label="整改建议" :contentStyle="{ whiteSpace: 'pre-wrap' }">{{ detailData.suggestion }}</el-descriptions-item>
      <el-descriptions-item v-if="detailData.remark" label="备注" :contentStyle="{ whiteSpace: 'pre-wrap' }">{{ detailData.remark }}</el-descriptions-item>
    </el-descriptions>

    <!-- 隐患照片 -->
    <div v-if="detailPhotos.length > 0" class="mt-15px">
      <el-divider content-position="left">现场照片</el-divider>
      <div class="photo-grid">
        <div v-for="(url, idx) in detailPhotos" :key="idx" class="photo-item">
          <el-image :src="url" fit="contain" style="width: 100%; height: 180px" :preview-src-list="detailPhotos" :initial-index="idx" />
        </div>
      </div>
    </div>

    <!-- 整改进度时间线 -->
    <div v-if="rectificationRecords.length > 0" class="mt-15px">
      <el-divider content-position="left">整改进度</el-divider>
      <el-timeline>
        <el-timeline-item
          v-for="(record, idx) in rectificationRecords"
          :key="idx"
          :timestamp="record.createTime"
          :type="record.type === 'progress' ? 'primary' : record.type === 'review' ? (record.reviewResult === 'approved' ? 'success' : 'danger') : 'info'"
          placement="top"
        >
          <div class="timeline-content">
            <div class="timeline-title">
              <el-tag v-if="record.type === 'progress'" size="small" type="primary">整改进度</el-tag>
              <el-tag v-else-if="record.type === 'review'" size="small" :type="record.reviewResult === 'approved' ? 'success' : 'danger'">
                {{ record.reviewResult === 'approved' ? '验收通过' : '验收不通过' }}
              </el-tag>
              <el-tag v-else size="small" type="info">系统记录</el-tag>
              <span class="ml-5px font-bold">{{ record.title }}</span>
            </div>
            <div v-if="record.content" class="timeline-desc">{{ record.content }}</div>
            <!-- 整改照片 -->
            <div v-if="getRecordPhotos(record).length > 0" class="mt-5px photo-grid">
              <div v-for="(url, pIdx) in getRecordPhotos(record)" :key="pIdx" class="photo-item">
                <el-image
                  :src="url"
                  fit="contain"
                  style="width: 120px; height: 120px"
                  :preview-src-list="getRecordPhotos(record)"
                  :initial-index="pIdx"
                />
              </div>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>
  </el-dialog>

  <!-- 整改对话框 -->
  <el-dialog v-model="rectifyVisible" title="隐患整改" width="600px" append-to-body>
    <el-form ref="rectifyFormRef" :model="rectifyForm" :rules="rectifyRules" label-width="100px">
      <el-form-item label="隐患信息">
        <el-input :value="rectifyForm.hazardDesc" type="textarea" disabled :rows="2" />
      </el-form-item>
      <el-form-item label="整改人" prop="rectificationUserId">
        <el-select v-model="rectifyForm.rectificationUserId" placeholder="请选择整改负责人" style="width: 100%">
          <el-option v-for="item in userList" :key="item.id" :label="item.nickname" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="整改措施" prop="measure">
        <el-input v-model="rectifyForm.measure" type="textarea" placeholder="请描述整改措施" :rows="3" />
      </el-form-item>
      <el-form-item label="整改进度" prop="progress">
        <el-slider v-model="rectifyForm.progress" :min="0" :max="100" :step="5" show-input style="width: 80%" />
      </el-form-item>
      <el-form-item label="整改说明">
        <el-input v-model="rectifyForm.remark" type="textarea" placeholder="请输入整改说明" :rows="2" />
      </el-form-item>
      <el-form-item label="整改照片">
        <el-upload
          ref="rectifyPhotoUploadRef"
          v-model:file-list="rectifyPhotoFileList"
          :auto-upload="false"
          list-type="picture-card"
          accept="image/*"
          multiple
        >
          <el-icon><Plus /></el-icon>
        </el-upload>
      </el-form-item>
      <el-form-item v-if="rectifyForm.progress >= 100" label="申请验收">
        <el-checkbox v-model="rectifyForm.applyReview">整改完成，申请验收</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rectifyVisible = false">取消</el-button>
      <el-button type="primary" @click="submitRectify">保存</el-button>
    </template>
  </el-dialog>

  <!-- 验收对话框 -->
  <el-dialog v-model="reviewVisible" title="验收复查" width="550px" append-to-body>
    <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
      <el-form-item label="隐患描述">
        <el-input :value="reviewForm.hazardDesc" type="textarea" disabled :rows="2" />
      </el-form-item>
      <el-form-item label="整改措施">
        <el-input :value="reviewForm.measure" type="textarea" disabled :rows="2" />
      </el-form-item>
      <el-form-item label="验收结果" prop="result">
        <el-radio-group v-model="reviewForm.result">
          <el-radio label="approved">验收通过，确认闭环</el-radio>
          <el-radio label="rejected">验收不通过，继续整改</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="验收意见" prop="remark">
        <el-input v-model="reviewForm.remark" type="textarea" placeholder="请输入验收意见" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewVisible = false">取消</el-button>
      <el-button type="primary" @click="submitReview">确定</el-button>
    </template>
  </el-dialog>

  <!-- 关闭对话框 -->
  <el-dialog v-model="closeVisible" title="关闭隐患" width="450px" append-to-body>
    <el-form ref="closeFormRef" :model="closeForm" :rules="closeRules" label-width="80px">
      <el-form-item label="关闭原因" prop="remark">
        <el-input v-model="closeForm.remark" type="textarea" placeholder="请输入关闭原因" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeVisible = false">取消</el-button>
      <el-button type="primary" @click="submitClose">确定关闭</el-button>
    </template>
  </el-dialog>

  <!-- 导入对话框 -->
  <el-dialog v-model="importVisible" title="导入隐患" width="400px" append-to-body>
    <el-upload
      ref="importUploadRef"
      :auto-upload="false"
      :limit="1"
      :headers="importHeaders"
      :action="importUrl"
      accept=".xlsx, .xls"
      drag
      :on-success="handleImportSuccess"
      :on-error="handleImportError"
    >
      <Icon icon="ep:upload" />
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-center">
          <span>仅允许导入 xls、xlsx 格式文件。</span>
          <el-link :underline="false" style="font-size: 12px; margin-left: 8px" type="primary" @click="handleDownloadTemplate">
            下载模板
          </el-link>
        </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button type="primary" @click="submitImport">确 定</el-button>
      <el-button @click="importVisible = false">取 消</el-button>
    </template>
  </el-dialog>

  <!-- 统计分析对话框 -->
  <el-dialog v-model="statisticsVisible" title="隐患统计分析" width="1000px" append-to-body top="30px" class="statistics-dialog">
    <!-- 统计筛选栏 -->
    <div class="stat-filter-bar">
      <div class="stat-filter-inner">
        <el-form :inline="true" class="stat-filter-form">
          <el-form-item label="统计范围">
            <el-date-picker
              v-model="statDateRange"
              type="daterange"
              range-separator="-"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="stat-date-picker"
            />
          </el-form-item>
          <el-form-item label="所属单位">
            <el-select v-model="statPropertyId" placeholder="全部单位" clearable style="width: 170px">
              <el-option v-for="item in propertyList" :key="item.id" :label="item.propertyName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="stat-search-btn" :loading="statLoading" @click="handleStatisticsSearch">
              <Icon v-if="!statLoading" icon="ep:search" class="mr-5px" />搜索
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 图表卡片区域 -->
    <div class="stat-charts-grid">
      <div class="stat-chart-card">
        <div class="stat-chart-header">
          <div class="stat-chart-title">
            <span class="stat-chart-dot level-dot"></span>
            隐患等级分布
          </div>
        </div>
        <div class="stat-chart-body">
          <div ref="levelChartRef" class="stat-chart-canvas"></div>
        </div>
      </div>
      <div class="stat-chart-card">
        <div class="stat-chart-header">
          <div class="stat-chart-title">
            <span class="stat-chart-dot status-dot"></span>
            隐患状态分布
          </div>
        </div>
        <div class="stat-chart-body">
          <div ref="statusChartRef" class="stat-chart-canvas"></div>
        </div>
      </div>
      <div class="stat-chart-card">
        <div class="stat-chart-header">
          <div class="stat-chart-title">
            <span class="stat-chart-dot trend-dot"></span>
            隐患趋势
          </div>
        </div>
        <div class="stat-chart-body">
          <div ref="trendChartRef" class="stat-chart-canvas"></div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  getHazardPage,
  createHazard,
  updateHazard,
  deleteHazard,
  getHazard,
  submitHazardRectification,
  reviewHazardRectification,
  closeHazard,
  getHazardStatistics,
  exportHazard,
  downloadHazardTemplate,
  importHazard
} from '@/api/eiot/firewatch/firewatch.api'
import { getPropertyList } from '@/api/eiot/firewatch/firewatch.api'
import { getSimpleUserList } from '@/api/system/user/index'
import * as FileApi from '@/api/infra/file'
import download from '@/utils/download'
import { getAccessToken, getTenantId } from '@/utils/auth'
import { showImportResult } from '@/utils/importResult'

defineOptions({ name: 'FireWatchHazard' })

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])
const propertyList = ref<any[]>([])
const userList = ref<any[]>([])
const deviceList = ref<any[]>([])
const deviceLoading = ref(false)

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  propertyId: undefined as number | undefined,
  hazardLevel: undefined as string | undefined,
  status: undefined as string | undefined,
  foundDate: undefined as string[] | undefined,
  keyword: undefined as string | undefined
})

const queryFormRef = ref<FormInstance>()

// 导入相关
const importVisible = ref(false)
const importUploadRef = ref<any>()
const importUrl = import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL + '/eiot/firewatch/hazard/import'
const importHeaders = computed(() => ({
  Authorization: 'Bearer ' + getAccessToken(),
  'tenant-id': getTenantId()
}))

// 新增/编辑对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const photoUploadRef = ref<any>()
const photoFileList = ref<any[]>([])

const formData = ref<any>({
  id: undefined,
  propertyId: undefined,
  hazardLevel: 'general',
  hazardDesc: '',
  location: '',
  deviceId: undefined,
  foundUserName: '',
  foundDate: undefined,
  deadline: undefined,
  suggestion: '',
  photos: '',
  remark: ''
})

const formRules = reactive<FormRules>({
  propertyId: [{ required: true, message: '请选择所属单位', trigger: 'change' }],
  hazardLevel: [{ required: true, message: '请选择隐患等级', trigger: 'change' }],
  hazardDesc: [{ required: true, message: '请输入隐患描述', trigger: 'blur' }],
  foundUserName: [{ required: true, message: '请输入发现人', trigger: 'blur' }],
  foundDate: [{ required: true, message: '请选择发现日期', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择整改期限', trigger: 'change' }]
})

// 详情
const detailVisible = ref(false)
const detailData = ref<any>({})
const detailPhotos = ref<string[]>([])
const rectificationRecords = ref<any[]>([])

// 整改
const rectifyVisible = ref(false)
const rectifyFormRef = ref<FormInstance>()
const rectifyPhotoUploadRef = ref<any>()
const rectifyPhotoFileList = ref<any[]>([])

const rectifyForm = ref<any>({
  id: undefined,
  hazardDesc: '',
  rectificationUserId: undefined,
  measure: '',
  progress: 0,
  remark: '',
  applyReview: false
})

const rectifyRules = reactive<FormRules>({
  rectificationUserId: [{ required: true, message: '请选择整改负责人', trigger: 'change' }],
  measure: [{ required: true, message: '请描述整改措施', trigger: 'blur' }],
  progress: [{ required: true, message: '请设置整改进度', trigger: 'change' }]
})

// 验收
const reviewVisible = ref(false)
const reviewFormRef = ref<FormInstance>()
const reviewForm = ref<any>({
  id: undefined,
  hazardDesc: '',
  measure: '',
  result: 'approved',
  remark: ''
})

const reviewRules = reactive<FormRules>({
  result: [{ required: true, message: '请选择验收结果', trigger: 'change' }],
  remark: [{ required: true, message: '请输入验收意见', trigger: 'blur' }]
})

// 关闭
const closeVisible = ref(false)
const closeFormRef = ref<FormInstance>()
const closeForm = ref<any>({ id: undefined, remark: '' })

const closeRules = reactive<FormRules>({
  remark: [{ required: true, message: '请输入关闭原因', trigger: 'blur' }]
})

// 统计分析
const statisticsVisible = ref(false)
const statDateRange = ref<string[]>([])
const statPropertyId = ref<number | undefined>(undefined)
const statLoading = ref(false)
const levelChartRef = ref<HTMLDivElement>()
const statusChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
let levelChart: ECharts | null = null
let statusChart: ECharts | null = null
let trendChart: ECharts | null = null

// 是否超期
const isOverdue = (deadline: any) => {
  if (!deadline) return false
  return new Date(formatDateArray(deadline)) < new Date(new Date().toLocaleDateString())
}

// 格式化日期，处理后端 LocalDate 数组格式 [year, month, day]
const formatDateArray = (date: any): string => {
  if (!date) return ''
  if (Array.isArray(date) && date.length >= 3) {
    const [y, m, d] = date
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return String(date)
}

// 解析整改记录中的照片
const getRecordPhotos = (record: any): string[] => {
  if (!record.photos) return []
  try {
    const photos = typeof record.photos === 'string' ? JSON.parse(record.photos) : record.photos
    return Array.isArray(photos) ? photos : []
  } catch {
    return []
  }
}

// 获取列表
const getList = async () => {
  loading.value = true
  try {
    const params: any = { ...queryParams }
    if (queryParams.foundDate && queryParams.foundDate.length === 2) {
      params.foundDateStart = queryParams.foundDate[0]
      params.foundDateEnd = queryParams.foundDate[1]
      delete params.foundDate
    }
    console.log('[getList] 请求参数:', JSON.stringify(params))
    const res = await getHazardPage(params)
    console.log('[getList] 响应结果:', JSON.stringify(res))
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } catch (e) {
    console.error('[getList] 获取隐患列表失败:', e)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

// 重置
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

// 新增
const handleCreate = () => {
  resetForm()
  photoFileList.value = []
  dialogTitle.value = '上报隐患'
  dialogVisible.value = true
  console.log('[handleCreate] 打开新建对话框，初始 formData:', JSON.stringify(formData.value))
}

// 编辑
const handleUpdate = (row: any) => {
  resetForm()
  photoFileList.value = []
  dialogTitle.value = '编辑隐患'
  console.log('[handleUpdate] 编辑行数据:', JSON.stringify(row))
  // 只提取表单需要的字段，避免多余后端字段污染提交数据
  formData.value = {
    id: row.id,
    propertyId: row.propertyId,
    hazardLevel: row.hazardLevel,
    hazardDesc: row.hazardDesc,
    location: row.location,
    deviceId: row.deviceId,
    foundUserName: row.foundUserName,
    foundDate: row.foundDate,
    deadline: row.deadline,
    suggestion: row.suggestion,
    photos: row.photos,
    remark: row.remark
  }
  console.log('[handleUpdate] 回填后 formData:', JSON.stringify(formData.value))
  dialogVisible.value = true
  // 如果有单位则加载设备列表
  if (row.propertyId) {
    loadDeviceList()
  }
}

// 删除
const handleDelete = async (row: any) => {
  await ElMessageBox.confirm('确认删除该隐患记录？', '提示', { type: 'warning' })
  await deleteHazard(row.id)
  ElMessage.success('删除成功')
  getList()
}

// 详情
const handleDetail = async (row: any) => {
  detailData.value = row
  // 解析照片
  try {
    const photos = row.photos ? JSON.parse(row.photos) : []
    detailPhotos.value = Array.isArray(photos) ? photos : []
  } catch {
    detailPhotos.value = row.photos ? [row.photos] : []
  }
  // 获取整改记录
  try {
    const res = await getHazard(row.id)
    if (res) {
      const records = res.rectificationRecords
      if (typeof records === 'string') {
        try {
          rectificationRecords.value = JSON.parse(records) || []
        } catch {
          rectificationRecords.value = []
        }
      } else if (Array.isArray(records)) {
        rectificationRecords.value = records
      } else {
        rectificationRecords.value = []
      }
    }
  } catch {
    rectificationRecords.value = []
  }
  detailVisible.value = true
}

// 整改
const handleRectify = async (row: any) => {
  rectifyForm.value = {
    id: row.id,
    hazardDesc: row.hazardDesc,
    rectificationUserId: undefined,
    measure: '',
    progress: 0,
    remark: '',
    applyReview: false
  }
  rectifyPhotoFileList.value = []
  rectifyVisible.value = true
}

// 整改进度
const handleRectifyProgress = async (row: any) => {
  rectifyForm.value = {
    id: row.id,
    hazardDesc: row.hazardDesc,
    rectificationUserId: row.rectificationUserId,
    measure: row.measure || '',
    progress: row.rectificationProgress || 0,
    remark: '',
    applyReview: false
  }
  rectifyPhotoFileList.value = []
  rectifyVisible.value = true
}

// 提交整改
const submitRectify = async () => {
  await rectifyFormRef.value?.validate()

  // 上传照片
  const photos: string[] = []
  const files = rectifyPhotoFileList.value || []
  for (const f of files) {
    const raw = f.raw
    if (raw) {
      try {
        const url = await FileApi.updateFile({ file: raw, path: '' })
        const realUrl = typeof url === 'string' ? url : (url?.data || url?.url || url)
        if (realUrl) photos.push(realUrl)
      } catch (e) {
        console.error('[submitRectify] 照片上传失败:', e)
      }
    }
  }

  const data: any = {
    id: rectifyForm.value.id,
    rectificationUserId: rectifyForm.value.rectificationUserId,
    measure: rectifyForm.value.measure,
    progress: rectifyForm.value.progress,
    remark: rectifyForm.value.remark
  }
  if (photos.length > 0) {
    data.photos = JSON.stringify(photos)
  }
  if (rectifyForm.value.applyReview) {
    data.applyReview = true
  }

  await submitHazardRectification(data)
  ElMessage.success(rectifyForm.value.progress >= 100 ? '整改完成，已提交验收申请' : '整改已保存')
  rectifyVisible.value = false
  getList()
}

// 验收
const handleReview = async (row: any) => {
  reviewForm.value = {
    id: row.id,
    hazardDesc: row.hazardDesc,
    measure: row.measure || '',
    result: 'approved',
    remark: ''
  }
  reviewVisible.value = true
}

// 提交验收
const submitReview = async () => {
  await reviewFormRef.value?.validate()
  await reviewHazardRectification(
    reviewForm.value.id,
    reviewForm.value.result,
    reviewForm.value.remark
  )
  ElMessage.success(reviewForm.value.result === 'approved' ? '验收通过，隐患已闭环' : '验收不通过，继续整改')
  reviewVisible.value = false
  getList()
}

// 关闭
const handleClose = async (row: any) => {
  closeForm.value = { id: row.id, remark: '' }
  closeVisible.value = true
}

// 提交关闭
const submitClose = async () => {
  await closeFormRef.value?.validate()
  await closeHazard(closeForm.value.id, closeForm.value.remark)
  ElMessage.success('隐患已关闭')
  closeVisible.value = false
  getList()
}

// 导出
const handleExport = async () => {
  try {
    const res = await exportHazard(queryParams)
    download.excel(res, '隐患数据.xls')
    ElMessage.success('导出成功')
  } catch (e) {
    console.error('导出失败', e)
  }
}

// 导入
const handleImport = () => {
  importVisible.value = true
}

const submitImport = () => {
  if (!importUploadRef.value || importUploadRef.value!.uploadFiles.length === 0) {
    ElMessage.error('请先选择文件')
    return
  }
  importUploadRef.value!.submit()
}

const handleImportSuccess = (response: any) => {
  if (response.code !== 0) {
    ElMessage.error(response.msg || '导入失败')
    return
  }
  importVisible.value = false
  getList()
  showImportResult(response)
}

const handleImportError = () => {
  ElMessage.error('导入失败，请检查文件格式')
}

const handleDownloadTemplate = async () => {
  try {
    const res = await downloadHazardTemplate()
    download.excel(res, '隐患导入模板.xls')
  } catch (e) {
    console.error('下载模板失败', e)
  }
}

// 统计分析
const handleStatistics = async () => {
  const now = new Date()
  const firstDay = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const lastDay = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()).padStart(2, '0')}`
  statDateRange.value = [firstDay, lastDay]
  statPropertyId.value = undefined
  statisticsVisible.value = true

  await nextTick()
  loadStatisticsData()
}

// 统计搜索（按条件过滤并数据隔离）
const handleStatisticsSearch = () => {
  console.log('[统计搜索] 选定单位ID:', statPropertyId.value, '日期范围:', statDateRange.value)

  // 查找选中单位的租户ID
  const selectedProperty = propertyList.value.find((p: any) => Number(p.id) === Number(statPropertyId.value))
  console.log('[统计搜索] 匹配物业对象:', selectedProperty, '租户ID:', selectedProperty?.tenantId)

  // 销毁旧图表实例，确保完全重新渲染
  if (levelChart) { levelChart.dispose(); levelChart = null }
  if (statusChart) { statusChart.dispose(); statusChart = null }
  if (trendChart) { trendChart.dispose(); trendChart = null }
  loadStatisticsData()
}

const loadStatisticsData = async () => {
  const now = new Date()
  const startDate = statDateRange.value?.[0] || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01`
  const endDate = statDateRange.value?.[1] || `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()).padStart(2, '0')}`

  // 查找选中单位的租户ID（使用 Number 强转避免类型不匹配），实现数据隔离
  const selectedProperty = propertyList.value.find((p: any) => Number(p.id) === Number(statPropertyId.value))
  const tenantId = selectedProperty?.tenantId

  const requestParams: any = {
    startDate,
    endDate,
    propertyId: statPropertyId.value !== undefined ? Number(statPropertyId.value) : undefined
  }
  // 仅在 tenantId 有效时追加，避免后端收到 undefined 参数
  if (tenantId !== undefined && tenantId !== null) {
    requestParams.tenantId = Number(tenantId)
  }
  console.log('[统计查询] 请求参数:', JSON.stringify(requestParams))

  statLoading.value = true
  try {
    const res = await getHazardStatistics(requestParams)
    console.log('[统计查询] API响应:', JSON.stringify(res).substring(0, 500))

    const data = res || {}

    await nextTick()

    // 等级分布饼图
    if (levelChartRef.value) {
      if (!levelChart) {
        levelChart = echarts.init(levelChartRef.value)
      }
      const levelMap: Record<string, string> = {
        general: '一般隐患', major: '较大隐患', critical: '重大隐患'
      }
      const levelColors: Record<string, string> = {
        general: '#e6a23c', major: '#f56c6c', critical: '#c03636'
      }
      const levelStats = data.levelStatistics || {}
      const levelData = Object.entries(levelStats)
        .filter(([_, v]) => (v as number) > 0)
        .map(([k, v]) => ({
          name: levelMap[k] || k,
          value: v,
          itemStyle: { color: levelColors[k] || '#909399' }
        }))
      console.log('[统计查询] 等级分布:', JSON.stringify(levelData))

      levelChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['35%', '55%'],
          center: ['50%', '45%'],
          data: levelData.length ? levelData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#eee' } }],
          label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, lineHeight: 15 },
          labelLine: { show: true, length: 8, length2: 6 },
          avoidLabelOverlap: false
        }]
      })
    }

    // 状态分布饼图
    if (statusChartRef.value) {
      if (!statusChart) {
        statusChart = echarts.init(statusChartRef.value)
      }
      const statusMap: Record<string, string> = {
        pending: '待整改', rectifying: '整改中', reviewing: '待验收',
        closed: '已闭环', cancelled: '已关闭'
      }
      const statusColors: Record<string, string> = {
        pending: '#909399', rectifying: '#409eff', reviewing: '#e6a23c',
        closed: '#67c23a', cancelled: '#c0c4cc'
      }
      const statusStats = data.statusStatistics || {}
      const statusData = Object.entries(statusStats)
        .filter(([_, v]) => (v as number) > 0)
        .map(([k, v]) => ({
          name: statusMap[k] || k,
          value: v,
          itemStyle: { color: statusColors[k] || '#909399' }
        }))
      console.log('[统计查询] 状态分布:', JSON.stringify(statusData))

      statusChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['35%', '55%'],
          center: ['50%', '45%'],
          data: statusData.length ? statusData : [{ name: '暂无数据', value: 1, itemStyle: { color: '#eee' } }],
          label: { show: true, formatter: '{b}\n{d}%', fontSize: 11, lineHeight: 15 },
          labelLine: { show: true, length: 8, length2: 6 },
          avoidLabelOverlap: false
        }]
      })
    }

    // 趋势折线图
    if (trendChartRef.value) {
      if (!trendChart) {
        trendChart = echarts.init(trendChartRef.value)
      }
      const trendData = data.trendStatistics || []
      console.log('[统计查询] 趋势数据:', JSON.stringify(trendData))

      trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { bottom: 0, data: ['新增隐患', '已闭环'] },
        xAxis: {
          type: 'category',
          data: trendData.map((t: any) => t.date),
          axisLabel: { rotate: 30, fontSize: 10 }
        },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            name: '新增隐患',
            type: 'line',
            data: trendData.map((t: any) => t.newCount || 0),
            smooth: true,
            lineStyle: { color: '#f56c6c', width: 2 },
            itemStyle: { color: '#f56c6c' }
          },
          {
            name: '已闭环',
            type: 'line',
            data: trendData.map((t: any) => t.closedCount || 0),
            smooth: true,
            lineStyle: { color: '#67c23a', width: 2 },
            itemStyle: { color: '#67c23a' }
          }
        ],
        grid: { left: '8%', right: '8%', bottom: '25%' }
      })
    }
    console.log('[统计查询] 图表渲染完成')
  } catch (e: any) {
    console.error('[统计查询] 失败:', e?.message || e)
    ElMessage.error(e?.message || '统计查询失败，请稍后重试')
  } finally {
    statLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: undefined,
    propertyId: undefined,
    hazardLevel: 'general',
    hazardDesc: '',
    location: '',
    deviceId: undefined,
    foundUserName: '',
    foundDate: undefined,
    deadline: undefined,
    suggestion: '',
    photos: '',
    remark: ''
  }
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  console.log('========== [submitForm] 开始提交 ==========')
  console.log('[submitForm] 当前 formData:', JSON.stringify(formData.value, null, 2))

  // 表单验证
  try {
    await formRef.value?.validate()
    console.log('[submitForm] 表单验证通过')
  } catch (e) {
    console.warn('[submitForm] 表单验证失败:', e)
    return
  }

  // 上传照片
  const photos: string[] = []
  const files = photoFileList.value || []
  console.log('[submitForm] 待上传照片数:', files.length)
  for (const f of files) {
    const raw = f.raw
    if (raw) {
      console.log('[submitForm] 正在上传照片:', raw.name, '大小:', raw.size)
      try {
        const url = await FileApi.updateFile({ file: raw, path: '' })
        const realUrl = typeof url === 'string' ? url : (url?.data || url?.url || url)
        console.log('[submitForm] 照片上传成功, URL:', realUrl)
        if (realUrl) photos.push(realUrl)
      } catch (e) {
        console.error('[submitForm] 照片上传失败:', e)
      }
    } else {
      console.log('[submitForm] 跳过已上传的照片:', f.url || f.name)
    }
  }
  if (photos.length > 0) {
    formData.value.photos = JSON.stringify(photos)
    console.log('[submitForm] 照片已序列化:', formData.value.photos)
  }

  // 提取 API 需要的字段，避免多余字段干扰后端反序列化
  let submitData: any = {
    propertyId: formData.value.propertyId,
    hazardLevel: formData.value.hazardLevel,
    hazardDesc: formData.value.hazardDesc,
    location: formData.value.location,
    deviceId: formData.value.deviceId,
    foundUserName: formData.value.foundUserName,
    foundDate: formData.value.foundDate,
    deadline: formData.value.deadline,
    suggestion: formData.value.suggestion,
    photos: formData.value.photos,
    remark: formData.value.remark
  }

  try {
    if (formData.value.id) {
      submitData.id = formData.value.id
      console.log('[submitForm] 更新请求, submitData:', JSON.stringify(submitData, null, 2))
      const res = await updateHazard(submitData)
      console.log('[submitForm] 更新响应:', JSON.stringify(res))
      ElMessage.success('更新成功')
    } else {
      console.log('[submitForm] 创建请求, submitData:', JSON.stringify(submitData, null, 2))
      const res = await createHazard(submitData)
      console.log('[submitForm] 创建响应:', JSON.stringify(res))
      ElMessage.success('隐患上报成功')
    }
    dialogVisible.value = false
    getList()
  } catch (e: any) {
    console.error('========== [submitForm] 提交失败 ==========')
    console.error('[submitForm] 错误对象:', e)
    console.error('[submitForm] 错误消息:', e?.message || e)
    console.error('[submitForm] 错误堆栈:', e?.stack)
    console.error('[submitForm] 请求数据:', JSON.stringify(submitData, null, 2))
    // 错误提示已由 axios 拦截器统一处理
  }
}

// 获取隐患等级标签
const getLevelTag = (level: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    general: 'warning',
    major: 'danger',
    critical: 'danger'
  }
  return map[level] || 'info'
}

// 获取状态标签
const getStatusTag = (status: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    pending: 'info',
    rectifying: 'primary',
    reviewing: 'warning',
    closed: 'success',
    cancelled: 'info'
  }
  return map[status] || 'info'
}

// 获取进度条状态
const getProgressStatus = (row: any): 'success' | 'exception' | undefined => {
  if (row.status === 'closed') return 'success'
  if (row.status === 'cancelled') return undefined
  if (isOverdue(row.deadline) && row.status !== 'closed' && row.status !== 'cancelled') return 'exception'
  if (row.rectificationProgress >= 100) return 'success'
  return undefined
}

// 加载物业列表
const loadPropertyList = async () => {
  try {
    const res = await getPropertyList()
    propertyList.value = res || []
  } catch (e) {
    propertyList.value = []
  }
}

// 加载用户列表
const loadUserList = async () => {
  try {
    const res = await getSimpleUserList()
    userList.value = (res as any) || []
  } catch (e) {
    userList.value = []
  }
}

// 加载设备列表（租户数据隔离）
const loadDeviceList = async (tenantId?: number) => {
  deviceLoading.value = true
  try {
    const { getDevicesList } = await import('@/api/eiot/deviceinfo/devices.api')
    const params: any = { pageNo: 1, pageSize: 100 }
    if (tenantId) {
      params.tenantId = tenantId // 按租户ID过滤，实现数据隔离
    }
    const res = await getDevicesList(params)
    deviceList.value = res?.list || []
  } catch (e) {
    deviceList.value = []
  } finally {
    deviceLoading.value = false
  }
}

// 单位变更时加载设备（租户数据隔离）
const handlePropertyChange = (propertyId: number) => {
  if (!propertyId) {
    deviceList.value = []
    formData.value.deviceId = undefined
    return
  }
  // 获取选中单位的租户ID
  const property = propertyList.value.find(p => p.id === propertyId)
  const tenantId = property?.tenantId
  loadDeviceList(tenantId)
}

// 初始化
onMounted(() => {
  getList()
  loadPropertyList()
  loadUserList()
})
</script>

<style scoped>
.chart-title {
  text-align: center;
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}
.text-gray {
  color: #999;
  font-size: 12px;
}
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
.ml-5px {
  margin-left: 5px;
}
.mt-15px {
  margin-top: 15px;
}
.font-bold {
  font-weight: bold;
}
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}
.photo-item {
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #eee;
}
.timeline-content {
  margin-bottom: 8px;
}
.timeline-title {
  margin-bottom: 4px;
}
.timeline-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}

/* ===== 紧凑搜索栏 ===== */
.search-form :deep(.el-form-item) {
  margin-right: 8px;
  margin-bottom: 6px;
}
.search-form :deep(.el-form-item__label) {
  padding-right: 6px;
}
.search-form .keyword-item {
  margin-right: 4px;
}
.search-form .search-btns {
  margin-right: 0;
}
.search-form .search-btns :deep(.el-button) {
  padding: 8px 14px;
}

/* ===== 统计分析对话框样式 ===== */
.statistics-dialog :deep(.el-dialog__body) {
  padding: 20px 24px;
}

/* 筛选栏 */
.stat-filter-bar {
  background: linear-gradient(135deg, #f8faff 0%, #f0f5ff 100%);
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 20px;
  border: 1px solid #e8edf5;
}
.stat-filter-inner {
  display: flex;
  align-items: center;
}
.stat-filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
.stat-filter-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 16px;
}
.stat-filter-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #3d4757;
  font-size: 13px;
}
.stat-date-picker {
  width: 250px;
}
.stat-search-btn {
  border-radius: 6px;
  padding: 8px 20px;
  font-weight: 500;
}

/* 图表卡片网格 */
.stat-charts-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

/* 图表卡片 */
.stat-chart-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #eef2f8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.3s ease, transform 0.3s ease;
  overflow: hidden;
}
.stat-chart-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

/* 图表卡片头部 */
.stat-chart-header {
  padding: 14px 18px 0;
}
.stat-chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #2c3a4b;
}
.stat-chart-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}
.level-dot {
  background: linear-gradient(135deg, #e6a23c, #f56c6c);
}
.status-dot {
  background: linear-gradient(135deg, #409eff, #67c23a);
}
.trend-dot {
  background: linear-gradient(135deg, #f56c6c, #67c23a);
}

/* 图表主体 */
.stat-chart-body {
  padding: 8px 12px 12px;
}
.stat-chart-canvas {
  height: 280px;
  width: 100%;
}

/* 响应式：小屏幕时图表堆叠 */
@media (max-width: 900px) {
  .stat-charts-grid {
    grid-template-columns: 1fr;
  }
}
</style>
