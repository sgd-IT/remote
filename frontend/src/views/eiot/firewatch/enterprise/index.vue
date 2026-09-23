<template>
  <div class="enterprise-container">
    <!-- ========== 企业账号视图：只展示本企业信息 ========== -->
    <template v-if="!isAdmin">
      <el-card shadow="never" class="detail-card">
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><OfficeBuilding /></el-icon>
              企业信息
            </span>
            <el-button type="primary" @click="handleEditOwn" v-if="!editingOwn">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <div v-else>
              <el-button @click="editingOwn = false">取消</el-button>
              <el-button type="primary" @click="handleSaveOwn" :loading="submitLoading">保存</el-button>
            </div>
          </div>
        </template>

        <!-- 只读详情展示 -->
        <div v-if="!editingOwn" v-loading="loading">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="企业名称">{{ ownEnterprise.propertyName }}</el-descriptions-item>
            <el-descriptions-item label="统一社会信用代码">{{ ownEnterprise.creditCode }}</el-descriptions-item>
            <el-descriptions-item label="企业类型">{{ ownEnterprise.propertyType }}</el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="getRiskTagType(ownEnterprise.riskLevel)">
                {{ getRiskLevelText(ownEnterprise.riskLevel) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="联系人">{{ ownEnterprise.contactName }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ ownEnterprise.contactPhone }}</el-descriptions-item>
            <el-descriptions-item label="企业地址" :span="2">{{ ownEnterprise.address }}</el-descriptions-item>
            <el-descriptions-item label="主要负责人">{{ ownEnterprise.mainPerson || '-' }}</el-descriptions-item>
            <el-descriptions-item label="安全管理人">{{ ownEnterprise.safetyPerson || '-' }}</el-descriptions-item>
            <el-descriptions-item label="建筑面积（㎡）">{{ ownEnterprise.buildingArea || '-' }}</el-descriptions-item>
            <el-descriptions-item label="建筑层数">{{ ownEnterprise.buildingFloors || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ ownEnterprise.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(ownEnterprise.createTime, 'YYYY-MM-DD') }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ ownEnterprise.updateTime }}</el-descriptions-item>
          </el-descriptions>

          <!-- 许可证信息 -->
          <div class="detail-section">
            <div class="section-title">许可证信息</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="许可证编号">{{ ownEnterprise.licenseNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="发证机关">{{ ownEnterprise.licenseAuthority || '-' }}</el-descriptions-item>
              <el-descriptions-item label="有效期开始">{{ ownEnterprise.licenseStartDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="有效期截止">{{ ownEnterprise.licenseEndDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="许可范围" :span="2">{{ ownEnterprise.licenseScope || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 仓储信息 -->
          <div class="detail-section">
            <div class="section-title">仓储信息</div>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="仓储类型">{{ ownEnterprise.storageType || '-' }}</el-descriptions-item>
              <el-descriptions-item label="仓储面积（㎡）">{{ ownEnterprise.storageArea || '-' }}</el-descriptions-item>
              <el-descriptions-item label="仓储容量">{{ ownEnterprise.storageCapacity || '-' }}</el-descriptions-item>
              <el-descriptions-item label="存储物品类型">{{ ownEnterprise.storageGoodsType || '-' }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 设备统计 -->
          <div class="detail-section" v-if="ownEnterprise.deviceStats">
            <div class="section-title">设备统计</div>
            <el-row :gutter="16">
              <el-col :span="6">
                <div class="stat-card total">
                  <div class="stat-value">{{ ownEnterprise.deviceStats?.total || 0 }}</div>
                  <div class="stat-label">设备总数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card normal">
                  <div class="stat-value">{{ ownEnterprise.deviceStats?.normal || 0 }}</div>
                  <div class="stat-label">正常运行</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card warning">
                  <div class="stat-value">{{ ownEnterprise.deviceStats?.warning || 0 }}</div>
                  <div class="stat-label">告警设备</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card fault">
                  <div class="stat-value">{{ ownEnterprise.deviceStats?.fault || 0 }}</div>
                  <div class="stat-label">故障设备</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- 编辑表单 -->
        <el-form v-else ref="ownFormRef" :model="ownFormData" :rules="ownFormRules" label-width="130px">
          <div class="section-title" style="margin-bottom: 16px;">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="企业名称" prop="name">
                <el-input v-model="ownFormData.name" placeholder="请输入企业名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="信用代码" prop="creditCode">
                <el-input v-model="ownFormData.creditCode" placeholder="请输入统一社会信用代码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="企业类型" prop="propertyType">
                <el-select v-model="ownFormData.propertyType" placeholder="请选择企业类型" style="width: 100%">
                  <el-option label="生产企业" value="生产企业" />
                  <el-option label="商业综合体" value="商业综合体" />
                  <el-option label="写字楼" value="写字楼" />
                  <el-option label="住宅小区" value="住宅小区" />
                  <el-option label="学校" value="学校" />
                  <el-option label="医院" value="医院" />
                  <el-option label="酒店" value="酒店" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="风险等级" prop="riskLevel">
                <el-select v-model="ownFormData.riskLevel" placeholder="请选择风险等级" style="width: 100%">
                  <el-option label="重大风险" value="critical" />
                  <el-option label="较大风险" value="major" />
                  <el-option label="一般风险" value="normal" />
                  <el-option label="较小风险" value="minor" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactName">
                <el-input v-model="ownFormData.contactName" placeholder="请输入联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="ownFormData.contactPhone" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="主要负责人">
                <el-input v-model="ownFormData.mainPerson" placeholder="请输入主要负责人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="安全管理人">
                <el-input v-model="ownFormData.safetyPerson" placeholder="请输入安全管理人" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="企业地址" prop="address">
            <el-input v-model="ownFormData.address" placeholder="请输入企业地址" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="建筑面积（㎡）">
                <el-input v-model="ownFormData.buildingArea" placeholder="请输入建筑面积" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="建筑层数">
                <el-input v-model="ownFormData.buildingFloors" placeholder="请输入建筑层数" />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="section-title" style="margin: 24px 0 16px;">许可证信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="许可证编号">
                <el-input v-model="ownFormData.licenseNo" placeholder="请输入许可证编号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发证机关">
                <el-input v-model="ownFormData.licenseAuthority" placeholder="请输入发证机关" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="有效期开始">
                <el-date-picker v-model="ownFormData.licenseStartDate" type="date" placeholder="请选择开始日期" style="width: 100%" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="有效期截止">
                <el-date-picker v-model="ownFormData.licenseEndDate" type="date" placeholder="请选择截止日期" style="width: 100%" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="许可范围">
            <el-input v-model="ownFormData.licenseScope" placeholder="请输入许可范围" />
          </el-form-item>

          <div class="section-title" style="margin: 24px 0 16px;">仓储信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="仓储类型">
                <el-select v-model="ownFormData.storageType" placeholder="请选择仓储类型" style="width: 100%">
                  <el-option label="普通仓库" value="普通仓库" />
                  <el-option label="危化品仓库" value="危化品仓库" />
                  <el-option label="冷藏仓库" value="冷藏仓库" />
                  <el-option label="露天堆场" value="露天堆场" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="仓储面积（㎡）">
                <el-input v-model="ownFormData.storageArea" placeholder="请输入仓储面积" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="仓储容量">
                <el-input v-model="ownFormData.storageCapacity" placeholder="请输入仓储容量" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="存储物品类型">
                <el-input v-model="ownFormData.storageGoodsType" placeholder="请输入存储物品类型" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="备注">
            <el-input v-model="ownFormData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <!-- ========== 管理员视图：全部企业管理 ========== -->
    <template v-else>
      <!-- 搜索区域 -->
      <el-card shadow="never" class="search-card">
        <el-form :model="queryParams" inline>
          <el-form-item label="企业名称">
            <el-input v-model="queryParams.name" placeholder="请输入企业名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="风险等级">
            <el-select v-model="queryParams.riskLevel" placeholder="请选择风险等级" clearable style="width: 150px">
              <el-option label="重大风险" value="critical" />
              <el-option label="较大风险" value="major" />
              <el-option label="一般风险" value="normal" />
              <el-option label="较小风险" value="minor" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 操作区域 -->
      <el-card shadow="never" class="table-card">
        <template #header>
          <div class="card-header">
            <span class="title">
              <el-icon><OfficeBuilding /></el-icon>
              企业信息管理
            </span>
            <div class="header-actions">
              <el-button type="primary" @click="handleAdd">
                <el-icon><Plus /></el-icon>
                新增企业
              </el-button>
              <el-button @click="loadData">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </div>
        </template>

        <!-- 表格 -->
        <el-table :data="tableData" v-loading="loading" style="width: 100%" border>
          <el-table-column prop="propertyName" label="企业名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="creditCode" label="统一社会信用代码" width="200" show-overflow-tooltip />
          <el-table-column prop="contactName" label="联系人" width="100" />
          <el-table-column prop="contactPhone" label="联系电话" width="130" />
          <el-table-column prop="address" label="地址" min-width="200" show-overflow-tooltip />
          <el-table-column label="风险等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getRiskTagType(row.riskLevel)">
                {{ getRiskLevelText(row.riskLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="propertyType" label="企业类型" width="120" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleView(row)">
                <el-icon><View /></el-icon>
                查看
              </el-button>
              <el-button type="primary" link @click="handleEdit(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button type="danger" link @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="queryParams.pageNo"
            v-model:page-size="queryParams.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadData"
            @current-change="loadData"
          />
        </div>
      </el-card>

      <!-- 新增/编辑弹窗 -->
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" destroy-on-close>
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
          <div class="section-title" style="margin-bottom: 16px;">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="企业名称" prop="name">
                <el-input v-model="formData.name" placeholder="请输入企业名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="信用代码" prop="creditCode">
                <el-input v-model="formData.creditCode" placeholder="请输入统一社会信用代码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactName">
                <el-input v-model="formData.contactName" placeholder="请输入联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="风险等级" prop="riskLevel">
                <el-select v-model="formData.riskLevel" placeholder="请选择风险等级" style="width: 100%">
                  <el-option label="重大风险" value="critical" />
                  <el-option label="较大风险" value="major" />
                  <el-option label="一般风险" value="normal" />
                  <el-option label="较小风险" value="minor" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="企业类型" prop="propertyType">
                <el-select v-model="formData.propertyType" placeholder="请选择企业类型" style="width: 100%">
                  <el-option label="生产企业" value="生产企业" />
                  <el-option label="商业综合体" value="商业综合体" />
                  <el-option label="写字楼" value="写字楼" />
                  <el-option label="住宅小区" value="住宅小区" />
                  <el-option label="学校" value="学校" />
                  <el-option label="医院" value="医院" />
                  <el-option label="酒店" value="酒店" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="主要负责人">
                <el-input v-model="formData.mainPerson" placeholder="请输入主要负责人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="安全管理人">
                <el-input v-model="formData.safetyPerson" placeholder="请输入安全管理人" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="企业地址" prop="address">
            <el-input v-model="formData.address" placeholder="请输入企业地址" />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="建筑面积（㎡）">
                <el-input v-model="formData.buildingArea" placeholder="请输入建筑面积" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="建筑层数">
                <el-input v-model="formData.buildingFloors" placeholder="请输入建筑层数" />
              </el-form-item>
            </el-col>
          </el-row>

          <div class="section-title" style="margin: 24px 0 16px;">许可证信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="许可证编号">
                <el-input v-model="formData.licenseNo" placeholder="请输入许可证编号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发证机关">
                <el-input v-model="formData.licenseAuthority" placeholder="请输入发证机关" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="有效期开始">
                <el-date-picker v-model="formData.licenseStartDate" type="date" placeholder="请选择开始日期" style="width: 100%" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="有效期截止">
                <el-date-picker v-model="formData.licenseEndDate" type="date" placeholder="请选择截止日期" style="width: 100%" value-format="YYYY-MM-DD" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="许可范围">
            <el-input v-model="formData.licenseScope" placeholder="请输入许可范围" />
          </el-form-item>

          <div class="section-title" style="margin: 24px 0 16px;">仓储信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="仓储类型">
                <el-select v-model="formData.storageType" placeholder="请选择仓储类型" style="width: 100%">
                  <el-option label="普通仓库" value="普通仓库" />
                  <el-option label="危化品仓库" value="危化品仓库" />
                  <el-option label="冷藏仓库" value="冷藏仓库" />
                  <el-option label="露天堆场" value="露天堆场" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="仓储面积（㎡）">
                <el-input v-model="formData.storageArea" placeholder="请输入仓储面积" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="仓储容量">
                <el-input v-model="formData.storageCapacity" placeholder="请输入仓储容量" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="存储物品类型">
                <el-input v-model="formData.storageGoodsType" placeholder="请输入存储物品类型" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="备注" prop="remark">
            <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
        </template>
      </el-dialog>

      <!-- 详情弹窗 -->
      <el-dialog v-model="detailVisible" title="企业详情" width="700px" destroy-on-close>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="企业名称">{{ currentRow.propertyName }}</el-descriptions-item>
          <el-descriptions-item label="统一社会信用代码">{{ currentRow.creditCode }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentRow.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentRow.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="企业地址" :span="2">{{ currentRow.address }}</el-descriptions-item>
          <el-descriptions-item label="风险等级">
            <el-tag :type="getRiskTagType(currentRow.riskLevel)">
              {{ getRiskLevelText(currentRow.riskLevel) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="企业类型">{{ currentRow.propertyType }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentRow.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(currentRow.createTime, 'YYYY-MM-DD') }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentRow.updateTime }}</el-descriptions-item>
        </el-descriptions>

        <!-- 设备统计 -->
        <div class="detail-section" v-if="currentRow.deviceStats">
          <div class="section-title">设备统计</div>
          <el-row :gutter="16">
            <el-col :span="6">
              <div class="stat-card total">
                <div class="stat-value">{{ currentRow.deviceStats?.total || 0 }}</div>
                <div class="stat-label">设备总数</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card normal">
                <div class="stat-value">{{ currentRow.deviceStats?.normal || 0 }}</div>
                <div class="stat-label">正常运行</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card warning">
                <div class="stat-value">{{ currentRow.deviceStats?.warning || 0 }}</div>
                <div class="stat-label">告警设备</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card fault">
                <div class="stat-value">{{ currentRow.deviceStats?.fault || 0 }}</div>
                <div class="stat-label">故障设备</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, FormInstance, FormRules } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete, View, OfficeBuilding } from '@element-plus/icons-vue'
import { formatDate } from '@/utils/formatTime'
import request from '@/config/axios'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'FireWatchEnterprise' })

// ========== 角色判断 ==========
const userStore = useUserStore()
const isAdmin = computed(() => {
  // 优先从store读取，兜底从localStorage读取
  let roles = userStore.getRoles
  if (!roles || roles.length === 0) {
    try {
      const userInfo = JSON.parse(localStorage.getItem('user') || '{}')
      roles = userInfo.roles || []
    } catch {
      roles = []
    }
  }
  return roles.some((r: string) => r === 'super_admin')
})

// ========== 企业账号 - 本企业数据 ==========
const ownEnterprise = ref<any>({})
const editingOwn = ref(false)
const ownFormRef = ref<FormInstance>()
const ownFormData = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  creditCode: '',
  contactName: '',
  contactPhone: '',
  address: '',
  riskLevel: 'normal',
  propertyType: '',
  customerType: '',
  mainPerson: '',
  safetyPerson: '',
  buildingArea: '',
  buildingFloors: '',
  floorCount: undefined as number | undefined,
  licenseNo: '',
  licenseAuthority: '',
  licenseStartDate: '',
  licenseEndDate: '',
  licenseScope: '',
  storageType: '',
  storageArea: '',
  storageCapacity: '',
  storageGoodsType: '',
  remark: '',
  status: 1
})
const ownFormRules: FormRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入企业地址', trigger: 'blur' }]
}

// 加载本企业数据
const loadOwnEnterprise = async () => {
  loading.value = true
  try {
    const res = await request.get({ url: '/eiot/firewatch/property/own' })
    // request.get 已自动解包 CommonResult.data，res 就是企业数据
    if (res) {
      ownEnterprise.value = res
    } else {
      loadOwnMockData()
    }
  } catch (error) {
    console.error('Load own enterprise error:', error)
    loadOwnMockData()
  } finally {
    loading.value = false
  }
}

// 企业账号模拟数据
const loadOwnMockData = () => {
  ownEnterprise.value = {
    id: 1,
    propertyName: '安为科技示范单位',
    creditCode: '91440300MA5EXXXX',
    contactName: '张经理',
    contactPhone: '13800138001',
    address: '广东省深圳市南山区科技园南区A栋3楼',
    riskLevel: 'normal',
    propertyType: '生产企业',
    mainPerson: '王总',
    safetyPerson: '李安全',
    buildingArea: '5000',
    buildingFloors: '6',
    licenseNo: 'XK-2025-00123',
    licenseAuthority: '深圳市应急管理局',
    licenseStartDate: '2025-01-01',
    licenseEndDate: '2028-12-31',
    licenseScope: '消防设备生产、安装、维护',
    storageType: '普通仓库',
    storageArea: '1200',
    storageCapacity: '500吨',
    storageGoodsType: '消防器材、电子设备',
    remark: '消防重点单位，定期巡检',
    createTime: '2026-01-15 10:00:00',
    updateTime: '2026-04-10 15:30:00',
    deviceStats: { total: 156, normal: 142, warning: 5, fault: 3 }
  }
}

// 编辑本企业信息
const handleEditOwn = () => {
  Object.assign(ownFormData, {
    id: ownEnterprise.value.id,
    name: ownEnterprise.value.name || ownEnterprise.value.propertyName || '',
    code: ownEnterprise.value.code || '',
    creditCode: ownEnterprise.value.creditCode || '',
    contactName: ownEnterprise.value.contactName || '',
    contactPhone: ownEnterprise.value.contactPhone || '',
    address: ownEnterprise.value.address || '',
    riskLevel: ownEnterprise.value.riskLevel || 'normal',
    propertyType: ownEnterprise.value.propertyType || '',
    customerType: ownEnterprise.value.customerType || '',
    mainPerson: ownEnterprise.value.mainPerson || '',
    safetyPerson: ownEnterprise.value.safetyPerson || '',
    buildingArea: ownEnterprise.value.buildingArea ?? '',
    buildingFloors: ownEnterprise.value.buildingFloors || '',
    floorCount: ownEnterprise.value.floorCount ?? undefined,
    licenseNo: ownEnterprise.value.licenseNo || '',
    licenseAuthority: ownEnterprise.value.licenseAuthority || '',
    licenseStartDate: ownEnterprise.value.licenseStartDate || '',
    licenseEndDate: ownEnterprise.value.licenseEndDate || '',
    licenseScope: ownEnterprise.value.licenseScope || '',
    storageType: ownEnterprise.value.storageType || '',
    storageArea: ownEnterprise.value.storageArea || '',
    storageCapacity: ownEnterprise.value.storageCapacity || '',
    storageGoodsType: ownEnterprise.value.storageGoodsType || '',
    remark: ownEnterprise.value.remark || '',
    status: ownEnterprise.value.status ?? 1
  })
  editingOwn.value = true
}

// 保存本企业信息
const handleSaveOwn = async () => {
  if (!ownFormRef.value) return
  await ownFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const submitData = { ...ownFormData }
      // 空字符串转为 null，避免后端反序列化失败
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const sd: any = submitData
      if (sd.buildingArea === '') sd.buildingArea = null
      if (sd.floorCount === '') sd.floorCount = null

      try {
        // 先尝试更新
        await request.put({ url: '/eiot/firewatch/property/update', data: submitData })
      } catch (updateError) {
        // 更新失败（物业还不存在），自动创建
        console.warn('更新失败，尝试创建企业:', updateError)
        const { id, code, ...createData } = submitData
        await request.post({ url: '/eiot/firewatch/property/create', data: createData })
      }

      ElMessage.success('保存成功')
      editingOwn.value = false
      await loadOwnEnterprise()
    } catch (error) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败，请重试或联系管理员')
    } finally {
      submitLoading.value = false
    }
  })
}

// ========== 管理员 - 全部企业管理 ==========
// 查询参数
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  riskLevel: ''
})

