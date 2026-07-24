<template>
  <div class="login-container">
    <div class="login-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)" />
    </div>

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
    } catch {
      // 拦截器已统一处理错误提示
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
    } catch {
      // 拦截器已统一处理错误提示
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
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0c1e3a 0%, #0d3b66 30%, #14919b 70%, #0f766e 100%);
  position: relative;
  overflow: hidden;
}

.login-particles {
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
  border-radius: 16px;
  border: none;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3), 0 0 40px rgba(20, 145, 155, 0.15);
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
  background: linear-gradient(135deg, #409eff, #14919b);
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.3);
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
  background: linear-gradient(90deg, #409eff, #14919b);
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
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #409eff, #14919b);
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
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  letter-spacing: 0.5px;
}

@media (max-width: 480px) {
  .login-card-inner {
    padding: 32px 24px 24px;
  }

  .login-title {
    font-size: 17px;
  }
}
</style>
