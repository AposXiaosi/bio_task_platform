# BioInfo Platform

生物信息分析任务管理平台。项目提供任务创建、文件上传、分析执行、实时日志和结果下载，并内置六种可直接运行的教学型生信分析。

## 主要功能

- 序列比对：输出最佳匹配位置、方向和 identity
- 序列组装：基于 reads 重叠生成 contigs 和 N50 等指标
- 基因表达分析：生成 CPM 风格的标准化表达矩阵
- 差异表达分析：计算组间均值、log2FC、p 值和 BH 校正结果
- 变异检测：比较参考与样本序列并输出 SNP VCF
- 功能注释：根据序列类型、GC 含量和内置 motif 规则生成注释
- 每个任务记录分阶段日志，并支持在线筛选、刷新和下载
- 分析结果保存为 TSV、FASTA、VCF、JSON 等格式并支持下载

> 内置分析引擎面向教学和中小型演示数据，不替代 BWA、GATK、DESeq2、BLAST 等生产级工具。

## 技术栈

- 后端：Java 17、Spring Boot 3、Spring Data JPA、MySQL 8
- 前端：Vue 3、Vite、Element Plus、Axios

## 本地运行

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS bioinfo_platform
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

也可以执行 `sql/init.sql`。应用首次启动时会自动补齐表结构、六种分析类型和演示管理员。

### 2. 配置数据库密码

PowerShell：

```powershell
$env:BIOINFO_DB_PASSWORD = "你的 MySQL 密码"
```

可选环境变量：

- `BIOINFO_DB_URL`：默认 `jdbc:mysql://localhost:3306/bioinfo_platform...`
- `BIOINFO_DB_USER`：默认 `root`
- `BIOINFO_DB_PASSWORD`：MySQL 密码
- `BIOINFO_SERVER_PORT`：默认 `8081`
- `BIOINFO_UPLOAD_DIR`：默认 `./data/uploads`

### 3. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8081`

### 4. 启动前端

```powershell
cd frontend
npm install
$env:BIOINFO_BACKEND_URL = "http://localhost:8081"
npm run dev
```

前端地址：`http://localhost:5173`

## 演示账号

- 用户名：`admin`
- 密码：`admin123`

生产部署前应修改默认账号，并通过环境变量或密钥管理服务提供数据库凭据。

## 测试与构建

```powershell
cd backend
mvn test
mvn package -DskipTests

cd ../frontend
npm run build
```

## 目录结构

```text
backend/   Spring Boot API、分析引擎和测试
frontend/  Vue 3 前端
sql/       MySQL 初始化脚本
```
