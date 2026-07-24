<template>
  <div class="login-container">
    <div class="login-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)" />
    </div>

    <section class="login-brand" aria-label="BioInfo Platform">
      <div class="brand-lockup"><div class="brand-icon"><el-icon><DataAnalysis /></el-icon></div><div><strong>BioInfo</strong><span>ANALYSIS PLATFORM</span></div></div>
      <div class="sequence-visual">
        <div class="sequence-head"><span>REFERENCE · chr1:1048-1095</span><b>48 BP</b></div>
        <div v-for="(row, index) in sequenceRows" :key="index" class="sequence-row"><small>{{ row.label }}</small><code><i v-for="(base, baseIndex) in row.bases" :key="baseIndex" :class="base.toLowerCase()">{{ base }}</i></code></div>
        <div class="sequence-foot"><span><i /> Alignment complete</span><strong>99.2% identity</strong></div>
      </div>
      <div class="brand-copy"><h2>让每一次分析都可追踪</h2><p>任务、运行日志和结果文件集中在同一个工作区。</p></div>
    </section>

    <el-card class="login-card" :body-style="{ padding: '0' }">
      <div class="login-card-inner">
        <div class="login-header">
          <div class="login-logo">
            <el-icon :size="40" color="#409eff"><DataAnalysis /></el-icon>
          </div>
          <h1 class="login-title">生物信息分析任务管理平台</h1>
          <p class="login-subtitle">Bioinformatics Analysis Task Management</p>
        </div>

        <el-tabs v-model="activeTab" class="login-tabs" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="login-form"
              @submit.prevent="handleLogin"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入用户名"
                  size="large"
                  :prefix-icon="User"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <div class="login-options">
                <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              </div>
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="login-btn"
                  :loading="loginLoading"
                  @click="handleLogin"
                >
                  {{ loginLoading ? '登录中...' : '登 录' }}
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              class="login-form"
              @submit.prevent="handleRegister"
            >
              <el-form-item prop="username">
                <el-input
                  v-model="registerForm.username"
                  placeholder="请输入用户名"
                  size="large"
                  :prefix-icon="User"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
              <el-form-item prop="confirmPassword">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请确认密码"
                  size="large"
                  :prefix-icon="Lock"
                  show-password
                />
              </el-form-item>
              <el-form-item prop="nickname">
                <el-input
                  v-model="registerForm.nickname"
                  placeholder="请输入昵称"
                  size="large"
                  :prefix-icon="UserFilled"
                />
              </el-form-item>
              <el-form-item prop="email">
                <el-input
                  v-model="registerForm.email"
                  placeholder="请输入邮箱"
                  size="large"
                  :prefix-icon="Message"
                />
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="login-btn"
                  :loading="registerLoading"
                  @click="handleRegister"
                >
                  {{ registerLoading ? '注册中...' : '注 册' }}
                </el-button>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>

    <div class="login-footer">
      <span>© 2026 生物信息分析任务管理平台 · BioInfo Platform</span>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Lock, UserFilled, Message, DataAnalysis
} from '@element-plus/icons-vue'
import { login, register } from '../api/user'
import { setToken, setUser } from '../utils/auth'

const router = useRouter()

const activeTab = ref('login')
const rememberMe = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)

const loginFormRef = ref(null)
const registerFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: ''
})

