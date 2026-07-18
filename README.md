# 失明症（Blindness）

适用于 Minecraft 1.21.1 的 Fabric 模组，以严重压暗的视觉、声音反馈、导盲杖扫描和服务器判定的绊倒机制，构成一种依赖声音、轮廓与记忆移动的游戏体验。

> 本模组以游戏化方式模拟部分重度视力障碍者可能使用的环境感知方式，不代表所有盲人或视障人士的真实体验。

当前版本：`0.1.0`　许可证：MIT　Java：21

## 前置依赖

- Fabric Loader 0.19.3 或更高版本
- Fabric API 0.116.14+1.21.1
- Veil 4.3.0
- Cardinal Components API 6.1.3（base、entity）
- owo-lib 0.12.15.4+1.21
- Player Animator 2.0.4+1.21.1

客户端安装模组本体和以上全部前置；专用服务端安装模组本体、Fabric API、CCA 与 owo-lib，Veil 和 Player Animator 只放客户端。使用 Java 21 启动 Minecraft。服务端不会加载 Veil 渲染器、MinecraftClient 或任何 Screen 类。

## 使用方式

导盲杖可以按以下形状有序合成：顶部为铁锭和白色羊毛，中间与底部各一根木棍。

- 短按右键：敲击前方最多 4 格内的第一个实际表面，播放材质反馈并产生声波。
- 长按右键约 1 秒：在前方约 90°、最多 5 格范围内完成扇形扫描；扫描时移动速度降低，疾跑会取消扫描。
- 被服务器确认的表面形成约 3 秒的临时安全路径，经过这些位置时绊倒概率显著降低。
- 默认按 `B` 打开“失明症设置”。

## 指令

- `/blindness enable`：开启当前玩家的失明症体验。
- `/blindness disable`：立即关闭体验并恢复正常视觉，是随时可用的无障碍退出方式。
- `/blindness status`：显示状态、视觉模式、导盲杖熟练度和累计摔倒次数。
- `/blindness reset`：重置玩家持久数据和临时状态，仅创造模式玩家或权限等级 2 的管理员可用。

新玩家默认开启体验，只接收一次简短教程。死亡重生会保留开启状态、视觉模式、熟练度、累计摔倒、成功扫描次数和教程状态；声波、平衡、摔倒和路径缓存不会保存。

## 视觉与声波实现

Veil 在 `after_level` 阶段、GUI 绘制之前运行 `blindness:blindness` 后处理管线。着色器从主颜色、主深度和 Veil 动态法线缓冲读取数据：

- 对世界颜色进行多采样模糊、去饱和、降低对比度和近距离明暗保留；高亮区域只留下柔和扩散光团。
- 使用深度差和法线差提取屏幕上最前方的几何边缘，再以服务器发送的扫描原点和传播半径裁剪成扩张球壳。
- 清晰内线与柔和外晕只覆盖几何边缘，不涂满平面，也不会使用方块贴图作为扫描结果。
- 轮廓只处理主深度缓冲已经可见的表面，因此不会显示墙后的方块或矿物。

使用 Iris 光影时自动改用 `blindness:blindness_depth` 深度差降级管线。若 Veil 管线或着色器不能加载，模组会记录错误并关闭视觉管线，保留导盲杖、声音、指令和服务器玩法，不让客户端直接崩溃。

## 摔倒系统

服务器每两 Tick 只检查玩家移动方向附近的碰撞形状、地面高度和局部实体 AABB。普通平地基础风险为零；升降高度、正面撞墙、冰面急转和实体尺寸会增加风险，蹲下慢走、已扫描路径和平衡状态会降低风险。同一危险只判定一次，不会每 Tick 累积抽奖。

摔倒时会停止疾跑、削减水平速度、播放 Player Animator 动画并暂时阻止攻击、挖掘、跳跃和使用物品。第一人称倾斜与晃动有硬上限，可在设置中降低或关闭。普通绊倒伤害被限制在一颗心以内；火焰、岩浆、仙人掌和高处跌落继续使用原版伤害。

## 配置

客户端 `config/blindness-client.json5`：视觉后处理、亮度、模糊、饱和度、轮廓粗细/亮度/时长、传播速度、生物声音轮廓、镜头晃动、第一人称倾斜、教程和提示音量。

服务端 `config/blindness-server.json5`：扫描范围、冷却、路径有效期、结果/路径容量、伤害上限和风险倍率。普通客户端只能接收经过钳制的只读快照，不能修改玩法参数。

## 已实现功能

- CCA 玩家持久化、死亡复制和服务器到客户端同步
- 默认开启、一次性教程、四个 `/blindness` 子命令
- 原创导盲杖物品、贴图、模型、配方和材质分类声音
- 服务器权威短敲、扇形扫描、受限结果包和临时安全路径
- 地形/碰撞/实体尺寸/速度/平衡综合绊倒判定
- 五个原创 Player Animator 动画、第一人称镜头与控制锁定
- Veil 深度/法线声波轮廓及 Iris 深度降级管线
- 生物真实发声时的短暂、深度受限模糊脉冲
- 简体中文与英文文本、owo-lib 设置页和可修改按键

## 兼容性与已知问题

- 纯 Fabric 环境已经在 Windows 11、Java 21 上实际启动并创建新世界；Veil 资源与 19 个模组着色器在世界内编译成功。
- Sodium `0.8.12+mc1.21.1` 与当前锁定的 Fabric Loader `0.19.3` 实测在第三方 `BakedQuadMixin` 应用阶段失败：Sodium 的方法缺少其自身配置要求的 `@Overwrite`。错误发生在失明症和 Veil 管线能够降级之前，因此当前组合不可用；不要为规避该问题降级本项目依赖。
- Sodium `0.8.12` + Iris `1.8.14-beta.1` 还会在 Loader 的开发环境重映射阶段触发 Iris `MixinLevelRenderer` 的 `@ModifyArg` 解析异常。此组合同样不标记为兼容，等待上游版本适配后重新测试。
- Iris 启用光影包时使用深度差降级轮廓，不具有法线差带来的转角细节。
- 生物定位是由真实声音触发的粗略提示，不是雷达；静止且未发声的生物不会显示。
- 声波轮廓来自当前相机深度，只能强调玩家当前可见的表面，刻意不提供透墙信息。

## 构建

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

兼容性开发运行：

```powershell
.\gradlew.bat runClient -PcompatTest=sodium
.\gradlew.bat runClient -PcompatTest=iris
```

构建产物位于 `build/libs/blindness-0.1.0.jar`。

## English summary

Blindness is a Fabric 1.21.1 mod that creates a gameplay-oriented severe-vision-impairment experience. The world remains faintly visible, while a guidance cane produces server-authorized, depth-respecting Veil scan outlines. It includes persistent player preferences, temporary scanned paths, contextual trip detection, Player Animator fall/cane animations, an owo-lib accessibility screen, and `/blindness disable` as an immediate opt-out.

This is a game-oriented simulation and does not represent every blind or visually impaired person's lived experience.
