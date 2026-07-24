<template>
  <el-container v-if="isLoginPage" class="bio-layout-login">
    <router-view />
  </el-container>

  <el-container v-else class="bio-layout">
    <el-aside :width="sidebarWidth" class="bio-sidebar">
      <div class="bio-logo">
        <div class="bio-logo-icon">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <span class="bio-logo-text">BioInfo 平台</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="false"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.7)"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/tasks">
          <el-icon><List /></el-icon>
          <template #title>任务管理</template>
        </el-menu-item>
        <el-menu-item index="/tasks/create">
          <el-icon><Plus /></el-icon>
          <template #title>新建任务</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="bio-header" height="56px">
        <span class="bio-header-title">{{ currentPageTitle }}</span>
        <div class="bio-header-right" style="margin-left: auto; display: flex; align-items: center; gap: 12px">
          <el-tooltip content="刷新" placement="bottom">
            <el-button :icon="Refresh" circle size="small" @click="handleRefresh" />
          </el-tooltip>
          <el-dropdown v-if="currentUser">
            <span class="bio-user-info" style="cursor: pointer; display: flex; align-items: center; gap: 6px; color: #303133; font-size: 14px">
              <el-icon><User /></el-icon>
              {{ currentUser.nickname || currentUser.username }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="bio-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh, User, SwitchButton, DataAnalysis } from '@element-plus/icons-vue'
import { getUser, logout } from './utils/auth'

const route = useRoute()
const router = useRouter()

const sidebarWidth = '220px'
const currentUser = ref(null)

const isLoginPage = computed(() => route.path === '/login')

const activeMenu = computed(() => {
  const path = route.path
  if (path === '/') return '/'
  if (path === '/tasks/create') return '/tasks/create'
  if (path.startsWith('/tasks')) return '/tasks'
  return path
})

const currentPageTitle = computed(() => {
  return route.meta?.title || '生物信息分析任务管理平台'
})

function refreshCurrentUser() {
  currentUser.value = getUser()
}

onMounted(refreshCurrentUser)

// 每次路由导航后刷新用户信息（登录/登出后 App.vue 不会重新挂载）
watch(() => route.fullPath, () => {
  refreshCurrentUser()
})

function handleRefresh() {
  router.go(0)
}

function handleLogout() {
  logout()
  router.push('/login')
}
</script>

<style scoped>
.bio-layout {
  height: 100vh;
  overflow: hidden;
}

.bio-layout-login {
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bio-header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