const sequenceRows = [
  { label: 'REF', bases: 'ATGCTAGCTACGATCGTACGATCG' },
  { label: 'R01', bases: 'ATGCTAGCTACGATCGTACGATCG' },
  { label: 'R02', bases: 'ATGCTAGCTACGATCGTTCGATCG' },
  { label: 'R03', bases: 'ATGCTAGCTACGATCGTACGATCG' }
]

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateEmail = (rule, value, callback) => {
  if (value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    callback(new Error('请输入正确的邮箱格式'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { validator: validateEmail, trigger: 'blur' }
  ]
}

async function handleLogin() {
  const form = loginFormRef.value
  if (!form) return

  await form.validate(async (valid) => {
    if (!valid) return
    loginLoading.value = true
    try {
      const res = await login({
        username: loginForm.username,
        password: loginForm.password
      })
      // 拦截器已解包，res就是userDTO对象
      if (res && res.token) {
        setToken(res.token)
        setUser(res)
      }
      ElMessage.success('登录成功')
      router.push('/')
    } catch (err) {
      ElMessage.error(err.message || '登录失败，请检查用户名和密码')
    } finally {
      loginLoading.value = false
    }
  })
}

async function handleRegister() {
  const form = registerFormRef.value
  if (!form) return

  await form.validate(async (valid) => {
    if (!valid) return
    registerLoading.value = true
    try {
      await register({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname,
        email: registerForm.email
      })
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.username = registerForm.username
      loginForm.password = ''
    } catch (err) {
      ElMessage.error(err.message || '注册失败，请稍后重试')
    } finally {
      registerLoading.value = false
    }
  })
}

function particleStyle(i) {
  const size = Math.random() * 6 + 2
  const left = Math.random() * 100
  const delay = Math.random() * 20
  const duration = Math.random() * 15 + 10
  return {
    width: size + 'px',
    height: size + 'px',
    left: left + '%',
    animationDelay: delay + 's',
    animationDuration: duration + 's'
  }
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 80px;
  padding: 48px;
  background: #edf1f2;
  position: relative;
  overflow: hidden;
}

.login-particles {
  display: none;
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  bottom: -10px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  animation: floatUp linear infinite;
}

@keyframes floatUp {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translateY(-100vh) scale(0.5);
    opacity: 0;
  }
}

.login-card {
  width: 440px;
  max-width: 92vw;
  border-radius: 8px;
  border: 1px solid #dbe2e5;
  box-shadow: 0 18px 50px rgba(31, 41, 46, 0.12);
  z-index: 10;
  overflow: hidden;
}

.login-card :deep(.el-card__body) {
  padding: 0;
}

.login-card-inner {
  padding: 40px 36px 32px;
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.login-logo {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  background: #087f73;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: none;
}

.login-logo .el-icon {
  color: #fff !important;
}

.login-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.login-subtitle {
  font-size: 12px;
  color: #909399;
  margin: 0;
  letter-spacing: 0.5px;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e8eaec;
}

.login-tabs :deep(.el-tabs__active-bar) {
  background: #087f73;
  height: 3px;
  border-radius: 2px;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: #909399;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: #14919b;
  font-weight: 600;
}

.login-form {
  padding: 0 4px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: all 0.3s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #14919b inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(20, 145, 155, 0.3) inset;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  background: #087f73;
  border: none;
  transition: all 0.3s;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(20, 145, 155, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.login-footer {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  z-index: 10;
}

.login-footer span {
  color: #8b979d;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.login-brand { width: min(470px, 42vw); align-self: stretch; padding: 42px; display: flex; flex-direction: column; justify-content: center; border-radius: 8px; background: #202b30; color: #fff; }
.brand-lockup { display: flex; align-items: center; gap: 12px; }.brand-icon { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 7px; background: #168b80; font-size: 22px; }.brand-lockup > div:last-child { display: flex; flex-direction: column; gap: 4px; }.brand-lockup strong { font-size: 18px; }.brand-lockup span { color: #84979f; font-size: 9px; letter-spacing: 1.5px; }
.sequence-visual { margin: 50px 0 34px; overflow: hidden; border: 1px solid #3a484e; border-radius: 7px; background: #182126; }.sequence-head,.sequence-foot { min-height: 44px; padding: 0 14px; display: flex; align-items: center; justify-content: space-between; color: #8ea0a7; font-size: 9px; letter-spacing: .6px; }.sequence-head { border-bottom: 1px solid #303e44; }.sequence-head b { color: #65c8bc; }.sequence-row { min-height: 34px; padding: 0 13px; display: grid; grid-template-columns: 34px 1fr; align-items: center; border-bottom: 1px solid rgba(255,255,255,.035); }.sequence-row small { color: #6d8088; font: 9px Consolas, monospace; }.sequence-row code { display: flex; gap: 2px; }.sequence-row i { width: 14px; color: #a9bac0; font-size: 9px; font-style: normal; text-align: center; }.sequence-row i.a { color: #65c8bc; }.sequence-row i.t { color: #e0ad65; }.sequence-row i.g { color: #d77b7b; }.sequence-row i.c { color: #85a7d0; }.sequence-foot span { display: flex; align-items: center; gap: 7px; }.sequence-foot i { width: 6px; height: 6px; border-radius: 50%; background: #54bd78; }.sequence-foot strong { color: #c2ced2; font-size: 9px; }
.brand-copy h2 { margin: 0 0 8px; font-size: 22px; letter-spacing: 0; }.brand-copy p { margin: 0; color: #96a7ad; font-size: 12px; line-height: 1.7; }

@media (max-width: 480px) {
  .login-card-inner {
    padding: 32px 24px 24px;
  }

  .login-title {
    font-size: 17px;
  }
}
@media (max-width: 900px) { .login-container { padding: 24px; }.login-brand { display: none; } }
</style>
