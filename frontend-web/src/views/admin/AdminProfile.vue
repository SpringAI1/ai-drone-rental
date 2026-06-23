<template>
  <div class="admin-profile-page">
    <PageHeader title="个人中心" subtitle="管理您的管理员账号信息" />

    <div class="profile-content">
      <div class="profile-sidebar">
        <GlassCard class="user-card">
          <div class="user-avatar">
            <el-upload
              class="avatar-upload"
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              :before-upload="beforeAvatarUpload"
              accept="image/*"
            >
              <el-avatar :size="80" :src="avatarUrl" class="avatar-clickable">
                {{ authStore.user?.username?.charAt(0) }}
              </el-avatar>
              <el-button text type="primary" :loading="avatarUploading">更换头像</el-button>
            </el-upload>
          </div>
          <h3 class="user-name">{{ authStore.user?.nickname || authStore.user?.username }}</h3>
          <p class="user-role">系统管理员</p>
          <StatusTag text="已激活" type="success" size="large" />
        </GlassCard>

        <GlassCard class="menu-card">
          <div class="menu-list">
            <div
              v-for="item in menuItems"
              :key="item.key"
              class="menu-item"
              :class="{ 'menu-item--active': activeMenu === item.key }"
              @click="activeMenu = item.key"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </GlassCard>
      </div>

      <div class="profile-main">
        <!-- 基本信息 -->
        <GlassCard v-if="activeMenu === 'info'" title="基本信息">
          <el-form
            ref="infoFormRef"
            :model="infoForm"
            :rules="infoRules"
            label-width="100px"
            label-position="left"
          >
            <el-form-item label="用户名">
              <el-input :model-value="authStore.user?.username" disabled />
              <div class="form-tip">用户名不可修改</div>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="infoForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSaveInfo">保存修改</el-button>
            </el-form-item>
          </el-form>
        </GlassCard>

        <!-- 修改密码 -->
        <GlassCard v-if="activeMenu === 'password'" title="修改密码">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            label-position="left"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入原密码"
                show-password
              />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                show-password
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">
                确认修改
              </el-button>
              <el-button @click="passwordFormRef?.resetFields()">重置</el-button>
            </el-form-item>
          </el-form>
        </GlassCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, markRaw } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import GlassCard from '@/components/common/GlassCard.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import { getUserInfo, updateUserInfo, updatePassword } from '@/api/user'
import { upload } from '@/api/request'

const authStore = useAuthStore()

const activeMenu = ref('info')
const saving = ref(false)
const changingPassword = ref(false)
const avatarUploading = ref(false)

const avatarUrl = computed(() => {
  const avatar = authStore.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return `/api${avatar}`
})

const menuItems = [
  { key: 'info', label: '基本信息', icon: markRaw(User) },
  { key: 'password', label: '修改密码', icon: markRaw(Lock) }
]

// 基本信息表单
const infoFormRef = ref(null)
const infoForm = reactive({
  nickname: '',
  phone: '',
  email: ''
})

const infoRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 修改密码表单
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！')
    return false
  }
  return true
}

const handleAvatarUpload = async (options) => {
  const { file } = options
  avatarUploading.value = true
  try {
    const uploadRes = await upload('/common/upload', file)
    const avatarPath = uploadRes.data

    await updateUserInfo({ avatar: avatarPath })
    authStore.updateUserInfo({ avatar: avatarPath })
    ElMessage.success('头像更新成功')
  } catch (error) {
    ElMessage.error('头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const handleSaveInfo = async () => {
  if (!infoFormRef.value) return

  await infoFormRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await updateUserInfo(infoForm)
      authStore.updateUserInfo(infoForm)
      ElMessage.success('保存成功')
    } catch (error) {
      // 错误已处理
    } finally {
      saving.value = false
    }
  })
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return

    changingPassword.value = true
    try {
      await updatePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      ElMessage.success('密码修改成功')
      passwordFormRef.value.resetFields()
    } catch (error) {
      // 错误已处理
    } finally {
      changingPassword.value = false
    }
  })
}

onMounted(async () => {
  try {
    // 拉一次最新数据，确保与后端一致
    const res = await getUserInfo()
    if (res.data) {
      authStore.updateUserInfo(res.data)
    }
  } catch (e) {
    // 静默失败：authStore 里已有用户信息
  }

  if (authStore.user) {
    infoForm.nickname = authStore.user.nickname || ''
    infoForm.phone = authStore.user.phone || ''
    infoForm.email = authStore.user.email || ''
  }
})
</script>

<style lang="scss" scoped>
.admin-profile-page {
  min-height: 100%;
}

.profile-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.profile-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: sticky;
  top: 96px;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

// 用户卡片
.user-card {
  text-align: center;
  padding: 32px;
}

.user-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;

  :deep(.el-upload) {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }
}

.avatar-clickable {
  cursor: pointer;
  transition: all 0.3s;
  border: 3px solid transparent;

  &:hover {
    border-color: #3b82f6;
    transform: scale(1.05);
  }
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px;
}

.user-role {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 16px;
}

// 菜单卡片
.menu-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 15px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    color: #0f172a;
    background: #e2e8f0;
  }

  &--active {
    color: #3b82f6;
    background: rgba(59, 130, 246, 0.15);
  }
}

// 表单样式
:deep(.el-form) {
  max-width: 500px;
}

.form-tip {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
  margin-top: 4px;
}

// 响应式
@media (max-width: 1024px) {
  .profile-content {
    flex-direction: column;
  }

  .profile-sidebar {
    width: 100%;
    position: static;
  }

  .menu-list {
    flex-direction: row;
    overflow-x: auto;
  }

  .menu-item {
    white-space: nowrap;
  }
}
</style>
