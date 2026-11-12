# 物业管家助手（wuyezhushou）

面向物业管家的 **Android 原生、离线优先** 管理工具。数据全部保存在本机 SQLite，不依赖账号、云端或公网服务；管家之间通过**临时组网**（局域网 / 蓝牙）互传楼栋数据实现对齐，并可在楼栋网格内用**离线语音听记**快速登记收费、事件与特征标签。

- 应用包名：`com.guanjiazhushou.assistant`
- 当前版本：`1.0.3`（versionCode 4）
- 适用平台：Android 7.0+（minSdk 24），首期仅 `arm64-v8a`

---

## 功能概览

| 模块 | 能力 |
| --- | --- |
| 楼栋与房屋 | 按「每层户数 / 楼层区间 / 避难层 / 剔除房间」批量生成住宅房间；商铺房间手动录入；楼层网格可视化；长按楼栋可改参/删除（导入楼栋只读，仅可删除副本） |
| 房屋标记 | 物业费按月（预存 / 欠费 N 月 / 正常）、业主原声、居住状态、特征画像（图标 + 描述，支持自定义） |
| 数据统计 | 总户数、出租率、欠费户、入住率、收缴率；居住状态分布、缴费饼图、特征柱图；指标可下钻清单；「我的 / 整体」统计范围切换 |
| 沟通记录 | 房间维度的文字、图片、语音沟通留痕 |
| 全局搜索 | 楼栋、房间、业主原声、备注、沟通记录模糊搜索 |
| 听记助手 | 网格内离线 ASR（sherpa-onnx Zipformer）+ 规则解析 + 端侧 Tiny NLU 复核；收费/事件/特征标签以提案卡确认后写入；点名唯一房号直接跳沟通页 |
| 定时提醒 | 事件与收费跟进的本机提醒（AlarmManager + 通知，支持精确闹钟权限引导） |
| 数据备份 | 密码加密的 `.pmbackup` 导出/导入，可经系统分享发出 |
| 临时组网 | 6 位验证码建网/加入，局域网 HTTP 为主通道、蓝牙为备用；按楼栋分享 `.pmex` 包，接收方得到只读副本；支持点对点分享与主动退出 |
| 个性化 | 6 套预设主题（蔚蓝商务 / 翡翠管治 / 暮紫渐变 / 暖砂质感 / 可爱粉 / 马卡龙）、可配置居住状态与入住统计规则 |
| 合规 | 首次启动隐私同意、隐私政策 / 服务条款 / 第三方许可、权限使用说明 |

**明确不做**：云端同步、账号登录、在线支付、短信/推送、iOS / Web / 小程序端、端侧大模型。

---

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 语言 / 构建 | Kotlin 2.3.10、AGP 9.0.0、Gradle Wrapper、JDK 17 |
| UI | Jetpack Compose（BOM 2024.10.01）、Material 3、Navigation Compose |
| 架构 | MVVM + UseCase + Repository、Hilt 2.59.2（KSP） |
| 存储 | Room 2.7.1（数据库版本 6，含 `MIGRATION_5_6`） |
| 图表 | ComposeCharts 0.2.5 |
| 组网传输 | OkHttp 4.12（局域网客户端）+ NanoHTTPD 2.3.1（局域网服务端）+ 经典蓝牙 |
| 端侧 AI | ONNX Runtime Android 1.23.1（文本意图 NLU，INT8 ≈ 3.3 MB，16 KB 页对齐）；sherpa-onnx 1.13.5（流式 ASR，Zipformer-zh-14M ≈ 31 MB） |
| SDK | compileSdk 36、targetSdk 36、minSdk 24 |

> 16 KB 页支持：`arm64-v8a` 原生库（sherpa-onnx 1.13.5、ONNX Runtime 1.23.1）均按 16 KB 对齐，满足 Google Play 自 2025-11-01 起对 targetSdk ≥ 35 应用的 16 KB 页要求。

---

## 目录结构