// 表格数据
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增企业')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const currentRow = ref<any>({})

// 表单数据
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  creditCode: '',
  contactName: '',
  contactPhone: '',
  address: '',
  riskLevel: 'normal',
  propertyType: '',
  customerType: '',
  mainPerson: '',
  safetyPerson: '',
  buildingArea: '',
  buildingFloors: '',
  floorCount: undefined as number | undefined,
  licenseNo: '',
  licenseAuthority: '',
  licenseStartDate: '',
  licenseEndDate: '',
  licenseScope: '',
  storageType: '',
  storageArea: '',
  storageCapacity: '',
  storageGoodsType: '',
  remark: '',
  status: 1
})

// 表单校验规则
const formRules: FormRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入企业地址', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  propertyType: [{ required: true, message: '请选择企业类型', trigger: 'change' }]
}

// 获取风险等级文本
const getRiskLevelText = (level: string) => {
  const map: Record<string, string> = {
    critical: '重大风险',
    major: '较大风险',
    normal: '一般风险',
    minor: '较小风险'
  }
  return map[level] || '未评估'
}

// 获取风险等级标签类型
const getRiskTagType = (level: string): 'success' | 'warning' | 'info' | 'primary' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary' | 'danger'> = {
    critical: 'danger',
    major: 'warning',
    normal: 'success',
    minor: 'info'
  }
  return map[level] || 'info'
}

