<template>
  <div>
    <PageHeader title="系统通知" description="查看所有新订单及系统事件通知" :extra="headerExtra">
    </PageHeader>

    <div class="row" style="margin-bottom: 16px; display: flex; gap: 16px;">
      <StatTile label="未读通知" :value="totalUnread" accent="linear-gradient(135deg, #ff9800, #f44336)" />
      <StatTile label="总通知数" :value="total" accent="linear-gradient(135deg, #4facfe, #00f2fe)" />
    </div>

    <GlassCard>
      <div class="notification-tabs" style="margin-bottom: 16px;">
        <el-radio-group v-model="selectedType" @change="handleTypeChange" size="default">
          <el-radio-button :value="0">全部</el-radio-button>
          <el-radio-button :value="1">订单通知</el-radio-button>
          <el-radio-button :value="2">评论审核</el-radio-button>
          <el-radio-button :value="3">故障报修</el-radio-button>
          <el-radio-button :value="4">空域备案</el-radio-button>
          <el-radio-button :value="5">系统通知</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="标题" prop="title" min-width="220">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-tag v-if="row.readStatus === 0" size="small" type="danger" effect="dark">未读</el-tag>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="内容" prop="content" min-width="380" show-overflow-tooltip />
        <el-table-column label="类型" prop="type" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="light">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联业务ID" prop="businessId" width="120" align="center" />
        <el-table-column label="时间" prop="createdTime" width="200" align="center">
          <template #default="{ row }">{{ formatTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="row.readStatus === 0" type="primary" link size="small" @click="handleMarkRead(row.id)">标记已读</el-button>
            <el-button v-else link size="small" disabled>已读</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        @size-change="fetchList"
        @current-change="fetchList"
        background
        style="margin-top: 16px; justify-content: flex-end; display: flex;"
      />

      <EmptyState v-if="!loading && rows.length === 0" description="暂无系统通知" />
    </GlassCard>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Check } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatTile from '@/components/common/StatTile.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { getAdminNotifications, getAdminUnreadCount, adminMarkAsRead, adminMarkAllAsRead } from '@/api/notification'

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalUnread = ref(0)
const rows = ref([])
const loading = ref(false)
const selectedType = ref(0)

const headerExtra = computed(() => [
  <el-button type="primary" onClick={refresh}><el-icon><Refresh /></el-icon><span style="margin-left:4px">刷新</span></el-button>,
  <el-button onClick={() => handleMarkAll()}><el-icon><Check /></el-icon><span style="margin-left:4px">全部已读</span></el-button>
])

const typeText = (t) => {
  const map = { 1: '订单通知', 2: '评论审核', 3: '故障报修', 4: '空域备案', 5: '系统通知' }
  return map[t] || '通知'
}

const typeTagType = (t) => {
  const map = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'primary', 5: 'info' }
  return map[t] || 'info'
}

const formatTime = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const p = (n) => (n < 10 ? '0' + n : '' + n)
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const handleTypeChange = () => {
  pageNum.value = 1
  fetchList()
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminNotifications({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      type: selectedType.value === 0 ? null : selectedType.value,
      adminOnly: true
    })
    if (res.code === 200 && res.data) {
      rows.value = res.data.records || res.data || []
      total.value = res.data.total || rows.value.length
    } else {
      rows.value = []
      total.value = 0
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const fetchUnread = async () => {
  try {
    const res = await getAdminUnreadCount()
    if (res.code === 200 && res.data) {
      totalUnread.value = Number(res.data.count) || 0
    }
  } catch (err) {
    console.error(err)
  }
}

const handleMarkRead = async (id) => {
  try {
    const res = await adminMarkAsRead(id)
    if (res.code === 200) {
      ElMessage.success('已标记为已读')
      window.dispatchEvent(new Event('notification-unread-updated'))
      fetchList()
      fetchUnread()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (err) {
    console.error(err)
  }
}

const handleMarkAll = async () => {
  try {
    const res = await adminMarkAllAsRead()
    if (res.code === 200) {
      ElMessage.success('已全部标记为已读')
      window.dispatchEvent(new Event('notification-unread-updated'))
      fetchList()
      fetchUnread()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (err) {
    console.error(err)
  }
}

const refresh = () => {
  fetchList()
  fetchUnread()
}

onMounted(() => {
  fetchList()
  fetchUnread()
})
</script>
