# 序列比对测试用例

本文档用于验证当前平台新增的第一种生信分析功能: `序列比对`.

## 测试前准备

1. 确保数据库已导入 `sql/init.sql`, 并且 `analysis_type` 表中存在 `sequence_alignment`.
2. 启动后端服务与前端页面.
3. 准备以下测试文件:

- `F:\实训\项目\7.24\backend\test-data\sequence-alignment\reference.fa`
- `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_exact.fa`
- `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_mismatch.fa`
- `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_gap.fa`

4. 确认 `application.yml` 中的上传目录存在写权限:

```yaml
bioinfo:
  upload:
    dir: D:/bioinfo-uploads
```

## 一、基础功能测试

### 用例 1: 完全匹配序列比对

目的: 验证平台能够完成一次成功的序列比对, 并生成结果文件与日志.

任务表单建议填写:

- 任务名称: `序列比对-完全匹配`
- 分析类型: `序列比对`
- 优先级: `MEDIUM`
- 输入文件路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_exact.fa`
- 参考基因组路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\reference.fa`
- 匹配得分: `2`
- 错配罚分: `1`
- 空位罚分: `2`

预期结果:

1. 任务创建成功.
2. 任务状态最终为 `COMPLETED`.
3. 日志中至少出现以下信息:
   - `开始执行序列比对任务`
   - `已加载参考序列`
   - `正在比对输入序列`
   - `序列比对完成`
4. 结果文件页签中出现 1 个结果文件.
5. 下载结果文件后, 文件内容中应包含:
   - `匹配数`
   - `错配数`
   - `Gap 数`
   - `序列一致性`
6. 完全匹配场景下, `序列一致性` 应接近 `100.00%`.

### 用例 2: 存在错配的序列比对

目的: 验证平台能正确处理错配序列并生成结果.

任务表单建议填写:

- 任务名称: `序列比对-错配`
- 分析类型: `序列比对`
- 输入文件路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_mismatch.fa`
- 参考基因组路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\reference.fa`
- 匹配得分: `2`
- 错配罚分: `1`
- 空位罚分: `2`

预期结果:

1. 任务状态最终为 `COMPLETED`.
2. 结果文件成功生成.
3. 结果文件中的 `错配数` 大于 `0`.
4. `序列一致性` 小于 `100%`.

### 用例 3: 存在 gap 的序列比对

目的: 验证平台能处理插入/缺失型序列差异.

任务表单建议填写:

- 任务名称: `序列比对-gap`
- 分析类型: `序列比对`
- 输入文件路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_gap.fa`
- 参考基因组路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\reference.fa`
- 匹配得分: `2`
- 错配罚分: `1`
- 空位罚分: `2`

预期结果:

1. 任务状态最终为 `COMPLETED`.
2. 结果文件成功生成.
3. 结果文件中的 `Gap 数` 大于 `0`.
4. 比对结果中应能看到 `-` 占位符.

## 二、异常场景测试

### 用例 4: 缺少参考基因组路径

目的: 验证必需参数缺失时系统能给出失败反馈.

任务表单建议填写:

- 任务名称: `序列比对-缺少参考`
- 分析类型: `序列比对`
- 输入文件路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_exact.fa`
- 参考基因组路径: 留空

预期结果:

1. 任务会被创建.
2. 任务状态最终为 `FAILED`.
3. 错误信息包含:
   `序列比对缺少参考基因组路径 referenceGenome`
4. 日志中存在执行失败记录.

### 用例 5: 输入文件路径不存在

目的: 验证系统能识别无效路径.

任务表单建议填写:

- 任务名称: `序列比对-文件不存在`
- 分析类型: `序列比对`
- 输入文件路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\not_exists.fa`
- 参考基因组路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\reference.fa`

预期结果:

1. 任务状态最终为 `FAILED`.
2. 错误信息包含:
   `找不到输入文件`
3. 不应生成结果文件.

## 三、批量输入测试

### 用例 6: 多个输入文件一次提交

目的: 验证系统可对多个输入序列逐个生成结果文件.

任务表单建议填写:

- 任务名称: `序列比对-批量输入`
- 分析类型: `序列比对`
- 输入文件路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_exact.fa,F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_mismatch.fa,F:\实训\项目\7.24\backend\test-data\sequence-alignment\query_gap.fa`
- 参考基因组路径:
  `F:\实训\项目\7.24\backend\test-data\sequence-alignment\reference.fa`

预期结果:

1. 任务状态最终为 `COMPLETED`.
2. 结果文件页签中应出现 3 个结果文件.
3. 日志中应至少出现 3 次 `正在比对输入序列`.

## 四、结果检查位置

成功执行后, 结果文件默认保存在:

```text
D:\bioinfo-uploads\analysis-results\task-{任务ID}
```

例如任务 ID 为 12 时:

```text
D:\bioinfo-uploads\analysis-results\task-12
```

## 五、建议的联调顺序

建议先按以下顺序测试:

1. 用例 1: 完全匹配
2. 用例 2: 错配
3. 用例 3: gap
4. 用例 6: 批量输入
5. 用例 4 和用例 5: 异常验证

这样可以先确认主流程通了, 再验证边界情况.
