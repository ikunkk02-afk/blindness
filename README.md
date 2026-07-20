简体中文 | [English](README_EN.md)

# 失明症

**失明症** 是一个 Minecraft Fabric 1.21.1 模组，模拟严重的视觉障碍体验。玩家的视觉被大幅限制，必须通过导盲杖探路、声音定位、方块轮廓和无障碍提示来判断周围环境。

> 这是一个游戏机制模组，目标是提供具有挑战性但可正常游玩的失明体验，不是医学模拟，也不代表所有盲人或视障人士的真实体验。

---

## 核心特色

### 视觉限制

- 画面几乎全黑，仅能感知近处微弱明暗
- 使用 Veil 实现后处理效果和方块轮廓
- 提供 Veil 不可用时的纯黑 HUD 降级方案
- 兼容 Sodium 和 Iris

### 导盲杖

- 新玩家首次进入世界自动获得一根导盲杖（每名玩家仅一次）
- **右键单击**：探测前方方块，显示中心方块及其六个直接邻接面的轮廓
- **右键长按**：连续探测少量方块
- 轮廓会淡入显示、短暂停留后淡出
- 普通方块与矿物具有不同反馈

### 矿物识别

- 导盲杖探测到矿物时显示特殊橙色轮廓
- HUD 显示矿物名称、数量、方向和距离
- 提供方向性声音提示
- 支持原版矿物：煤、铁、铜、金、红石、青金石、钻石、绿宝石、下界石英、远古残骸
- 对使用 Fabric 通用矿石标签的模组矿物提供兼容
- **不是自动矿透**：只有导盲杖实际探测范围内的矿物才会显示

### 生物和方块声音感知

- 生物发出的声音会产生"声纹标记"显示在 HUD 上
- 声音来源方向以标记形式呈现，可区分普通生物和敌对生物
- 附近有危险生物时给出模糊警告
- 支持声音遮挡判断（墙后声音模糊化）

### 末影之眼无障碍提示

- 投掷末影之眼后，屏幕内显示追踪标识（绿色圆环 + 距离）
- 末影之眼飞出屏幕时，边缘显示方向箭头
- 实时显示距离，高处/低处有对应提示
- 末影之眼掉落后继续短暂标记掉落物（约 9 秒）
- 破碎时显示明确提示并播放声音
- **只追踪玩家自己投掷的末影之眼**，不显示要塞坐标

### 其他机制

- 奔跑过快或撞墙可能失去平衡摔倒
- 悬崖边缘有警告（文字 + 旁白 + 镜头反馈）
- 第一人称摔倒镜头倾斜
- 字幕方向模糊化
- 可屏蔽小地图、信息 HUD 等辅助模组

> **注意**：本模组的方块探测机制**不是安全级反作弊**。它依赖服务端确认来防止部分作弊行为，但无法替代专业的反作弊插件。

---

## 操作说明

| 操作 | 默认按键 | 功能 |
|---|---|---|
| 持有导盲杖右键单击 | 鼠标右键 | 探测前方方块环境 |
| 持有导盲杖右键长按 | 鼠标右键（按住） | 连续探测方块 |
| 打开设置界面 | `B` | 调整客户端配置 |

**末影之眼追踪**：投掷后自动显示，无需额外操作。追踪标识在末影之眼飞行/掉落时自动出现。

**配置**：按 `B` 打开模组设置，可关闭不需要的提示或调整音量/轮廓等参数。

---

## 安装要求

### 必须安装

- Minecraft 1.21.1
- Fabric Loader（≥ 0.19.3）
- Fabric API（≥ 0.116.14）
- Cardinal Components API（≥ 6.1.3）
- owo-lib（≥ 0.12.15.4+1.21）

### 推荐安装

- Veil（≥ 4.3.0）— 用于更好的视觉效果和轮廓渲染
- Player Animator（≥ 2.0.4）— 用于导盲杖动画
- Mod Menu（≥ 11.0.4）— 方便在游戏内打开设置

### 可选

- Sodium — 性能优化，已测试兼容
- Iris — 光影支持，已测试兼容

---

## 安装方法

1. 安装 Minecraft 1.21.1
2. 安装 Fabric Loader
3. 下载上述"必须安装"的所有前置模组，放入 `.minecraft/mods/`
4. 下载 `blindness-1.1.0.jar`（正式 remap JAR），放入 `.minecraft/mods/`
5. 启动游戏

> **注意**：请使用文件名包含版本号的正式 JAR（如 `blindness-1.1.0.jar`），不要安装 `-dev.jar` 或 `-sources.jar`。

---

## 配置说明

游戏内按 `B` 打开设置界面，或手动编辑 `config/blindness-client.properties`。

### 视觉设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| enableVisualPostProcessing | true | 启用 Veil 后处理（关闭后使用纯黑降级方案） |
| useDetailedModelOutlines | true | 使用详细模型轮廓 |
| contactHoldTime | 5.00 秒 | 接触轮廓保持时间 |
| contactFadeOutTime | 0.80 秒 | 接触轮廓淡出时间 |
| keepHeldCaneVisible | true | 保持手持导盲杖可见 |
| blackScreenBehindMenus | true | 菜单后方保持纯黑 |