// 加载数据（管理员）
const loadData = async () => {
  loading.value = true
  try {
    // 调试日志
    console.log('[Enterprise] queryParams:', JSON.stringify(queryParams))
    // 管理员使用 list-all 接口（跨租户，能查到所有企业数据）
    const res = await request.get({ url: '/eiot/firewatch/property/list-all', params: queryParams })
    console.log('[Enterprise] response:', JSON.stringify(res))
    // request.get 已自动解包 CommonResult.data，res 就是分页数据 { list, total }
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } catch (error) {
    console.error('[Enterprise] Load data error:', error)
    loadAdminMockData()
  } finally {
    loading.value = false
  }
}

// 管理员模拟数据
const loadAdminMockData = () => {
  tableData.value = [
    {
      id: 1,
      propertyName: '安为科技示范单位',
      creditCode: '91440300MA5EXXXX',
      contactName: '张经理',
      contactPhone: '13800138001',
      address: '广东省深圳市南山区科技园',
      riskLevel: 'normal',
      propertyType: '生产企业',
      remark: '消防重点单位',
      createTime: '2026-01-15 10:00:00',
      updateTime: '2026-04-10 15:30:00',
      deviceStats: { total: 156, normal: 142, warning: 5, fault: 3 }
    },
    {
      id: 2,
      propertyName: '广州天河广场',
      creditCode: '91440100MA5EXYYY',
      contactName: '李经理',
      contactPhone: '13900139002',
      address: '广东省广州市天河区天河路',
      riskLevel: 'major',
      propertyType: '商业综合体',
      remark: '',
      createTime: '2026-02-20 09:00:00',
      updateTime: '2026-04-08 11:20:00',
      deviceStats: { total: 289, normal: 270, warning: 12, fault: 7 }
    },
    {
      id: 3,
      propertyName: '佛山智慧小区',
      creditCode: '91440600MA5EZZZZ',
      contactName: '王主任',
      contactPhone: '13700137003',
      address: '广东省佛山市顺德区大良街道',
      riskLevel: 'minor',
      propertyType: '住宅小区',
      remark: '新建小区',
      createTime: '2026-03-01 14:00:00',
      updateTime: '2026-04-12 16:45:00',
      deviceStats: { total: 78, normal: 75, warning: 2, fault: 1 }
    },
    {
      id: 4,
      propertyName: '深圳湾创新科技园',
      creditCode: '91440300MA5EAAAA',
      contactName: '陈总',
      contactPhone: '13600136004',
      address: '广东省深圳市南山区深圳湾科技生态园',
      riskLevel: 'critical',
      propertyType: '写字楼',
      remark: '高层建筑',
      createTime: '2026-01-20 08:30:00',
      updateTime: '2026-04-15 09:00:00',
      deviceStats: { total: 320, normal: 298, warning: 15, fault: 7 }
    },
    {
      id: 5,
      propertyName: '东莞松山湖学校',
      creditCode: '91441900MA5EBBBB',
      contactName: '刘校长',
      contactPhone: '13500135005',
      address: '广东省东莞市松山湖高新技术开发区',
      riskLevel: 'major',
      propertyType: '学校',
      remark: '重点教育单位',
      createTime: '2026-02-10 11:00:00',
      updateTime: '2026-04-13 14:20:00',
      deviceStats: { total: 95, normal: 88, warning: 4, fault: 3 }
    }
  ]
  total.value = 5
}

