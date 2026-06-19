<template>
  <div class="airspace-audit-page">
    <PageHeader title="空域备案审核" subtitle="审核用户空域备案申请" />

    <GlassCard class="table-card">
      <div class="table-header">
        <el-radio-group v-model="auditStatus" @change="handleStatusChange">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="0">待审核</el-radio-button>
          <el-radio-button value="1">已通过</el-radio-button>
          <el-radio-button value="2">已拒绝</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="airspaceList" v-loading="loading" style="width: 100%">
        <el-table-column label="用户" width="120">
          <template #default="{ row }">
            {{ row.username || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="区域名称" prop="regionName" min-width="150" />
        <el-table-column label="区域范围" min-width="200">
          <template #default="{ row }">
            {{ row.regionRange || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="飞行高度" width="120">
          <template #default="{ row }">
            {{ row.maxAltitude || '-' }}米
          </template>
        </el-table-column>
        <el-table-column label="备案时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :text="getAuditStatusText(row.auditStatus)" :type="getAuditStatusType(row.auditStatus)" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 4px; flex-wrap: nowrap;">
              <template v-if="row.auditStatus === 0">
                <el-button text type="success" @click="handleApprove(row)">通过</el-button>
                <el-button text type="danger" @click="handleReject(row)">拒绝</el-button>
              </template>
              <el-button text type="primary" @click="handleViewDetail(row)">详情</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="fetchAirspaceList"
          @current-change="fetchAirspaceList"
        />
      </div>
    </GlassCard>

    <el-dialog v-model="detailVisible" title="空域备案详情" width="700px">
      <div v-if="currentAirspace" class="airspace-detail">
        <div class="detail-section">
          <h4>基本信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="用户">{{ currentAirspace.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="区域名称">{{ currentAirspace.regionName }}</el-descriptions-item>
            <el-descriptions-item label="区域范围">{{ currentAirspace.regionRange || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最大飞行高度">{{ currentAirspace.maxAltitude || '-' }}米</el-descriptions-item>
            <el-descriptions-item label="备案时间" :span="2">
              {{ formatDateTime(currentAirspace.startTime) }} ~ {{ formatDateTime(currentAirspace.endTime) }}
            </el-descriptions-item>
            <el-descriptions-item label="提交时间" :span="2">{{ formatDateTime(currentAirspace.createdTime) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ currentAirspace.remark || '无' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="currentAirspace.auditStatus !== 0" class="detail-section">
          <h4>审核结果</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="审核状态">
              <StatusTag :text="getAuditStatusText(currentAirspace.auditStatus)" :type="getAuditStatusType(currentAirspace.auditStatus)" />
            </el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ formatDateTime(currentAirspace.auditTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="currentAirspace.auditStatus === 2" label="拒绝原因" :span="2">
              {{ currentAirspace.auditRemark }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <template v-if="currentAirspace?.auditStatus === 0">
          <el-button type="success" @click="handleApprove(currentAirspace)">通过</el-button>
          <el-button type="danger" @click="handleReject(currentAirspace)">拒绝</el-button>
        </template>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { getAdminAirspaceList, approveAirspace, rejectAirspace } from '@/api/airspace'

const loading = ref(false)
const airspaceList = ref([])
const total = ref(0)
const auditStatus = ref('')
const detailVisible = ref(false)
const currentAirspace = ref(null)

const pagination = reactive({
  page: 1,
  pageSize: 10
})

const getAuditStatusText = (status) => {
  const map = { 0: '待审核', 1: '已通过', 2: '已拒绝' }
  return map[status] || '未知'
}

const getAuditStatusType = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'error' }
  return map[status] || 'default'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return dateTime.replace('T', ' ').substring(0, 16)
}

const fetchAirspaceList = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    }
    if (auditStatus.value) {
      params.auditStatus = parseInt(auditStatus.value)
    }

    const res = await getAdminAirspaceList(params)
    airspaceList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('获取空域备案列表失败:', error)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  pagination.page = 1
  fetchAirspaceList()
}

const handleViewDetail = (row) => {
  currentAirspace.value = row
  detailVisible.value = true
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm('确定通过该空域备案申请？', '审核通过', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    await approveAirspace(row.id)
    ElMessage.success('审核通过')
    detailVisible.value = false
    fetchAirspaceList()
  } catch {}
}

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '审核拒绝', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '请输入拒绝原因'
    })
    await rejectAirspace(row.id, value)
    ElMessage.success('已拒绝')
    detailVisible.value = false
    fetchAirspaceList()
  } catch {}
}

onMounted(() => {
  fetchAirspaceList()
})
</script>

<style lang="scss" scoped>
.airspace-audit-page {
  min-height: 100%;
}

.table-card {
  :deep(.el-table) {
    --el-table-bg-color: transparent;
    --el-table-tr-bg-color: transparent;
    --el-table-header-bg-color: #f8fafc;
  }
}

.table-header {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.airspace-detail {
  .detail-section {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }

    h4 {
      font-size: 16px;
      font-weight: 600;
      color: #0f172a;
      margin: 0 0 16px;
    }
  }
}
</style>