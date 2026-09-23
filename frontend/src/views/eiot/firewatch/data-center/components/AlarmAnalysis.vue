<template>
  <div class="analysis-grid">
    <!-- 告警趋势 -->
    <ChartCard title="告警趋势" :icon="TrendCharts" :loading="loading" :empty="!alarmTrend.length" height="280px">
      <div class="data-list">
        <div v-for="d in alarmTrend.slice(-14)" :key="d.date" class="data-row">
          <span class="data-label">{{ d.date?.substring(5) }}</span>
          <span class="data-val red">{{ d.alarmCount || 0 }} 告警</span>
          <span class="data-val green">{{ d.handleCount || 0 }} 处置</span>
        </div>
      </div>
    </ChartCard>

    <!-- 告警类型分布 -->
    <ChartCard title="告警类型分布" :icon="DataLine" :loading="loading" :empty="!alarmTypes.length" height="280px">
      <div class="data-list">
        <div v-for="d in alarmTypes" :key="d.typeName" class="data-row">
          <span class="data-label">{{ d.typeName }}</span>
          <span class="data-val">{{ d.count }}</span>
        </div>
      </div>
    </ChartCard>

    <!-- 告警等级分布 -->
    <ChartCard title="告警等级分布" :icon="SetUp" :loading="loading" :empty="!alarmLevels.length" height="280px">
      <div class="data-list">
        <div v-for="d in alarmLevels" :key="d.levelName" class="data-row">
          <span class="data-label">{{ d.levelName }}</span>
          <span class="data-val">{{ d.count }}</span>
        </div>
      </div>
    </ChartCard>

    <!-- 各物业告警排名 -->
    <ChartCard title="物业告警排名" :icon="Rank" :loading="loading" :empty="!propertyRank.length" height="280px">
      <div class="data-list">
        <div v-for="(d, i) in propertyRank" :key="d.propertyName" class="data-row">
          <span class="data-label">{{ i + 1 }}. {{ d.propertyName }}</span>
          <span class="data-val">{{ d.alarmCount }}</span>
        </div>
      </div>
    </ChartCard>

    <!-- 24h告警时段分布 -->
    <ChartCard title="24h告警时段分布" :icon="Clock" :loading="loading" :empty="!heatmap.length" height="280px">
      <div class="data-list">
        <div v-for="d in heatmap" :key="d.hour" class="data-row">
          <span class="data-label">{{ d.hour }}:00</span>
          <span class="data-val">{{ d.count }}</span>
        </div>
      </div>
    </ChartCard>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { TrendCharts, DataLine, SetUp, Rank, Clock } from '@element-plus/icons-vue'
import ChartCard from './ChartCard.vue'
import {
  getAlarmTrend,
  getAlarmTypeDistribution,
  getAlarmPropertyRank,
  getAlarmHourlyHeatmap,
  getAlarmLevelDistribution
} from '@/api/eiot/firewatch/dataCenter.api'
import {
  mockAlarmTrend, mockAlarmTypeDistribution, mockAlarmLevelDistribution,
  mockPropertyAlarmRank, mockAlarmHourlyHeatmap
} from '../mock'

const props = defineProps<{ propertyId?: number; days: number }>()

const loading = ref(false)
const alarmTrend = ref<any[]>([])
const alarmTypes = ref<any[]>([])
const alarmLevels = ref<any[]>([])
const propertyRank = ref<any[]>([])
const heatmap = ref<any[]>([])

const safe = async (fn: () => Promise<any>) => {
  try { return await fn() } catch { return null }
}

const loadData = async () => {
  loading.value = true
  try {
    const [t, tp, r, h, l] = await Promise.all([
      safe(() => getAlarmTrend(props.days, props.propertyId)),
      safe(() => getAlarmTypeDistribution(props.propertyId)),
      safe(() => getAlarmPropertyRank(props.days)),
      safe(() => getAlarmHourlyHeatmap(props.propertyId)),
      safe(() => getAlarmLevelDistribution(props.propertyId)),
    ])
    alarmTrend.value = (t && t.length) ? t : mockAlarmTrend
    alarmTypes.value = (tp && tp.length) ? tp : mockAlarmTypeDistribution
    propertyRank.value = (r && r.length) ? r : mockPropertyAlarmRank
    heatmap.value = (h && h.length) ? h : mockAlarmHourlyHeatmap
    alarmLevels.value = (l && l.length) ? l : mockAlarmLevelDistribution
  } finally {
    loading.value = false
  }
}

watch(() => [props.propertyId, props.days], () => loadData())
onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  width: 100%;
}

.data-list {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  &::-webkit-scrollbar { display: none; }
}

.data-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  border-bottom: 1px solid rgba(79,195,247,0.06);
  font-size: 12px;

  .data-label {
    color: #8b9dc5;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 55%;
  }
  .data-val {
    color: #fff;
    font-weight: 500;
    &.red { color: #ff4d4f; }
    &.green { color: #52c41a; }
  }
}
</style>
