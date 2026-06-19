<template>
  <div class="airspace-record-page">
    <PageHeader title="空域备案" subtitle="提交无人机飞行空域备案申请" />

    <div class="tab-container">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="新增备案" name="add">
          <GlassCard class="form-card">
            <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
              <el-form-item label="飞行区域名称" prop="regionName">
                <el-input v-model="formData.regionName" placeholder="请输入飞行区域名称" />
              </el-form-item>

              <el-form-item label="飞行区域地址" prop="regionAddress">
                <el-input v-model="formData.regionAddress" placeholder="请输入飞行区域详细地址" />
              </el-form-item>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="经度" prop="longitude">
                    <el-input v-model="formData.longitude" placeholder="请输入经度" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="纬度" prop="latitude">
                    <el-input v-model="formData.latitude" placeholder="请输入纬度" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="飞行半径(米)" prop="radius">
                    <el-input-number v-model="formData.radius" :min="1" :max="10000" placeholder="飞行半径" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="最大飞行高度(米)" prop="maxAltitude">
                    <el-input-number v-model="formData.maxAltitude" :min="1" :max="500" placeholder="最大高度" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="计划开始时间" prop="plannedStartTime">
                    <el-date-picker
                      v-model="formData.plannedStartTime"
                      type="datetime"
                      placeholder="选择开始时间"
                      :default-time="['08:00:00']"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="计划结束时间" prop="plannedEndTime">
                    <el-date-picker
                      v-model="formData.plannedEndTime"
                      type="datetime"
                      placeholder="选择结束时间"
                      :default-time="['18:00:00']"
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="飞行用途" prop="purpose">
                <el-select v-model="formData.purpose" placeholder="请选择飞行用途">
                  <el-option label="航拍" value="航拍" />
                  <el-option label="测绘" value="测绘" />
                  <el-option label="农业植保" value="农业植保" />
                  <el-option label="巡检" value="巡检" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="submitting" @click="submitForm">提交备案</el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </GlassCard>
        </el-tab-pane>

        <el-tab-pane label="备案列表" name="list">
          <div v-loading="loading" class="record-list">
            <GlassCard v-for="record in recordList" :key="record.id" class="record-card">
              <div class="record-header">
                <span class="region-name">{{ record.regionName }}</span>
                <StatusTag :text="getAuditStatusText(record.auditStatus)" :type="getAuditStatusType(record.auditStatus)" />
              </div>
              <div class="record-body">
                <div class="record-item">
                  <span class="label">地址：</span>
                  <span class="value">{{ record.regionAddress }}</span>
                </div>
                <div class="record-item">
                  <span class="label">坐标：</span>
                  <span class="value">{{ record.longitude }}, {{ record.latitude }}</span>
                </div>
                <div class="record-item">
                  <span class="label">飞行半径：</span>
                  <span class="value">{{ record.radius }}米</span>
                </div>
                <div class="record-item">
                  <span class="label">最大高度：</span>
                  <span class="value">{{ record.maxAltitude }}米</span>
                </div>
                <div class="record-item">
                  <span class="label">计划时间：</span>
                  <span class="value">{{ formatDateTime(record.plannedStartTime) }} 至 {{ formatDateTime(record.plannedEndTime) }}</span>
                </div>
                <div class="record-item">
                  <span class="label">用途：</span>
                  <span class="value">{{ record.purpose }}</span>
                </div>
                <div class="record-item">
                  <span class="label">提交时间：</span>
                  <span class="value">{{ formatDateTime(record.createdTime) }}</span>
                </div>
              </div>
            </GlassCard>

            <EmptyState
              v-if="recordList.length === 0 && !loading"
              title="暂无备案记录"
              description="点击上方标签页新增备案"
              :icon="MapLocation"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MapLocation } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { submitAirspaceRecord, getAirspaceList } from '@/api/airspace'

const activeTab = ref('add')
const loading = ref(false)
const submitting = ref(false)
const recordList = ref([])

const formRef = ref(null)
const formData = reactive({
  regionName: '',
  regionAddress: '',
  longitude: '',
  latitude: '',
  radius: 500,
  maxAltitude: 120,
  plannedStartTime: '',
  plannedEndTime: '',
  purpose: ''
})

const formRules = {
  regionName: [{ required: true, message: '请输入飞行区域名称', trigger: 'blur' }],
  regionAddress: [{ required: true, message: '请输入飞行区域地址', trigger: 'blur' }],
  plannedStartTime: [{ required: true, message: '请选择计划开始时间', trigger: 'change' }],
  plannedEndTime: [{ required: true, message: '请选择计划结束时间', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择飞行用途', trigger: 'change' }]
}

const getAuditStatusText = (status) => {
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已驳回'
  }
  return map[status] || '未知'
}

const getAuditStatusType = (status) => {
  const map = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return map[status] || 'default'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').substring(0, 19)
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await submitAirspaceRecord({
        ...formData,
        longitude: formData.longitude ? parseFloat(formData.longitude) : null,
        latitude: formData.latitude ? parseFloat(formData.latitude) : null
      })
      ElMessage.success('备案提交成功，等待审核')
      resetForm()
      activeTab.value = 'list'
      fetchRecords()
    } catch (error) {
      // 错误已处理
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.radius = 500
  formData.maxAltitude = 120
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getAirspaceList()
    recordList.value = res.data || []
  } catch (error) {
    console.error('获取备案列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'list') {
    fetchRecords()
  }
}

onMounted(() => {
})
</script>

<style lang="scss" scoped>
.airspace-record-page {
  min-height: 100%;
}

.tab-container {
  margin-top: 16px;
}

.form-card {
  padding: 32px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.record-card {
  padding: 24px;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.region-name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.record-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-item {
  font-size: 14px;
}

.record-item .label {
  color: #64748b;
}

.record-item .value {
  color: #0f172a;
}

@media (max-width: 768px) {
  .form-card {
    padding: 20px;
  }

  .record-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>