### 矿物识别

| 配置项 | 默认值 | 说明 |
|---|---|---|
| enableOreHud | true | 显示矿物 HUD |
| enableOreSound | true | 矿物方向性声音 |
| oreOutlineDurationTicks | 60 | 矿物轮廓持续时间（tick） |
| maxRenderedOres | 16 | 最大同时显示矿物数量 |

### 末影之眼追踪

| 配置项 | 默认值 | 说明 |
|---|---|---|
| enableEnderEyeTrackingMarker | true | 启用追踪标识（总开关） |
| enableEnderEyeWorldMarker | true | 屏幕内世界空间标识 |
| enableEnderEyeEdgeArrow | true | 屏幕外方向箭头 |
| showEnderEyeDistance | true | 显示距离 |
| enableEnderEyeTrackingSound | true | 飞行追踪提示音 |
| enderEyeTrackingSoundIntervalTicks | 25 | 提示音间隔（10~100 tick） |
| droppedEnderEyeMarkerDurationTicks | 180 | 掉落标识持续（40~600 tick） |
| enableEnderEyeResultHint | true | 末影之眼结果提示 |

### 声音感知

| 配置项 | 默认值 | 说明 |
|---|---|---|
| entitySoundEchoEnabled | true | 启用生物声纹 |
| listeningChunkRadius | 1 | 监听区块范围（0=当前，1=3×3，2=5×5） |
| showOffscreenSoundEchoes | true | 显示屏幕外声纹 |

### 辅助功能

| 配置项 | 默认值 | 说明 |
|---|---|---|
| cameraShakeStrength | 0.45 | 镜头晃动强度 |
| showTutorial | true | 显示新手指南 |
| cliffWarningText | true | 显示悬崖警告文字 |
| hostileWarningText | true | 显示危险警告 |

---

## 兼容性说明

### 已测试兼容

- Sodium
- Iris（含和不含光影）
- Veil
- 单人世界
- Fabric 多人服务器

### 已知不兼容

- **DashLoader**：不支持。其着色器缓存恢复机制与 Veil 初始化时序冲突，会导致崩溃。请勿同时安装。

> 配方查看类模组（JEI、REI、EMI）**不会被屏蔽**，玩家仍可正常查询合成配方。

如遇到与其他模组的兼容问题，请提供 `latest.log`、崩溃报告、完整模组列表和复现步骤。

---

## 多人游戏说明

- 客户端和服务端**均需安装**本模组
- 每名玩家独立记录是否领取过开局导盲杖
- 矿物探测由服务端确认，防止作弊
- 末影之眼标识**只显示玩家自己投掷的实体**，其他玩家看不到
- 服务器管理员可使用 `/blindness enable/disable` 控制单个玩家的失明状态

---

## 命令

| 命令 | 权限 | 说明 |
|---|---|---|
| `/blindness enable` | 玩家本人 | 开启失明体验 |
| `/blindness disable` | 玩家本人 | 关闭失明体验 |
| `/blindness status` | 玩家本人 | 查看当前状态 |
| `/blindness reset` | OP 或创造模式 | 重置玩家数据 |

---

## 1.1.0 更新内容

### 新增

- 新玩家首次进入世界时自动获得导盲杖
- 导盲杖矿物识别系统（特殊轮廓、名称、数量、方向、距离）
- 矿物方向性声音提示
- 末影之眼破碎提示
- 末影之眼掉落提示
- 末影之眼屏幕内追踪标识（绿色圆环 + 距离）
- 末影之眼屏幕外方向箭头
- 掉落末影之眼短暂拾取标识
- 追踪提示音（可配置间隔）

### 修复

- 修复末影之眼 Mixin 注入失败导致的启动崩溃
- 修复末影之眼结果提示不明确的问题

### 兼容性

- Sodium、Iris 和 Veil 环境下的渲染兼容处理
- DashLoader 仍不受支持

---

## 问题反馈

请在 [GitHub Issues](https://github.com/ikunkk02-afk/blindness/issues) 提交问题，附上：

```
Minecraft 版本：
Fabric Loader 版本：
模组版本：
前置模组版本：
是否安装 Sodium/Iris：
问题描述：
复现步骤：
latest.log：
```

---

## 许可证

本项目采用 MIT License，详见仓库中的 `LICENSE` 文件。

---

## 致谢

- [Fabric](https://fabricmc.net/) 和 [Fabric API](https://github.com/FabricMC/fabric)
- [Veil](https://github.com/FoundryMC/Veil) — 渲染后处理框架
- [Cardinal Components API](https://github.com/Ladysnake/Cardinal-Components-API)
- [owo-lib](https://github.com/wisp-forest/owo-lib)
- [Player Animator](https://github.com/KosmX/playerAnimator)
- 参与测试并提供反馈的玩家和观众