// 搜索
const handleSearch = () => {
  queryParams.pageNo = 1
  loadData()
}

// 重置
const handleReset = () => {
  queryParams.name = ''
  queryParams.riskLevel = ''
  queryParams.pageNo = 1
  loadData()
}

// 重置表单
const resetForm = () => {
  formData.id = undefined
  formData.name = ''
  formData.code = ''
  formData.creditCode = ''
  formData.contactName = ''
  formData.contactPhone = ''
  formData.address = ''
  formData.riskLevel = 'normal'
  formData.propertyType = ''
  formData.customerType = ''
  formData.mainPerson = ''
  formData.safetyPerson = ''
  formData.buildingArea = ''
  formData.buildingFloors = ''
  formData.floorCount = undefined
  formData.licenseNo = ''
  formData.licenseAuthority = ''
  formData.licenseStartDate = ''
  formData.licenseEndDate = ''
  formData.licenseScope = ''
  formData.storageType = ''
  formData.storageArea = ''
  formData.storageCapacity = ''
  formData.storageGoodsType = ''
  formData.remark = ''
  formData.status = 1
}

// 新增
const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增企业'
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: any) => {
  resetForm()
  dialogTitle.value = '编辑企业'
  Object.assign(formData, {
    id: row.id,
    name: row.name || row.propertyName || '',
    code: row.code || '',
    creditCode: row.creditCode || '',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    address: row.address || '',
    riskLevel: row.riskLevel || 'normal',
    propertyType: row.propertyType || '',
    customerType: row.customerType || '',
    mainPerson: row.mainPerson || '',
    safetyPerson: row.safetyPerson || '',
    buildingArea: row.buildingArea ?? '',
    buildingFloors: row.buildingFloors || '',
    floorCount: row.floorCount ?? undefined,
    licenseNo: row.licenseNo || '',
    licenseAuthority: row.licenseAuthority || '',
    licenseStartDate: row.licenseStartDate || '',
    licenseEndDate: row.licenseEndDate || '',
    licenseScope: row.licenseScope || '',
    storageType: row.storageType || '',
    storageArea: row.storageArea || '',
    storageCapacity: row.storageCapacity || '',
    storageGoodsType: row.storageGoodsType || '',
    remark: row.remark || '',
    status: row.status ?? 1
  })
  dialogVisible.value = true
}

