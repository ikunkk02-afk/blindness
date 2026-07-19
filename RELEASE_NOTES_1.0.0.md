# 失明症 1.0.0

**Minecraft 1.21.1 · Fabric · Java 21**

一款完全改变游戏视觉体验的沉浸式模组。世界全黑，依靠触觉和听觉探索。

## 主要内容

- 🌑 **完全黑暗的世界** — Veil 后处理管线实现真正的全黑渲染
- 🦯 **导盲杖** — 短按敲击、长按扫动，以真实碰撞检测前方方块
- ✨ **真实模型轮廓** — 短草、栅栏、楼梯等显示与方块模型一致的高精度轮廓
- ⛰️ **悬崖警告** — 导盲杖探测时检测 2 格以上危险落差和岩浆
- 🚶 **摔倒系统** — 疾跑撞到障碍物或未探测地形时可能摔倒
- 👂 **生物声音声纹** — 生物发声时屏幕显示方向和高度的声纹标记
- ⚠️ **模糊危险感知** — 检测到附近敌对生物但不暴露名称和数量
- 🎛️ **可调辅助范围** — 监听区块半径、声音轮廓范围均可配置
- 🔧 **Mod Menu 集成** — 游戏内快捷键 B 或 Mod Menu 打开配置
- 🗺️ **兼容性保护** — 进入世界前检测地图和信息 HUD 模组

## 安装要求

- Minecraft 1.21.1
- Fabric Loader ≥ 0.19.3
- Java 21

**必需前置**：Fabric API、Veil、Cardinal Components API、owo-lib、Player Animator
**推荐前置**：Mod Menu（仅客户端）

## 安装方法

1. 安装 Minecraft 1.21.1 和 Fabric Loader
2. 下载所有必需前置模组
3. 下载 `blindness-1.0.0+1.21.1.jar`
4. 将所有模组放入 `mods` 文件夹
5. 启动游戏

## 重要兼容性说明

- 🚫 地图/小地图模组（Xaero's、JourneyMap、FTB Chunks 等）会被阻止
- 🚫 方块信息 HUD（Jade、WTHIT、WAILA 等）会被阻止
- ✅ JEI、REI、EMI 等配方查询模组可以使用
- ✅ Mod Menu 推荐安装用于配置

## 首次使用提示

- 世界全黑是正常设计，不是 Bug
- 需要制作导盲杖：铁锭 + 白色羊毛 + 木棍（×2）
- 按 `B` 键打开设置，调整声音感知范围
- 可通过 `/blindness disable` 临时恢复正常视觉进行排错

## 已知问题

当前没有已确认的严重问题。

## 反馈

[GitHub Issues](https://github.com/ikunkk02-afk/blindness/issues)