```
管家助手/
├── android/                  # Android 工程（Gradle 根）
│   ├── app/
│   │   ├── libs/             # sherpa-onnx AAR（本地依赖，避免 JitPack 抖动）
│   │   └── src/main/
│   │       ├── assets/asr/   # 离线 ASR 模型（encoder/decoder/joiner/tokens）
│   │       ├── assets/nlu/   # 文本意图 ONNX + vocab + tokenizer_meta.json
│   │       ├── java/com/guanjiazhushou/assistant/
│   │       │   ├── data/         # Room、备份、组网、蓝牙、ASR/NLU 实现
│   │       │   ├── domain/       # 模型、仓库接口、UseCase、听记领域
│   │       │   ├── presentation/ # Compose 页面与 ViewModel
│   │       │   └── di/           # Hilt 模块
│   │       └── res/raw/          # 隐私政策、服务条款、功能介绍等文案
│   └── version.properties    # versionCode / Major / Minor / Patch
├── doc/物业管家助手/          # 需求、解决方案、设计文档、实施计划
├── docs/superpowers/         # NLU 设计与实施计划（spec / plan）
├── scripts/                  # 构建、签名、版本、模型拉取脚本
├── tools/asr/                # ASR 模型说明
└── tools/nlu/                # 语料、训练、导出 ONNX、契约自检
```

---

## 环境要求

- JDK 17
- Android SDK：Platform 35、Build-Tools、Platform-Tools（`adb` 可选）
- 首次构建需联网下载 Gradle 依赖；运行期完全离线

### Windows 中文路径

工程路径包含中文时，建议用 `subst` 映射盘符后再构建：

```powershell
subst X: "C:\Work\Codes\git\管家助手"
cd X:\
```

---

## 构建与运行

```bash
# 单元测试
./scripts/build-android.sh test

# Debug 构建（有在线 adb 设备时自动安装）
./scripts/build-android.sh debug

# Release 构建（先校验签名配置，构建后自动 patch +1、versionCode +1）
./scripts/build-android.sh release

# 仅递增版本号 / 查看当前版本
./scripts/bump-app-version.sh
./scripts/bump-app-version.sh --show
```

产物路径：

- Debug：`android/app/build/outputs/apk/debug/app-debug.apk`
- Release：`android/app/build/outputs/apk/release/app-release.apk`

### 模型资产

仓库已内置运行所需的 ASR / NLU 资产，一般无需重新拉取：

```bash
./scripts/fetch-asr-models.sh   # 重新下载 sherpa-onnx Zipformer 模型到 assets/asr/
./scripts/fetch-sherpa-aar.sh   # 重新下载 sherpa-onnx AAR 到 app/libs/
```

NLU 语料、训练与导出见 `tools/nlu/`（`train_listen_intent.py`、`export_text_intent_onnx.py`、`verify_listen_intent_contract.py`）。运行时契约：`input_ids` / `attention_mask`（L=32）→ `intent_logits[1,6]` + `should_act_logit[1,1]`；资产缺失时硬失败，不降级。

### Release 签名

```bash
cp android/keystore.properties.example android/keystore.properties
./scripts/setup-release-signing.sh      # 首次：生成密钥库并写入本地配置
./scripts/verify-release-signing.sh     # 校验配置与 APK 签名
```

也可用环境变量 `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` 提供签名信息。

> `android/keystore.properties`、`android/keystore/*.jks|*.keystore` 已被 `.gitignore` 排除，**不要提交**。

---

## 开发进度

> 更新时间：2026-11-12　｜　基线：设计文档 v1.9.0 + 听记实施计划 v1.4.0

### 主应用里程碑（设计文档 §10）

