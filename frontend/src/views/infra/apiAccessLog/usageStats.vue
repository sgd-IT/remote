<template>
  <ContentWrap>
    <!-- 筛选 -->
    <el-form :inline="true" class="-mb-15px">
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="queryParams.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :clearable="false"
          class="!w-260px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">
          <Icon icon="ep:search" class="mr-5px" />查询
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-loading="loading" class="mt-10px">
    <!-- 汇总指标 -->
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in summaryCards" :key="card.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 每日趋势 -->
    <div class="section-title">每日调用趋势</div>
    <div ref="trendEl" style="width: 100%; height: 300px"></div>

    <!-- 排行榜 -->
    <el-row :gutter="16" class="mt-8px">
      <el-col :span="12">
        <div class="section-title">
          接口调用排行 TOP 20
          <el-radio-group v-model="urlRankMode" size="small">
            <el-radio-button label="top">调用最多</el-radio-button>
            <el-radio-button label="bottom">调用最少</el-radio-button>
          </el-radio-group>
        </div>
        <el-table :data="currentUrlRank" stripe border max-height="420">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="请求地址" prop="requestUrl" min-width="240" show-overflow-tooltip />
          <el-table-column label="调用次数" prop="callCount" width="100" align="right" />
          <el-table-column label="用户数" prop="userCount" width="80" align="right" />
        </el-table>
      </el-col>
      <el-col :span="12">
        <div class="section-title">用户活跃排行 TOP 20</div>
        <el-table :data="stats?.userRank || []" stripe border max-height="420">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="用户" min-width="140">
            <template #default="{ row }">{{ displayUser(row) }}</template>
          </el-table-column>
          <el-table-column label="调用次数" prop="callCount" width="100" align="right" />
        </el-table>
      </el-col>
    </el-row>

    <el-alert
      class="mt-16px"
      type="info"
      show-icon
      :closable="false"
      title="统计口径：API 访问日志里的接口调用次数，不调接口的纯前端点击不在统计内；日志会按清理任务保留策略删除，时间越久数据越少。"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { getApiAccessLogStatistics, type ApiAccessLogStatisticsVO } from '@/api/infra/apiAccessLog'
import { useChart } from '@/views/eiot/firewatch/data-center/useChart'

defineOptions({ name: 'InfraApiAccessLogUsageStats' })

const { renderChart } = useChart()

const loading = ref(false)
const stats = ref<ApiAccessLogStatisticsVO>()
const trendEl = ref<HTMLDivElement>()

const fmtDate = (d: Date) =>
  `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

// 默认看最近 7 天（含今天）
const defaultRange = (): string[] => {
  const end = new Date()
  const begin = new Date()
  begin.setDate(begin.getDate() - 6)
  return [fmtDate(begin), fmtDate(end)]
}

const queryParams = reactive<{ dateRange: string[] }>({ dateRange: defaultRange() })

const summaryCards = computed(() => {
  const s = stats.value?.summary
  return [
    { label: '接口调用总量', value: s?.totalCount ?? 0, color: '#409eff' },
    { label: '活跃用户数', value: s?.userCount ?? 0, color: '#67c23a' },
    { label: '涉及接口数', value: s?.urlCount ?? 0, color: '#e6a23c' },
    { label: '日均调用', value: s?.dailyAvg ?? 0, color: '#909399' }
  ]
})

// 用户被删或跨租户查不到昵称时，兜底显示编号
const displayUser = (row: { userId: number; nickname?: string }) => row.nickname || `用户#${row.userId}`

// 接口排行可在“最多/最少”之间切换，两套数据后端一次返回
const urlRankMode = ref<'top' | 'bottom'>('top')
const currentUrlRank = computed(() =>
  urlRankMode.value === 'top' ? stats.value?.urlRank || [] : stats.value?.urlRankBottom || []
)

const renderTrend = () => {
  const trend = stats.value?.trend || []
  renderChart(trendEl.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 24, bottom: 28 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.map((i) => i.date),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#909399' }
    },
    series: [
      {
        name: '调用次数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: trend.map((i) => i.callCount),
        itemStyle: { color: '#409eff' },
        areaStyle: { color: 'rgba(64, 158, 255, 0.12)' }
      }
    ]
  })
}

const getList = async () => {
  loading.value = true
  try {
    const [begin, end] = queryParams.dateRange
    // 后端按 yyyy-MM-dd HH:mm:ss 解析，daterange 只给日期，这里补足当天首尾时间
    const data = await getApiAccessLogStatistics({
      beginTime: `${begin} 00:00:00`,
      endTime: `${end} 23:59:59`
    })
    stats.value = data
    await nextTick()
    renderTrend()
  } finally {
    loading.value = false
  }
}

onMounted(getList)
</script>

<style lang="scss" scoped>
.stat-card {
  text-align: center;

  .stat-value {
    font-size: 26px;
    font-weight: 600;
    line-height: 36px;
  }

  .stat-label {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 16px 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
