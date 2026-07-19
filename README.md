# 失明症（Blindness）

适用于 Minecraft 1.21.1 的 Fabric 模组。纯黑视觉、声音反馈、导盲杖接触感知和服务器判定的绊倒机制，共同构成依赖听觉、短时轮廓与记忆移动的游戏体验。

> 本模组以游戏化方式模拟部分重度视力障碍者可能使用的环境感知方式，不代表所有盲人或视障人士的真实体验。

当前版本：`0.1.0`　许可证：MIT　Java：21

## 前置依赖

- Fabric Loader 0.19.3 或更高版本
- Fabric API 0.116.14+1.21.1
- Veil 4.3.0
- Cardinal Components API 6.1.3（base、entity）
- owo-lib 0.12.15.4+1.21
- Player Animator 2.0.4+1.21.1
- Mod Menu 11.0.4（客户端可选，用于模组列表中的配置按钮）

客户端安装模组本体及 Fabric API、Veil、CCA、owo-lib、Player Animator；推荐安装 Mod Menu。专用服务端只需要模组本体、Fabric API、CCA 与 owo-lib，不需要 Mod Menu、Veil 或 Player Animator。使用 Java 21 启动。

## 导盲杖

导盲杖可以按配方有序合成：顶部为铁锭和白色羊毛，中间与底部各一根木棍。

- 短按右键：服务端从玩家视线向前检测最多 5 格。必须实际碰到方块，才会短暂显示该方块及最多六个正交相邻方块。
- 未命中：播放挥空声音和敲击动画，不显示任何环境轮廓。
- 长按右键：播放左右摆杖动画，在一秒内执行最多四次有限方向接触；每次仍只能命中一个实际方块，不会照亮扇形区域。疾跑会取消，摆杖期间移动速度降低。
- 中心方块立即以更亮、更粗的轮廓出现；相邻方块延迟约 0.1 秒且强度较低。轮廓淡入约 0.1 秒、完整保持 5 秒，再用约 0.8 秒平滑淡出。
- 最近确认的中心和直接相邻位置会在约 2.5 秒内降低绊倒风险，但不会完全免疫摔倒。

所有命中、距离、遮挡、数量和路径认知都由服务端确认。单次接触最多发送 8 个位置，不接受客户端提交任意方块坐标，也不会向无关玩家广播环境结果。

## 视觉

世界背景始终为纯黑，普通方块纹理和颜色不可见。Veil 后处理只合成服务器授权的接触轮廓、可选的声音提示以及 GUI/HUD：

- 普通方块轮廓来自当前 `BlockState` 的实际 `BakedModel`：模型像素先写入独立遮罩，原纹理颜色不会进入最终画面；纹理 Alpha 仍用于裁剪，因此短草、花、藤蔓和火把保留真实可见像素形状。
- 模型遮罩复制当前帧的世界深度并使用 `LEQUAL` 测试；随后由屏幕空间边缘提取和横/纵两阶段膨胀生成默认 4 像素中心线、3 像素相邻线及 10/8 像素光晕。被墙完全遮挡的方块和墙后矿物不会显示。
- 方块实体渲染器使用的顶点格式和材质不统一。箱子、末影箱、告示牌、床、横幅、潜影盒、钟、讲台书、头颅和装饰罐等方块实体目前安全降级为实际 `VoxelShape`；流体使用实际流体表面形状。普通植物、火把、栅栏、墙、玻璃板、门、楼梯、半砖、铁轨和梯子不走该降级路径。
- 暂停菜单和其他 GUI 后方默认保持黑色，GUI 本身正常显示。
- 第一人称导盲杖默认保持可见。
- 所有接触轮廓淡出后，画面重新只剩纯黑世界与 GUI/HUD。

旧版球形声波、扩散圆环、半径世界扫描和扇形批量扫描已经删除。shader 不再使用 `ScanOrigin`、`ScanRadius`、`ScanProgress` 或传播速度。

## 配置

默认按 `B` 打开 owo-lib 生成的“失明症设置”。安装 Mod Menu 后也可以通过：

```text
主菜单 → 模组 → 失明症 → 配置
```

两个入口打开同一个 `config/blindness-client.json5`，使用同一个 owo 配置实例。可切换详细模型轮廓，并分别调整中心/相邻轮廓粗细、亮度、光晕半径和强度，以及淡入、5 秒保持、淡出、相邻出现延迟、镜头晃动、手持导盲杖显示和菜单后方黑屏。

旧配置中的 `outlineDuration`、`waveSpeed`、`tapRange`、`sweepRange`、`scannedPathTtlTicks` 和 `maxScanHits` 暂时保留以兼容现有 JSON5 文件，但已从配置界面隐藏且不再参与效果。新服务端路径认知使用 `contactPathTtlTicks`。

## 指令

- `/blindness enable`：开启当前玩家的失明症体验。
- `/blindness disable`：关闭体验、恢复正常视觉并立即清空接触轮廓。
- `/blindness status`：显示状态、视觉模式、导盲杖熟练度和累计摔倒次数。
- `/blindness reset`：重置玩家持久数据和临时状态，仅创造模式玩家或权限等级 2 的管理员可用。

玩家开启状态、视觉模式、熟练度、累计摔倒、成功接触次数和教程状态由 CCA 保存。接触轮廓、平衡、摔倒动画状态和路径缓存不会写入玩家存档，并会在断线、切换维度、死亡或禁用功能时清空。

## 摔倒系统

服务器检测移动方向附近的碰撞形状、地面高度和局部实体。升降高度、正面撞墙、冰面急转和实体尺寸会增加风险，蹲下慢走、近期接触确认区域和平衡状态会降低风险。

摔倒时会停止疾跑、削减水平速度、播放 Player Animator 动画并暂时阻止攻击、挖掘、跳跃和使用物品。第一人称倾斜与晃动有硬上限，可在设置中降低或关闭。普通绊倒伤害有上限；火焰、岩浆、仙人掌和高处跌落继续使用原版伤害。

## 构建与验证

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物位于 `build/libs/blindness-0.1.0.jar`。

## English summary

Blindness is a Fabric 1.21.1 mod with a fully black world and server-authoritative guidance-cane contact sensing. A real cane hit reveals only the struck block and its direct orthogonal neighbors for five seconds. Ordinary blocks use alpha-clipped baked-model masks, world-depth occlusion, and screen-space dilation instead of cube selection boxes. Holding the cane performs up to four limited contacts instead of a wide scan. Both the in-game key and optional Mod Menu integration open the same owo-lib configuration screen.

This is a game-oriented simulation and does not represent every blind or visually impaired person's lived experience.