| 阶段 | 交付物 | 状态 | 说明 |
| --- | --- | --- | --- |
| M1 基础框架 | 工程脚手架、数据库、底部导航 | ✅ 完成 | 实际为 3 个 Tab（楼栋 / 统计 / 我的）；「沟通」改为房间维度页面，不再单列空白 Tab |
| M2 楼栋模块 | 表单建楼、网格展示、房屋编辑 | ✅ 完成 | 含住宅/商铺生成、避难层与剔除房间、导入楼栋只读、长按改参/删除 |
| M3 统计模块 | 指标卡片、图表、清单钻取 | ✅ 完成 | 5 项指标 + 分布/饼图/柱图 + 我的/整体范围切换 |
| M4 我的模块 | 主题、备份、基础设置、共享页 | ✅ 完成 | 主题扩展为 6 套；备份 `.pmbackup` 密码加密；基础设置支持居住状态与特征标签增删改 |
| M5 组网传输 | 临时组网、点对点分享、局域网+蓝牙 | ✅ 代码完成，⏳ 真机联调 | 局域网 HTTP 主通道 + 蓝牙备用；`.pmex` 按楼栋分享、6 位验证码、只读导入、退出组网 |
| M6 测试发布 | 联调、修复、打包 APK | 🟡 进行中 | 单测与 Debug APK 已通过；Release 签名脚本就绪；多机型/蓝牙兼容性矩阵待补 |

### 听记助手（实施计划 M1–M6）

| 任务 | 内容 | 状态 |
| --- | --- | --- |
| M1 | `feature_tags.description` + `MIGRATION_5_6`（事件/提醒表） | ✅ |
| M2 | 事件 / 提醒 CRUD + AlarmManager + 事件列表 | ✅ |
| M3 | 网格听记壳（状态条、提案卡、确认写入） | ✅ |
| M3.5 | 端侧文本意图 NLU（ONNX INT8）+ 提案卡意图展示 | ✅ |
| M4 | 房号 + 业务词切沟通页（防抖、歧义对话框） | ✅ |
| M5 | sherpa-onnx 流式 ASR + 内置 Zipformer 模型 | ✅ 代码与 APK 完成；⏳ 真机飞行模式口述验收 |
| M6 | 隐私文案、权限引导、字幕与筛选手建 | ✅ |

### 待办

- [x] targetSdk 36（Android 16）升级 + 16 KB 页对齐（2026-11-12，待真机回归）
- [ ] 真机安装后**飞行模式口述验收**（收费 / 事件 / 特征标签 / 负例 / 跳转 / 提醒深链）
- [ ] 组网与点对点分享的真机联调（局域网、蓝牙备用、多机型兼容性）
- [ ] Release 签名构建与商店发布材料（截图、隐私表单）核对
- [ ] 语料扩充后重新训练 NLU（当前为种子语料）

---

## 文档索引

| 文档 | 说明 |
| --- | --- |
| [`doc/物业管家助手/开发设计文档.md`](doc/物业管家助手/开发设计文档.md) | 需求、数据模型、UI 规范、测试策略、里程碑 |
| [`doc/物业管家助手/听记助手与定时提醒-需求说明.md`](doc/物业管家助手/听记助手与定时提醒-需求说明.md) | 听记与提醒的功能需求 |
| [`doc/物业管家助手/听记助手与定时提醒-解决方案.md`](doc/物业管家助手/听记助手与定时提醒-解决方案.md) | 技术方案与架构 |
| [`doc/物业管家助手/听记助手与定时提醒-实施计划.md`](doc/物业管家助手/听记助手与定时提醒-实施计划.md) | 任务拆解与验收清单 |
| [`docs/superpowers/specs/2026-11-11-listen-intent-text-nlu-design.md`](docs/superpowers/specs/2026-11-11-listen-intent-text-nlu-design.md) | 文本意图 NLU 设计说明 |
| [`docs/superpowers/plans/2026-11-11-listen-intent-text-nlu.md`](docs/superpowers/plans/2026-11-11-listen-intent-text-nlu.md) | 文本意图 NLU 实施计划 |
| [`doc/物业管家助手/色彩方法论.md`](doc/物业管家助手/色彩方法论.md) | 主题与色彩规范 |

---

## 数据与隐私

- 核心业务数据保存在设备本地 SQLite（`guanjia_assistant.db`），不会自动上传开发者服务器。
- 听记识别与定时提醒均在本机完成，不联网、不使用云推送。
- 仅在主动使用组网 / 分享 / 备份时，数据才在本地网络或用户选择的文件范围内传输。
- 权限按需申请：网络/Wi-Fi、蓝牙、麦克风、通知/精确闹钟、图片与文件；应用内提供权限用途说明。

---

## 许可证

见 [LICENSE](LICENSE)。
