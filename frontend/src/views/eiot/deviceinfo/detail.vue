<template>
  <div class="box equipment-detail">
    <el-page-header @back="goBack" content="设备详情" />
    <el-divider />
    <el-tabs v-loading="loading" v-model="state.activeName" @tab-click="handleClick">
      <el-tab-pane label="基本信息" name="base">
        <el-row :gutter="20">
          <el-col :span="14">
            <el-form
              ref="editFormRef"
              :model="state.deviceDetail"
              :rules="editRules"
              label-width="auto"
            >
              <el-form-item v-if="inAdd != true" label="设备id" prop="id">
                <el-input v-model="state.deviceDetail.id" :disabled="true" />
              </el-form-item>
              <el-form-item label="别名" prop="name">
                <el-input v-model="state.deviceDetail.name" :disabled="!inEdit" />
              </el-form-item>
              <el-form-item label="productKey" prop="productKey">
                <el-input
                  readonly
                  :value="formattedProductName"
                  :placeholder="$t('productKey')"
                  :disabled="!inAdd"
                >
                  <template #append>
                    <el-button @click="selectProduct" :disabled="!inAdd">{{ $t('选择') }}</el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="序列号" prop="serial No">
                <el-input v-model="state.deviceDetail.serialNo" :disabled="!inAdd">
                  <template #append>
                    <el-button v-if="state.nodeType !== 3" @click="generateSerialNo" :disabled="!inAdd">
                      {{ $t('输入序列号') }}
                    </el-button>
                    <el-button v-if="state.nodeType === 3" @click="generateSerialNo" :disabled="!inAdd">
                      {{ $t('生成') }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item label="设备唯一标识" prop="dn">
                <el-input v-model="state.deviceDetail.dn" :disabled="!inAdd" />
              </el-form-item>
              <el-form-item label="设备密钥" prop="secret">
                <el-input v-model="state.deviceDetail.secret" :disabled="true" />
              </el-form-item>
              <el-form-item label="设备地址" prop="addr">
                <el-input v-model="state.deviceDetail.addr" :disabled="!inEdit" />
              </el-form-item>
              <el-form-item label="经度" prop="lon">
                <el-input v-model="state.deviceDetail.lon" disabled />
              </el-form-item>
              <el-form-item label="纬度" prop="lat">
                <el-input v-model="state.deviceDetail.lat" disabled />
              </el-form-item>
              <el-form-item label="固件版本" prop="firmVersion">
                <el-input
                  v-model="state.deviceDetail.firmVersion"
                  type="number"
                  step="0.1"
                  :disabled="!inEdit"
                />
              </el-form-item>
              <el-form-item label="设备型号" prop="model">
                <el-input v-model="state.deviceDetail.model" :disabled="!inEdit" />
              </el-form-item>
              <el-form-item label="品牌代码" prop="brandCode">
                <el-input v-model="state.deviceDetail.brandCode" :disabled="!inEdit" />
              </el-form-item>
              <el-form-item label="协议版本" prop="apiVersion">
                <el-input v-model="state.deviceDetail.apiVersion" :disabled="!inEdit" />
              </el-form-item>
              <el-form-item label="设备类型" prop="deviceType">
                <el-select v-model="state.deviceDetail.deviceType" :disabled="!inEdit" placeholder="请选择设备类型" style="width: 100%">
                  <el-option label="用传" value="transfer" />
                  <el-option label="摄像头" value="camera" />
                  <el-option label="烟感" value="smoke" />
                  <el-option label="温湿度传感器" value="temp_humidity" />
                  <el-option label="门磁传感器" value="door_magnet" />
                  <el-option label="其他" value="other" />
                </el-select>
              </el-form-item>
              <el-form-item label="是否显示" prop="visible">
                <el-switch v-model="state.deviceDetail.visible" :disabled="!inEdit" />
              </el-form-item>
              <el-form-item label="在线状态" prop="state">
                <el-tag type="success" v-if="state.deviceDetail.state === 1">在线</el-tag>
                <el-tag type="warning" v-if="state.deviceDetail.state === 0">离线</el-tag>
              </el-form-item>
              <el-form-item label="创建时间" prop="createTime" v-if="!inAdd">
                <el-input v-model="state.deviceDetail.createTime" disabled="true" />
              </el-form-item>
              <el-form-item label="激活时间" prop="activeTime" v-if="!inAdd">
                <el-input v-model="state.deviceDetail.activeTime" disabled="true" />
              </el-form-item>
              <div class="flex justify-center" v-if="handleType != 'view'">
                <el-button type="primary" @click="submitForm"
                >{{ handleType == 'add' ? '新增' : '保存' }}
                </el-button>
              </div>
            </el-form>
          </el-col>
          <el-col :span="10">
            <el-card class="map-card">
              <template #header>
                <div class="card-header">
                  <span>设备位置</span>
                </div>
              </template>
              <Map
                :is-write="inEdit"
                :click-map="inEdit"
                :center="mapCenter"
                :address="deviceAddress"
                @locate-change="handleLocateChange"
                @address-change="handleAddressChange"
              />
            </el-card>
          </el-col>
        </el-row>
        <div style="margin: 10px 10px"
        >设备标签&nbsp;<el-button size="small" @click="addTag">
          <Icon icon="ep:plus" /> </el-button
        ></div>
        <el-descriptions :column="2" border :labelStyle="{ 'font-weight': 'bold' }">
          <el-descriptions-item
            v-for="tag in state.tags"
            :key="tag.name"
            :label="tag.name + '(' + tag.id + ')'"
          >{{ tag.value }}</el-descriptions-item
          >
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="属性" name="property" :disabled="inAdd">
        <el-table
          v-if="state.activeName === 'property'"
          :data="state.properties"
          border
          v-loading="state.loading"
          style="width: 100%"
        >
          <el-table-column prop="name" label="属性名" width="250">
            <template #default="scope"> {{ scope.row.name }}({{ scope.row.identifier }}) </template>
          </el-table-column>
          <el-table-column prop="value" label="属性值">
            <template #default="scope">
              <span>{{ scope.row.value }}{{ scope.row.unit }} &nbsp;</span>
              <el-button @click="showPropertyHistory(scope.row)" size="small">历史</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="occurred" label="修改时间">
            <template #default="scope">
              <span>{{ formatDate(scope.row.occurred) }} </span>
            </template>
          </el-table-column>
          <el-table-column label="可读写" width="80">
            <template #default="scope">
              <el-tag v-if="!scope.row.write" type="info" size="small" effect="plain">只读</el-tag>
              <el-button
                @click="showWriteProperty(scope.row)"
                v-if="scope.row.write"
                size="small"
                type="success"
                plain
              >可写</el-button
              >
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!!state.propertyHistory.name">
          <el-divider />
          <el-row>
            <el-col :span="2">
              <h5>历史数据</h5>
            </el-col>
            <el-col :span="9">
              <el-date-picker
                v-model="state.historyTime"
                type="datetimerange"
                :picker-options="state.pickerOptions"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                align="right"
                @change="timeRangeChange"
              />
            </el-col>
            <el-col :span="4">
              <el-radio-group v-model="state.dataType">
                <el-radio-button value="">无</el-radio-button>
                <el-radio-button value="stats" @click="openPropertyTable">统计</el-radio-button>
              </el-radio-group>
            </el-col>
          </el-row>
          <PropertyTable ref="PropertyTableRef" />
          <PropertyChart
            :name="state.propertyHistory.name"
            :properties="state.propertyHistory.data"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="服务" name="service" :disabled="inAdd">
        <el-table
          v-if="state.activeName === 'service'"
          :data="state.services"
          border
          v-loading="state.loading"
          style="width: 100%"
        >
          <el-table-column label="服务名称" width="180">
            <template #default="scope"> {{ scope.row.name }}({{ scope.row.identifier }}) </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button @click="showInvokeService(scope.row)" type="success" size="small" plain
              >调用</el-button
              >
            </template>
          </el-table-column>
          <el-table-column label="参数">
            <template #default="scope">
              <pre class="equipment-param">{{ scope.row.inputData }}</pre>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
<!--      <el-tab-pane label="设备通道" name="subChannel" v-if="state.nodeType === 3">-->
<!--        <Channel ref="ChannelRef" :device="state.deviceDetail" @player-event="getPlayerData" />-->
<!--      </el-tab-pane>-->

<!--      <el-tab-pane name="sipPlayer" label="直播" v-if="state.nodeType === 3" lazy>-->
<!--        <device-live-stream ref="deviceLiveStreamRef" :device="state.deviceDetail" />-->
<!--      </el-tab-pane>-->

      <el-tab-pane label="关联摄像头" name="cameras" :disabled="inAdd">
        <CameraBindPanel
          v-if="state.activeName === 'cameras' && state.deviceDetail.id"
          :deviceId="state.deviceDetail.id"
          :showPlay="true"
        />
      </el-tab-pane>
      <el-tab-pane label="日志" name="event" :disabled="inAdd">
        <el-form
          v-if="state.activeName === 'event'"
          :inline="true"
          :model="state.formInline"
          class="user-search"
        >
          <el-form-item>
            <el-select
              v-model="state.formInline.type"
              style="width: 150px"
              placeholder="请选择日志类型"
            >
              <el-option label="所有" value="" />
              <el-option label="状态" value="state" />
              <el-option label="事件" value="event" />
              <el-option label="属性" value="property" />
              <el-option label="服务" value="service" />
            </el-select>
          </el-form-item>
          <el-form-item label="搜索：">
            <el-input v-model="state.formInline.identifier" placeholder="日志识符" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="logSearch">搜索</el-button>
          </el-form-item>
        </el-form>

        <el-table :data="state.events" border v-loading="state.loading" style="width: 100%">
          <el-table-column label="时间" align="center" width="180">
            <template #default="scope">
              {{ formatDate(scope.row.time) }}
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" align="center" width="120" />
          <el-table-column prop="name" label="名称(标识符)" align="center" width="180" />
          <el-table-column label="内容">
            <template #default="scope">
              <pre class="equipment-param">{{ scope.row.content.data }}</pre>
            </template>
          </el-table-column>
        </el-table>
        <div class="mt-[20px] w-400px ml-auto">
          <el-pagination
            v-model:current-page="state.formInline.pageNo"
            v-model:page-size="state.formInline.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, pager, jumper"
            :total="state.formInline.total"
            @size-change="getEvents"
            @current-change="getEvents"
          />
        </div>
        <!--        <Pagination
          :data="state.formInline"
          :total="state.formInline.total"
          :page="state.formInline.pageNo"
          :limit="state.formInline.pageSize"
          @pagination="getEvents"
        />-->
      </el-tab-pane>

      <el-tab-pane label="模拟上报" name="report" :disabled="inAdd">
        <el-table
          v-if="state.activeName === 'report'"
          :data="state.modelFunctions"
          highlight-current-row
          v-loading="state.loading"
          border
          element-loading-text="拼命加载中"
          style="width: 100%"
        >
          <el-table-column sortable prop="type" label="功能类型" width="110" />
          <el-table-column sortable prop="name" label="功能名称" width="180" />
          <el-table-column sortable prop="identifier" label="标识符" width="150" />
          <el-table-column sortable prop="dataTypeName" label="数据类型" width="110" />
          <el-table-column sortable prop="params" label="数据定义" />
          <el-table-column label="上报">
            <template #default="scope">
              <el-form inline v-model="scope.row" label-width="80px">
                <el-form-item label="值" v-if="scope.row.type == 'property'">
                  <el-input v-model="scope.row.value" size="small" />
                </el-form-item>
                <el-form-item label="内容" v-else>
                  <el-input type="textarea" v-model="scope.row.content" size="small" rows="4" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="small" @click="sendDeviceMsg(scope.row)"
                  >发送</el-button
                  >
                </el-form-item>
              </el-form>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="设备配置" name="config" :disabled="inAdd">
        <div v-if="state.activeName === 'config'" class="px-4">
          <el-form label-width="90px" class="w-full">
            <el-form-item label="配置内容">
              <el-input
                v-model="state.configContent"
                type="textarea"
                :rows="18"
                placeholder="请输入设备配置(JSON)"
                :disabled="state.configLoading"
              />
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button @click="formatConfig" :disabled="state.configLoading">格式化</el-button>
                <el-button type="primary" @click="saveDeviceConfig" :loading="state.configLoading">
                  保存
                </el-button>
              </el-space>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="模拟设备" name="simulator" :disabled="inAdd">
        <DeviceSimulator
          v-if="state.activeName === 'simulator'"
          :thingModelFunctions="state.modelFunctions"
          :deviceDetail="state.deviceDetail"
        />
      </el-tab-pane>

      <el-tab-pane label="告警配置" name="alertConfig" :disabled="inAdd">
        <DeviceAlertConfig
          v-if="state.activeName === 'alertConfig'"
          :deviceId="state.deviceId"
        />
      </el-tab-pane>

      <el-tab-pane label="告警记录" name="alertRecord" :disabled="inAdd">
        <div v-if="state.activeName === 'alertRecord'" class="alert-record-tab">
          <el-form :inline="true" :model="state.alertRecordQuery" class="mb-4">
            <el-form-item label="告警名称">
              <el-input v-model="state.alertRecordQuery.name" placeholder="请输入告警名称" clearable style="width: 150px" />
            </el-form-item>
            <el-form-item label="告警等级">
              <el-select v-model="state.alertRecordQuery.level" placeholder="请选择" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option label="火警" value="1" />
                <el-option label="设备故障" value="2" />
                <el-option label="电源故障" value="3" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="state.alertRecordQuery.status" style="width: 120px" clearable>
                <el-option label="全部" value="" />
                <el-option label="未处置" value="pending" />
                <el-option label="已处置" value="done" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="onAlertRecordSearch">搜索</el-button>
              <el-button @click="resetAlertRecordQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="state.alertRecords" border v-loading="state.alertRecordLoading" style="width: 100%">
            <el-table-column prop="name" label="告警名称" width="150" />
            <el-table-column prop="level" label="告警等级" width="100">
              <template #default="scope">
                <el-tag :type="getLevelType(scope.row.level)">{{ scope.row.level }}级</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="readFlg" label="处置状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.readFlg ? 'success' : 'danger'">
                  {{ scope.row.readFlg ? '已处置' : '未处置' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="alertTime" label="告警时间" width="180">
              <template #default="scope">
                {{ formatDate(scope.row.alertTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="details" label="详情" />
          </el-table>
          <div class="mt-4 flex justify-end">
            <el-pagination
              v-model:current-page="state.alertRecordPage"
              v-model:page-size="state.alertRecordPageSize"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, pager"
              :total="state.alertRecordTotal"
              @size-change="loadAlertRecords"
              @current-change="loadAlertRecords"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="网关子设备" name="subEquipment" :disabled="inAdd"  v-if="state.nodeType === 0">
        <SubEquipment v-if="state.activeName === 'subEquipment'" :deviceInfo="state" />
      </el-tab-pane>

      <el-tab-pane label="远程控制" name="remoteControl" :disabled="inAdd" v-if="state.nodeType === 0">
        <RemoteControlTab
          v-if="state.activeName === 'remoteControl' && state.deviceDetail.id"
          :deviceId="state.deviceDetail.id"
        />
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      :title="state.title"
      v-model="state.propertyWriteFormVisible"
      width="40%"
      @close="closeDialog"
      :close-on-press-escape="false"
      :close-on-click-modal="false"
      append-to-body
      destroy-on-close
    >
      <el-form
        v-if="state.propertyWriteFormVisible"
        label-width="120px"
        :model="state.propertyWriteForm"
        ref="propertyWriteForm"
      >
        <div style="display: none">
          <el-input v-model="state.propertyWriteForm.identifier" type="hidden" />
        </div>
        <el-form-item label="属性值" prop="value">
          <el-input v-model="state.propertyWriteForm.value" auto-complete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button
            type="primary"
            :loading="state.loading"
            class="title"
            @click="submitPropertyWriteForm"
          >保存</el-button
          >
        </div>
      </template>
    </el-dialog>

    <el-dialog
      :title="state.title"
      v-model="state.serviceFormVisible"
      width="40%"
      @close="closeDialog"
      :close-on-press-escape="false"
      :close-on-click-modal="false"
      append-to-body
      destroy-on-close
    >
      <el-form
        v-if="state.serviceFormVisible"
        label-width="120px"
        :model="state.serviceForm"
        ref="serviceForm"
      >
        <div>
          <el-input v-model="state.serviceForm.identifier" type="hidden" />
          <el-input v-model="state.serviceForm.productKey" type="hidden" />
          <el-input v-model="state.serviceForm.dn" type="hidden" />
        </div>
        <div v-if="state?.serviceForm?.params.length === 0">是否确认调用？</div>
        <el-form-item
          v-for="param in state.serviceForm.params"
          :key="param.identifier"
          :label="param.identifier"
          prop="params"
        >
          <el-input v-model="param.value" auto-complete="off" />
          <div class="form-tips">{{ param.name }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDialog">取消</el-button>
          <el-button
            type="primary"
            :loading="state.loading"
            class="title"
            @click="submitServiceForm"
          >确认</el-button
          >
        </div>
      </template>
    </el-dialog>

    <el-dialog
      title="添加设备标签"
      v-model="state.showAddTag"
      width="400px"
      :close-on-press-escape="false"
      :close-on-click-modal="false"
      append-to-body
      destroy-on-close
    >
      <el-form
        v-if="state.showAddTag"
        ref="formRef"
        :model="state.tagForm"
        :rules="state.rules"
        label-width="80px"
      >
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="state.tagForm.name" />
        </el-form-item>
        <el-form-item label="标识符" prop="id">
          <el-input v-model="state.tagForm.id" />
        </el-form-item>
        <el-form-item label="标签值" prop="value">
          <el-input v-model="state.tagForm.value" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitAddTag">提交</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
  <!-- 选择产品 -->
  <product-list ref="productListRef" @product-event="getProductData" />
</template>

<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import { ThingModelApi } from '@/api/eiot/thingmodel'
import { ProductApi, ProductVO } from '@/api/eiot/product'
import { DeviceInfoApi } from '@/api/eiot/deviceinfo/index'

import {
  deviceLogs,
  devicePropertyLogs,
  deviceSimulateSend,
  propertySet,
  serviceInvoke
} from '@/api/eiot/deviceinfo/devices.api'

import PropertyTable from './modules/PropertyTable.vue'

import PropertyChart from './modules/PropertyChart.vue'

import productList from './product-list.vue'
import Map from '@/components/Map/index.vue'
const message = useMessage() // 消息弹窗
import DeviceSimulator from './modules/detail/DeviceSimulator.vue'
import SubEquipment from "./modules/detail/subEquipment.vue";
import RemoteControlTab from './modules/RemoteControlTab.vue'
import DeviceAlertConfig from '@/views/eiot/devicealert/config.vue'
import { getAlertRecordPage } from '@/api/eiot/firewatch/firewatch.api'
import { ref } from 'vue'
import request from '@/config/axios'
import CameraBindPanel from '@/views/eiot/video/components/CameraBindPanel.vue'

const { t } = useI18n() // 国际化
const route = useRoute()
const router = useRouter()
const { id } = route.params
const handleType = window.history.state.type
const productListRef = ref()
const inAdd = handleType == 'add' ? true : false
const inEdit = handleType == 'edit' || inAdd ? true : false

const { showMap } = route.query || false
const goBack = () => {
  router.push({
    path: '/device/device-info'
  })
}
const PropertyTableRef = ref()
const formRef = ref()
const state = reactive<any>({
  loading: false,
  activeName: 'base',
  title: '',
  nodeType: '',
  product: {},
  propertyWriteFormVisible: false,
  propertyWriteForm: {
    identifier: '',
    productKey: '',
    dn: '',
    value: ''
  },
  serviceFormVisible: false,
  serviceForm: {
    identifier: '',
    productKey: '',
    dn: '',
    params: []
  },
  showAddTag: false,
  showDeviceMap: false,
  tagForm: {
    name: '',
    identifier: '',
    value: ''
  },
  rules: {
    id: [{ required: true, message: '请输入标识符', trigger: 'blur' }],
    name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
    value: [{ required: true, message: '请输入标签值', trigger: 'blur' }]
  },
  deviceId: '',
  deviceDetail: {},
  thingModel: null,
  modelFunctions: [],
  properties: [],
  services: [],
  events: [],
  eventMap: {},
  mapLnglat: '',
  tags: [],
  formInline: {
    type: '',
    identifier: '',
    pageNo: 1,
    pageSize: 10,
    total: 0
  },
  deviceLogs: [],
  typeMap: {
    lifetime: '生命周期',
    state: '设备状态',
    property: '属性',
    event: '事件',
    service: '服务'
  },
  propertyHistory: {
    name: '',
    data: []
  },
  // 设备配置
  configContent: '',
  configLoading: false,
  // 告警记录
  alertRecords: [] as any[],
  alertRecordLoading: false,
  alertRecordQuery: {
    name: '',
    level: '',
    status: ''
  },
  alertRecordPage: 1,
  alertRecordPageSize: 10,
  alertRecordTotal: 0,
  dataType: '',
  currHistoryProperty: {},
  historyTime: [
    new Date(new Date().getTime() - 2 * 3600 * 1000),
    new Date()
  ],
  pickerOptions: {
    shortcuts: [
      {
        text: '最近1小时',
        onClick(picker) {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000)
          picker.$emit('pick', [start, end])
        }
      },
      {
        text: '最近6小时',
        onClick(picker) {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000 * 6)
          picker.$emit('pick', [start, end])
        }
      },
      {
        text: '最近1天',
        onClick(picker) {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000 * 24)
          picker.$emit('pick', [start, end])
        }
      },
      {
        text: '最近5天',
        onClick(picker) {
          const end = new Date()
          const start = new Date()
          start.setTime(start.getTime() - 3600 * 1000 * 24 * 5)
          picker.$emit('pick', [start, end])
        }
      }
    ]
  }
})



const selectProduct = async () => {
  await productListRef.value.open()
}
const getProductData = (product) => {
  state.deviceDetail.productKey = product.productKey
  state.deviceDetail.productName = product.name
  state.nodeType = product.nodeType
  state.product = product
}

const generateSerialNo = () => {
  if (!state.deviceDetail.productKey) {
    message.alert(t('输入产品KEY'))
    return
  }
  DeviceInfoApi.genSerialNo(state.product.nodeType).then((res) => {
    state.deviceDetail.serialNo = res
    if (state.product.dnTyp === 1) {
      state.deviceDetail.dn = state.deviceDetail.serialNo
    }
  })
}
const editFormRef = ref()
const editRules = reactive({
  productKey: [{ required: true, message: '产品KEY不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '别名不能为空', trigger: 'blur' }],
  dn: [{ required: true, message: '唯一吗不能为空', trigger: 'blur' }],
  serialNo: [{ required: true, message: '序列号不能为空', trigger: 'blur' }]
})

const submitForm = async () => {
  await editFormRef.value.validate((valid, fields) => {
    if (valid) {
      onSave()
    } else {
      console.log('error submit!', fields)
    }
  })
}

const onSave = async () => {
  const data = state.deviceDetail as unknown as DeviceInfoVO

  if (handleType === 'add') {
    await DeviceInfoApi.createDeviceInfo(data).then((newId)=>{
      message.success(t('common.createSuccess'))
      router.push({
        path: `/eiot/device/deviceDetail/${newId}`,
        state: {
          type: 'edit'
        }
      })
    })

  } else {
    await DeviceInfoApi.updateDeviceInfo(data)
    message.success(t('common.updateSuccess'))
  }
}



const getdata = () => {
  // 新增
  if (handleType === 'add') {
    // state.deviceDetail.value = ref<DeviceInfoVO>({fi})
    // };
    return
  }
  state.deviceId = id
  DeviceInfoApi.getDeviceInfo(state.deviceId).then((data: any) => {
    state.onlineTime = data.onlineTime

    state.deviceDetail = data
    state.mapLnglat = data.lon + ',' + data.lat
    state.showDeviceMap = true

    //取设备物模型信息
    console.log('state.thingModel', state.thingModel)
    if (!state.thingModel) {
      ProductApi.getByPk(data.productKey).then((res: any) => {
        console.log('res', res)
        state.nodeType = res?.nodeType
        state.product = res
      })
      ThingModelApi.getThingModelByProductKey(data.productKey).then((objRes: any) => {
        const data = objRes || {}
        //取物模型功能列表
        data.model = data.model || {
          properties: [],
          events: [],
          services: []
        }
        let model = data.model
        console.log('model', model)

        state.thingModel = model

        state.services = model.services
        // 获取设备当前属性
        DeviceInfoApi.getDeviceProperties(state.deviceId).then((data) => {
          fillProperty(data.identifier2property)
        })

        data.model.properties = data.model.properties || []
        data.model.events = data.model.events || []
        data.model.services = data.model.services || []
        data.model = JSON.parse(JSON.stringify(data.model))

        model = data.model || {}
        let modelFuncs: any[] = []
        model.properties.forEach((p) => {
          let params = JSON.stringify(p.dataType.specs || '{}', null, 4)
          modelFuncs.push({
            raw: p,
            type: 'property',
            name: p.name,
            identifier: p.identifier,
            dataTypeName: p.dataType.type,
            params: params == '{}' ? '' : params,
            value: '',
            occurred: ''
          })
        })
        model.events.forEach((e) => {
          let output = {}
          e.outputData.forEach((p) => {
            output[p.identifier] = p.name
          })
          modelFuncs.push({
            raw: e,
            type: 'event',
            name: e.name,
            identifier: e.identifier,
            dataTypeName: '-',
            params: JSON.stringify(output, null, 4),
            content: JSON.stringify(output, null, 4)
          })
        })
        model.services.forEach((s) => {
          let input = {}
          ;(s.inputData || []).forEach((p) => {
            input[p.identifier] = p.name
          })
          let output = {}
          ;(s.outputData || []).forEach((p) => {
            output[p.identifier] = p.name
          })
          modelFuncs.push({
            raw: s,
            type: 'service',
            name: s.name + '回复',
            identifier: s.identifier + '_reply',
            dataTypeName: '-',
            params: JSON.stringify(output, null, 4),
            content: JSON.stringify(output, null, 4)
          })
        })
        state.modelFunctions = modelFuncs
      })
    } else {
      DeviceInfoApi.getDeviceProperties(state.deviceId).then((data) => {
        fillProperty(data.identifier2property)
      })
    }

    let deviceTag = data.tag
    state.tags = []
    for (var p in deviceTag) {
      var tag = deviceTag[p]
      state.tags.push({ id: tag.id, name: tag.name, value: tag.value })
    }
  })
}
const fillProperty = (prop) => {
  let model = state.thingModel
  let props: any[] = []
  model.properties.forEach((p) => {
    props.push({
      identifier: p.identifier,
      name: p.name,
      value: prop[p.identifier]?.value,
      occurred: prop[p.identifier]?.occurred ?? '',
      write: p.accessMode != 'r',
      unit: p.unit ?? ''
    })
  })
  state.properties = props
}
const addTag = () => {
  state.showAddTag = true
}
const submitAddTag = () => {
  state.tagForm.deviceId = state.deviceId
  formRef.value.validate((valid) => {
    if (valid) {
      // devicesTagAdd(state.tagForm).then(() => {
      //   ElMessage({
      //     type: 'success',
      //     message: '添加成功',
      //   })
      //   getdata()
      //   state.showAddTag = false
      // })
    }
  })
}
const logSearch = () => {
  state.formInline.pageNo = 1
  getEvents()
}
const getEvents = () => {
  deviceLogs({
    deviceId: state.deviceId,
    ...state.formInline
  }).then((res) => {
    state.formInline.total = res.total
    let logs: any[] = []
    res.list.map((de) => {
      let row = {
        time: de.time,
        type: state.typeMap[de.type],
        name: '未知事件',
        content: de
      }
      logs.push(row)
      if (!state.thingModel) return
      let modeEvents = state.thingModel.events
      if (modeEvents && modeEvents.length > 0) {
        modeEvents.forEach((e) => {
          if (de.identifier == e.identifier) {
            row.name = e.name
            return
          }
        })
      }
      let modeServices = state.thingModel.services
      if (de.type == 'property') {
        if (de.identifier == 'set_reply') {
          row.name = '设置回复'
        } else if (de.identifier == 'report') {
          row.name = '上报'
        } else if (de.identifier == 'set') {
          row.name = '设置'
        }
      } else if (de.type == 'state') {
        if (de.identifier == 'online') {
          row.name = '上线'
        } else {
          row.name = '下线'
        }
      } else if (de.type == 'lifetime') {
        if (de.identifier == 'register') {
          row.name = '注册'
        }
      } else if (modeServices && modeServices.length > 0) {
        var ids = de.identifier.split('_reply')
        modeServices.forEach((e) => {
          if (ids[0] == e.identifier) {
            row.name = e.name + (ids.length > 1 ? '回复' : '')
            return
          }
        })
      }

      row.name = row.name + '(' + de.identifier + ')'
      return de
    })

    state.events = logs
  })
}
const showPropertyHistory = (row) => {
  state.currHistoryProperty = row
  state.historyTime = [
    new Date(new Date().getTime() - 2 * 3600 * 1000),
    new Date()
  ]
  refreshPropertyHistory()
}
const loading = ref(false)
const refreshPropertyHistory = () => {
  var end = state.historyTime[1]
  var start = state.historyTime[0]
  loading.value = true
  devicePropertyLogs({
    deviceId: state.deviceId,
    name: state.currHistoryProperty.identifier,
    start: start.getTime(),
    end: end.getTime()
  })
    .then((res) => {
      state.propertyHistory.name = state.currHistoryProperty.name
      state.propertyHistory.data = res
    })
    .finally(() => {
      loading.value = false
    })
}


const timeRangeChange = (e) => {
  refreshPropertyHistory()
}
const channelRef = ref()
const handleClick = (tab) => {
  if (tab.name === 'sipChannel') {
    nextTick(() => {
      channelRef.value.getList()
    })
  } else if (tab.name === 'event') {
    getEvents()
  } else if (tab.name === 'config') {
    loadDeviceConfig()
  } else if (tab.name === 'alertRecord') {
    loadAlertRecords()
  } else if (tab.name === 'remoteControl') {
    // 远程控制Tab由组件自身管理加载
  } else {
    getdata()
  }
}
const showWriteProperty = (prop) => {
  state.propertyWriteFormVisible = true
  state.title = '设置属性'
  state.propertyWriteForm.identifier = prop.identifier
  state.propertyWriteForm.productKey = state.deviceDetail.productKey
  state.propertyWriteForm.dn = state.deviceDetail.dn
  state.propertyWriteForm.value = prop.value?.value ? prop.value.value : prop.value
}
const submitPropertyWriteForm = () => {
  let form = state.propertyWriteForm
  let prop = {}
  prop[form.identifier] = form.value
  propertySet({
    deviceId: state.deviceId,
    args: prop
  }).then(() => {
    ElMessage({
      type: 'success',
      message: '操作成功'
    })

  })
}
const showInvokeService = (service) => {
  state.serviceFormVisible = true
  state.title = '服务调用'
  state.serviceForm.identifier = service.identifier
  state.serviceForm.deviceId = state.deviceDetail.id
  let params: any[] = []
  service.inputData.forEach((p) => {
    params.push({
      identifier: p.identifier,
      name: p.name,
      value: ''
    })
  })
  state.serviceForm.params = params
}
const submitServiceForm = () => {
  let form = state.serviceForm
  let param = {}
  state.serviceForm.params.forEach((p) => {
    param[p.identifier] = p.value
  })

  serviceInvoke({
    deviceId: state.deviceId,
    service: form.identifier,
    args: param
  }).then((res) => {
    if (res.code === 200) {
      state.serviceFormVisible = false
      ElMessage({
        type: 'info',
        message: '操作成功'
      })
    } else {
      ElMessage({
        type: 'error',
        message: res.message
      })
    }
  })
}
const sendDeviceMsg = (fun) => {
  //发送模拟设备消息
  let data = {}
  if (fun.type == 'property') {
    let val = fun.value
    switch (fun.dataTypeName) {
      case 'int32':
        val = parseInt(val, 10)
        if (isNaN(val) || val < Number(fun.raw.dataType.specs.min) || val > Number(fun.raw.dataType.specs.max)) {
          ElMessage({
            type: 'info',
            message: `请输入有效的整数（范围 ${fun.raw.dataType.specs.min} 到 ${fun.raw.dataType.specs.max}）`
          })
          return
        }
        break
      case 'bool':

        break
      case 'enum':
        if (!(val in fun.raw.dataType.specs)) {
          ElMessage({
            type: 'info',
            message: `请输入有效的枚举值（可选值: ${Object.keys(fun.raw.dataType.specs).join(', ')})`
          })
          return
        }
        break
      case 'float':
        val = parseFloat(val)
        if (isNaN(val)) {
          ElMessage({
            type: 'error',
            message: '请输入有效的浮点数'
          })
          return
        }
        break
    }
    data[fun.identifier] = val
  } else {
    try {
      data = JSON.parse(fun.content)
      console.log('Parsed JSON data:', data)
    } catch (e) {
      ElMessage({
        type: 'error',
        message: '无效的 JSON 格式'
      })
      console.error('JSON parse error:', e)
      return
    }
  }

  deviceSimulateSend({
    deviceId: state.deviceId,
    productKey: state.deviceDetail.productKey,
    dn: state.deviceDetail.dn,
    type: fun.type,
    identifier: fun.type == 'property' ? 'report' : fun.identifier,
    data: data
  }).then(() => {
    ElMessage({
      type: 'info',
      message: '操作成功'
    })
  })
}

// 加载设备配置
const loadDeviceConfig = async () => {
  if (!state.deviceId) return
  state.configLoading = true
  try {
    const res: any = await request.post({
      url: '/eiot/device/config/get',
      data: { deviceId: state.deviceId }
    })
    const cfg = res?.data?.config ?? res?.config ?? ''
    if (cfg) {
      try {
        state.configContent = JSON.stringify(JSON.parse(cfg), null, 2)
      } catch {
        state.configContent = cfg
      }
    } else {
      state.configContent = '{}'
    }
  } catch (e) {
    ElMessage.error('获取设备配置失败')
  } finally {
    state.configLoading = false
  }
}

// 保存设备配置
const saveDeviceConfig = async () => {
  if (!state.deviceId) {
    ElMessage.warning('设备ID缺失')
    return
  }
  let formatted = state.configContent
  try {
    const obj = JSON.parse(state.configContent || '{}')
    formatted = JSON.stringify(obj)
  } catch (e) {
    ElMessage.error('配置不是合法的 JSON')
    return
  }
  state.configLoading = true
  try {
    await request.post({
      url: '/eiot/device/config/save',
      data: { deviceId: state.deviceId, config: formatted }
    })
    ElMessage.success('保存成功')
    loadDeviceConfig()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    state.configLoading = false
  }
}

// 格式化配置
const formatConfig = () => {
  try {
    const obj = JSON.parse(state.configContent || '{}')
    state.configContent = JSON.stringify(obj, null, 2)
    ElMessage.success('格式化完成')
  } catch (e) {
    ElMessage.error('格式化失败：内容不是合法 JSON')
  }
}

const closeDialog = () => {
  state.propertyWriteFormVisible = false
  state.serviceFormVisible = false
}
const locateChange = (e) => {
  state.propertyWriteForm.value = e[0] * 1 + ',' + e[1] * 1
}

const openPropertyTable = () => {
  PropertyTableRef.value.open(state)
}

const sipInfo = reactive<any>({
  channelId: undefined
})

const deviceLiveStreamRef = ref()
// 获取直播子组件传递的激活选项卡名称
const getPlayerData = (data) => {
  sipInfo.channelId = data.channelId
  // this.$set(this.form, 'channelId', this.channelId);
  nextTick(() => {
    if (sipInfo.channelId) {
      deviceLiveStreamRef.value.channelId = data.channelId
      deviceLiveStreamRef.value.changeChannel()
    }
  })
}

const formattedProductName = computed(() => {
  return state.product && state.deviceDetail?.productKey
    ? `${state?.product?.name}(${state.deviceDetail?.productKey})`
    : ''
})

const mapCenter = computed(() => {
  if (state.deviceDetail.lon !== undefined && state.deviceDetail.lon !== null && state.deviceDetail.lon !== '' &&
      state.deviceDetail.lat !== undefined && state.deviceDetail.lat !== null && state.deviceDetail.lat !== '') {
    // AMap 使用 "经度,纬度" 格式
    return `${state.deviceDetail.lon},${state.deviceDetail.lat}`
  }
  return ''
})

const deviceAddress = computed(() => {
  return state.deviceDetail.addr || ''
})

const handleLocateChange = (lnglat: string[]) => {
  if (lnglat && lnglat.length >= 2) {
    // AMap 返回 [经度, 纬度]
    state.deviceDetail.lon = lnglat[0]
    state.deviceDetail.lat = lnglat[1]
  }
}

const handleAddressChange = (address: string) => {
  state.deviceDetail.addr = address
}

const getLevelType = (level: string) => {
  const levelMap: Record<string, string> = {
    '1': 'danger',
    '2': 'warning',
    '3': 'info',
    '4': '',
    '5': 'info'
  }
  return levelMap[level] || ''
}

const loadAlertRecords = async () => {
  if (!state.deviceId) return
  state.alertRecordLoading = true
  try {
    const res: any = await getAlertRecordPage({
      pageNo: state.alertRecordPage,
      pageSize: state.alertRecordPageSize,
      deviceId: state.deviceId,
      name: state.alertRecordQuery.name || undefined,
      level: state.alertRecordQuery.level || undefined,
      readFlg: state.alertRecordQuery.status === '' ? undefined : state.alertRecordQuery.status === 'done'
    })
    state.alertRecords = res?.list || []
    state.alertRecordTotal = res?.total || 0
    

  } catch (e) {
    ElMessage.error('获取告警记录失败')
  } finally {
    state.alertRecordLoading = false
  }
}

// 搜索：回到第一页再查询
const onAlertRecordSearch = () => {
  state.alertRecordPage = 1
  loadAlertRecords()
}

// 重置告警记录查询条件
const resetAlertRecordQuery = () => {
  state.alertRecordQuery = {
    name: '',
    level: '',
    status: ''
  }
  state.alertRecordPage = 1
  loadAlertRecords()
}

getdata()
logSearch()
</script>

<style lang="scss" scoped>
.box {
  padding: 15px;
  background: #fff;
}
.form-tips {
  font-size: 12px;
  line-height: 14px;
}
.equipment-param {
  max-height: 160px;
  overflow: hidden auto;
  word-wrap: break-word;
  white-space: pre-wrap;
  font-size: 12px;
  line-height: 14px;
}
.alert-record-tab {
  padding: 10px;
}
.map-card {
  height: 100%;
}
.map-card :deep(.el-card__body) {
  padding: 10px;
}
.card-header {
  font-weight: bold;
  font-size: 14px;
}
</style>
