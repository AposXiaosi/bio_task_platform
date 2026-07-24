<template>
  <router-view v-if="isLoginPage" />

  <el-container v-else class="bio-layout">
    <el-aside width="236px" class="bio-sidebar desktop-sidebar">
      <BrandBlock />
      <SideMenu :active="activeMenu" />
      <div class="sidebar-foot">
        <span class="health-dot" />
        <span>分析服务就绪</span>
      </div>
    </el-aside>

    <el-drawer v-model="mobileMenu" direction="ltr" size="268px" :with-header="false" class="mobile-drawer">
      <div class="bio-sidebar drawer-sidebar">
        <BrandBlock />
        <SideMenu :active="activeMenu" @navigate="mobileMenu = false" />
      </div>
    </el-drawer>

    <el-container class="bio-workspace">
      <el-header class="bio-header" height="64px">
        <el-button class="mobile-menu-btn" :icon="Menu" circle aria-label="打开导航" @click="mobileMenu = true" />
        <div>
          <div class="bio-header-title">{{ currentPageTitle }}</div>
          <div class="bio-header-context">BioInfo Workspace</div>
        </div>
        <div class="bio-header-right">
          <el-tooltip content="刷新当前页面" placement="bottom">
            <el-button :icon="Refresh" circle @click="handleRefresh" />
          </el-tooltip>
          <span class="header-divider" />
          <el-dropdown v-if="currentUser" trigger="click">
            <button class="bio-user-info">
              <span class="user-avatar">{{ userInitial }}</span>
              <span class="user-name">{{ currentUser.nickname || currentUser.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="bio-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in"><component :is="Component" :key="$route.fullPath" /></transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, DataAnalysis, List, Menu, Odometer, Plus, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { ElIcon, ElMenu, ElMenuItem } from 'element-plus'
import { getUser, logout } from './utils/auth'

const route = useRoute()
const router = useRouter()
const currentUser = ref(null)
const mobileMenu = ref(false)
const isLoginPage = computed(() => route.path === '/login')
const activeMenu = computed(() => route.path === '/' ? '/' : route.path === '/tasks/create' ? '/tasks/create' : route.path.startsWith('/tasks') ? '/tasks' : route.path)
const currentPageTitle = computed(() => route.meta?.title || '生物信息分析任务平台')
const userInitial = computed(() => (currentUser.value?.nickname || currentUser.value?.username || 'U').slice(0, 1).toUpperCase())

const BrandBlock = defineComponent({
  setup: () => () => h('div', { class: 'bio-logo' }, [
    h('div', { class: 'bio-logo-icon' }, h(ElIcon, null, { default: () => h(DataAnalysis) })),
    h('div', null, [h('div', { class: 'bio-logo-text' }, 'BioInfo'), h('div', { class: 'bio-logo-subtitle' }, 'ANALYSIS PLATFORM')])
  ])
})

const SideMenu = defineComponent({
  props: { active: String }, emits: ['navigate'],
  setup(props, { emit }) {
    const item = (path, icon, label, caption) => h(ElMenuItem, { index: path, onClick: () => emit('navigate') }, { default: () => [h(ElIcon, null, { default: () => h(icon) }), h('div', { class: 'nav-copy' }, [h('span', label), h('small', caption)])] })
    return () => h(ElMenu, { defaultActive: props.active, router: true, class: 'bio-nav' }, { default: () => [
      h('div', { class: 'nav-section-label' }, '工作区'),
      item('/', Odometer, '分析概览', 'Overview'),
      item('/tasks', List, '任务管理', 'Tasks & results'),
      item('/tasks/create', Plus, '新建分析', 'Create workflow')
    ] })
  }
})

onMounted(() => { currentUser.value = getUser() })
function handleRefresh() { router.go(0) }
function handleLogout() { logout(); router.push('/login') }
</script>
