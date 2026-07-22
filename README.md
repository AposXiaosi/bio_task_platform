# bio_task_platform

生物信息分析任务管理平台 —— 高效管理序列分析、表达分析等生信任务的全生命周期

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-4FC08D)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

---

## 📋 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [系统设计](#系统设计)
- [API文档](#api文档)
- [部署指南](#部署指南)
- [贡献指南](#贡献指南)
---

## 📖 项目简介

**bio_task_platform** 是一个专为生物信息学研究人员设计的任务管理平台，旨在解决生信分析中任务分散、参数记录不规范、结果追溯困难等问题。平台提供统一的Web界面，支持创建、监控和管理各类生信分析任务，自动记录任务参数、执行状态、日志和结果文件，助力科研工作高效、可重复。

### 适用场景

- 🔬 基因组序列比对与分析
- 🧬 基因表达定量与差异分析
- 📊 变异检测与注释
- 🧪 批量生信流水线作业

---

## ✨ 核心功能

| 功能模块 | 说明 |
|---------|------|
| **任务创建** | 支持序列分析、表达分析等多种任务类型，自定义参数配置 |
| **任务管理** | 查看任务列表、详情，支持筛选、搜索和分页 |
| **状态监控** | 实时跟踪任务执行状态（待执行/运行中/已完成/失败） |
| **日志查看** | 在线查看任务执行日志，支持实时刷新 |
| **结果管理** | 结果文件上传、下载、预览，支持多格式文件 |
| **参数记录** | 完整记录每次任务的输入参数，保证实验可重复性 |
| **用户认证** | 基于JWT的用户登录与权限控制 |

---

## 🏗️ 技术架构

### 技术栈详情

#### 后端
- **框架**: Spring Boot 3.x
- **ORM**: Spring Data JPA / Hibernate
- **安全**: Spring Security + JWT
- **数据库**: MySQL 8.0
- **连接池**: HikariCP
- **构建工具**: Maven 3.8+

#### 前端
- **框架**: Vue 3 (Composition API)
- **UI库**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite

#### 部署
- **容器化**: Docker + Docker Compose
- **反向代理**: Nginx

---

## 🚀 快速开始

### 前置要求### 前置要求### 前置要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+
- Docker (可选)

### 本地开发运行

#### 1. 克隆项目

```bash
git clone https://github.com/yourusername/bio_task_platform.git
cd bio_task_platform
```

#### 2. 配置数据库

```sql
CREATE DATABASE bio_task_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

修改 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bio_task_db?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

#### 3. 启动后端

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

#### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端服务默认运行在 `http://localhost:5173`

### Docker 一键部署

```bash
docker-compose up -d
```

---

## 📊 系统设计

### 数据库设计

#### 核心表结构

```sql
-- 任务表
CREATE TABLE t_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    task_type VARCHAR(50) NOT NULL COMMENT '任务类型: SEQUENCE/EXPRESSION',
    description TEXT COMMENT '任务描述',
    parameters JSON COMMENT '任务参数 (JSON格式)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
    log_file_path VARCHAR(500) COMMENT '日志文件路径',
    result_file_path VARCHAR(500) COMMENT '结果文件路径',
    created_by VARCHAR(100) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- 用户表
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## 📡 API文档

### 认证接口

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | 用户登录，返回JWT Token |
| POST | `/api/auth/register` | 用户注册 |
| POST | `/api/auth/logout` | 用户登出 |

### 任务接口 (需认证)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | 获取任务列表 (支持分页、筛选) |
| GET | `/api/tasks/{id}` | 获取任务详情 |
| POST | `/api/tasks` | 创建新任务 |
| PUT | `/api/tasks/{id}` | 更新任务 |
| DELETE | `/api/tasks/{id}` | 删除任务 |
| GET | `/api/tasks/{id}/log` | 获取任务日志 |
| GET | `/api/tasks/{id}/result` | 下载结果文件 |
| POST | `/api/tasks/{id}/retry` | 重试失败任务 |

### 请求示例

```json
POST /api/tasks
{
    "taskName": "RNA-seq差异表达分析",
    "taskType": "EXPRESSION",
    "description": "使用DESeq2分析处理组vs对照组",
    "parameters": {
        "referenceGenome": "hg38",
        "sampleGroup": "treatment_vs_control",
        "pValueCutoff": 0.05,
        "log2FoldChange": 1.0
    }
}
```

---

## 📁 项目结构

```
bio_task_platform/
├── backend/                          # 后端服务
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/bio/
│   │   │   │   ├── controller/       # REST控制器
│   │   │   │   ├── service/          # 业务逻辑层
│   │   │   │   ├── repository/       # 数据访问层
│   │   │   │   ├── entity/           # 实体类
│   │   │   │   ├── dto/              # 数据传输对象
│   │   │   │   ├── config/           # 配置类 (Security, JWT等)
│   │   │   │   ├── utils/            # 工具类
│   │   │   │   └── exception/        # 全局异常处理
│   │   │   └── resources/
│   │   │       ├── application.yml   # 主配置文件
│   │   │       └── db/               # 数据库迁移脚本
│   │   └── test/                     # 单元测试
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/                         # 前端应用
│   ├── src/
│   │   ├── api/                      # API接口封装
│   │   ├── assets/                   # 静态资源
│   │   ├── components/               # 公共组件
│   │   ├── views/                    # 页面视图
│   │   │   ├── TaskList.vue          # 任务列表页
│   │   │   ├── TaskCreate.vue        # 创建任务页
│   │   │   ├── TaskDetail.vue        # 任务详情页
│   │   │   └── Login.vue             # 登录页
│   │   ├── stores/                   # Pinia状态管理
│   │   ├── router/                   # 路由配置
│   │   ├── utils/                    # 工具函数
│   │   └── App.vue                   # 根组件
│   ├── package.json
│   ├── vite.config.js
│   └── Dockerfile
│
├── docker-compose.yml                # Docker编排配置
├── nginx.conf                        # Nginx配置
└── README.md                         # 项目文档
```

---

## 🔧 配置说明

### 后端配置 (`application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bio_task_db
    username: root
    password: ${DB_PASSWORD:123456}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400000  # 24小时

file:
  upload-dir: ./uploads
  log-dir: ./logs
```

### 前端配置 (`.env`)

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_URL=ws://localhost:8080/ws
```

---

## 🚢 部署指南

### 生产环境部署

#### 1. 使用 Docker Compose (推荐)

```bash
# 构建并启动所有服务
docker-compose up -d --build

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

#### 2. 手动部署

```bash
# 打包后端
cd backend && mvn clean package -DskipTests
java -jar target/bio-task-platform-*.jar

# 打包前端
cd frontend && npm run build
# 将dist目录部署到Nginx
```

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！请遵循以下流程：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 开发规范

- 后端遵循 [Spring Boot 最佳实践](https://spring.io/guides)
- 前端使用 [Vue 3 风格指南](https://vuejs.org/style-guide/)
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 📧 联系方式

- 项目维护者: 刘恒希 王麓涵 吴佳芩 杨阳
- 项目地址: [https://github.com/yourusername/bio_task_platform](https://github.com/AposXiaosi/bio_task_platform)
- 问题反馈: [Issues](https://github.com/yourusername/bio_task_platform/issues)

---

## 🙏 致谢

感谢以下开源项目提供的技术支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [MySQL](https://www.mysql.com/)

---

**⭐ 如果这个项目对您有帮助，请给我们一个 Star！**
