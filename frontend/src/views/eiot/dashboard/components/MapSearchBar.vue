<template>
  <div class="map-search-bar" :class="{ 'is-collapsed': isCollapsed }">
    <div v-if="!isCollapsed" class="search-bar-content">
      <el-input
        v-model="searchKeyword"
        :placeholder="$t('dashboard.searchPlaceholder')"
        clearable
        @input="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <Icon icon="ep:search" />
        </template>
      </el-input>

      <el-select
        v-model="statusFilter"
        :placeholder="$t('dashboard.statusFilter')"
        clearable
        @change="handleFilterChange"
        class="filter-select"
      >
        <el-option :label="$t('dashboard.allStatus')" value="" />
        <el-option :label="$t('dashboard.online')" value="online">
          <span class="status-option">
            <span class="status-dot online"></span>
            {{ $t('dashboard.online') }}
          </span>
        </el-option>
        <el-option :label="$t('dashboard.offline')" value="offline">
          <span class="status-option">
            <span class="status-dot offline"></span>
            {{ $t('dashboard.offline') }}
          </span>
        </el-option>
        <el-option :label="$t('dashboard.error')" value="error">
          <span class="status-option">
            <span class="status-dot error"></span>
            {{ $t('dashboard.error') }}
          </span>
        </el-option>
      </el-select>

      <el-select
        v-model="productFilter"
        :placeholder="$t('dashboard.productFilter')"
        clearable
        filterable
        @change="handleFilterChange"
        class="filter-select"
        :loading="productLoading"
      >
        <el-option :label="$t('dashboard.allProducts')" value="" />
        <el-option
          v-for="product in products"
          :key="product.id"
          :label="product.name"
          :value="product.id"
        />
      </el-select>

      <el-button-group class="view-mode-group">
        <el-tooltip :content="$t('dashboard.markerMode')" placement="bottom">
          <el-button
            :type="viewMode === 'marker' ? 'primary' : 'default'"
            @click="handleViewModeChange('marker')"
          >
            <Icon icon="ep:location" />
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('dashboard.clusterMode')" placement="bottom">
          <el-button
            :type="viewMode === 'cluster' ? 'primary' : 'default'"
            @click="handleViewModeChange('cluster')"
          >
            <Icon icon="ep:grid" />
          </el-button>
        </el-tooltip>
        <el-tooltip :content="$t('dashboard.heatmapMode')" placement="bottom">
          <el-button
            :type="viewMode === 'heatmap' ? 'primary' : 'default'"
            @click="handleViewModeChange('heatmap')"
          >
            <Icon icon="ep:histogram" />
          </el-button>
        </el-tooltip>
      </el-button-group>

      <el-button @click="handleClearFilters" :disabled="!hasFilters">
        <Icon icon="ep:close" />
        {{ $t('dashboard.clearFilters') }}
      </el-button>
    </div>

    <el-button
      v-if="isCollapsed"
      class="expand-button"
      @click="isCollapsed = false"
    >
      <Icon icon="ep:search" />
      {{ $t('dashboard.expandSearch') }}
    </el-button>

    <el-button
      v-if="!isCollapsed && showCollapseButton"
      class="collapse-button"
      type="text"
      @click="isCollapsed = true"
    >
      <Icon icon="ep:arrow-up" />
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { debounce } from 'lodash-es'
import { getProducts } from '@/api/eiot/dashboard'

defineOptions({ name: 'MapSearchBar' })

const props = defineProps({
  showCollapseButton: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['search', 'filter-change', 'view-mode-change'])

const searchKeyword = ref('')
const statusFilter = ref('')
const productFilter = ref('')
const viewMode = ref<'marker' | 'cluster' | 'heatmap'>('marker')
const products = ref<any[]>([])
const productLoading = ref(false)
const isCollapsed = ref(false)

const hasFilters = computed(() => {
  return (
    searchKeyword.value !== '' ||
    statusFilter.value !== '' ||
    productFilter.value !== ''
  )
})

const debouncedSearch = debounce(() => {
  emit('search', searchKeyword.value)
}, 300)

const handleSearch = () => {
  debouncedSearch()
}

const handleFilterChange = () => {
  emit('filter-change', {
    keyword: searchKeyword.value,
    status: statusFilter.value,
    productKey: productFilter.value
  })
}

const handleViewModeChange = (mode: 'marker' | 'cluster' | 'heatmap') => {
  viewMode.value = mode
  emit('view-mode-change', mode)
}

const handleClearFilters = () => {
  searchKeyword.value = ''
  statusFilter.value = ''
  productFilter.value = ''
  emit('filter-change', {
    keyword: '',
    status: '',
    productKey: ''
  })
}

const fetchProducts = async () => {
  try {
    productLoading.value = true
    products.value = await getProducts()
  } catch (error) {
    console.error('Failed to fetch products:', error)
  } finally {
    productLoading.value = false
  }
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped lang="scss">
.map-search-bar {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;

  &.is-collapsed {
    padding: 8px 16px;
  }
}

.search-bar-content {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.filter-select {
  width: 160px;
}

.view-mode-group {
  :deep(.el-button) {
    padding: 8px 12px;
  }
}

.status-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.online {
    background: #67c23a;
  }

  &.offline {
    background: #909399;
  }

  &.error {
    background: #f56c6c;
  }
}

.expand-button,
.collapse-button {
  padding: 8px 16px;
}

.collapse-button {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
}

@media (max-width: 768px) {
  .search-bar-content {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .filter-select {
    width: 100%;
  }

  .view-mode-group {
    width: 100%;
    display: flex;

    .el-button {
      flex: 1;
    }
  }
}
</style>