// 查看详情
const handleView = async (row: any) => {
  try {
    const res = await request.get({ url: '/eiot/firewatch/property/get', params: { id: row.id } })
    currentRow.value = res || row
  } catch (error) {
    currentRow.value = row
  }
  detailVisible.value = true
}

// 删除
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除企业"${row.propertyName}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    try {
      await request.delete({ url: '/eiot/firewatch/property/delete', params: { id: row.id } })
      ElMessage.success('删除成功')
      await loadData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请重试')
    }
  } catch {
    // 用户取消
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      // 构建提交数据，处理类型转换
      const submitData = { ...formData }
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const sd: any = submitData
      if (sd.buildingArea === '') sd.buildingArea = null
      if (sd.floorCount === '') sd.floorCount = null

      const url = submitData.id ? `/eiot/firewatch/property/update` : `/eiot/firewatch/property/create`
      const method = submitData.id ? 'put' : 'post'
      await request[method]({ url, data: submitData })
      ElMessage.success(submitData.id ? '编辑成功' : '新增成功')
      dialogVisible.value = false
      await loadData()
    } catch (error) {
      console.error('Submit error:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

onMounted(() => {
  if (isAdmin.value) {
    loadData()
  } else {
    loadOwnEnterprise()
  }
})
</script>

<style lang="scss" scoped>
.enterprise-container {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.detail-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: bold;
    }
  }
}

.table-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: bold;
    }
  }
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-section {
  margin-top: 24px;

  .section-title {
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 12px;
    padding-left: 10px;
    border-left: 3px solid var(--el-color-primary);
  }
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  padding-left: 10px;
  border-left: 3px solid var(--el-color-primary);
}

.stat-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
  border-radius: 8px;
  padding: 16px;
  text-align: center;

  &.total {
    background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
    color: #fff;
  }

  &.normal {
    background: linear-gradient(135deg, #67c23a 0%, #529b2e 100%);
    color: #fff;
  }

  &.warning {
    background: linear-gradient(135deg, #e6a23c 0%, #c48a3a 100%);
    color: #fff;
  }

  &.fault {
    background: linear-gradient(135deg, #f56c6c 0%, #c45656 100%);
    color: #fff;
  }

  .stat-value {
    font-size: 24px;
    font-weight: bold;
  }

  .stat-label {
    font-size: 12px;
    margin-top: 4px;
    opacity: 0.9;
  }
}
</style>
