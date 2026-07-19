# 失明症（Blindness）

适用于 Minecraft 1.21.1 的 Fabric 模组。世界保持纯黑，玩家依靠导盲杖接触、短暂的真实方块模型轮廓、声音和模糊安全提示行动。

> 本模组以游戏化方式模拟部分重度视力障碍者可能使用的环境感知方式，不代表所有盲人或视障人士的真实体验。

当前版本：`0.1.0`；Java：21；许可证：MIT。

## 前置依赖

- Fabric API 0.116.14+1.21.1
- Veil 4.3.0
- Cardinal Components API 6.1.3
- owo-lib 0.12.15.4+1.21
- Player Animator 2.0.4+1.21.1
- Mod Menu 11.0.4

专用服务器只加载通用玩法代码，不加载 Screen、MinecraftClient、Veil 渲染或字幕 Mixin。

## 导盲杖与高级轮廓

- 短按在服务端执行一次最多 4 格的真实接触射线；长按在一秒内进行四次有限方向接触，不会生成扇形或大范围声波扫描。
- 命中方块后只显示中心方块、直接正交相邻方块和必要的双格结构配对方块。
- 普通方块使用当前 `BlockState` 的真实 `BakedModel`、纹理 Alpha 裁剪和世界深度遮挡；方块实体安全回退到真实 `VoxelShape`。
- 中心轮廓保持约 5 秒并高于相邻轮廓；第一人称和第三人称共用同一深度正确的 Veil 遮罩。
- 世界和暂停菜单后方仍然保持纯黑，旧版球形声波、扩散圆环与大范围地形扫描没有恢复。

## 悬崖警告

悬崖检查只在导盲杖短按接触或长按摆动的每次实际接触时运行，空手站立不会获得地形雷达。服务端沿水平朝向在 4 格内每 0.5 格采样，并读取实际碰撞形状的可行走顶面；眼部和脚部碰撞射线会在实体墙前截断路径。

- 平地、楼梯、半砖和单次下降 1 格通常安全。
- 突然下降 2–3 格：双重提示音和“前方有明显落差”。
- 突然下降 4 格及以上、岩浆、火焰、虚空或没有安全落脚面：三连提示音和严重警告。
- 同一边缘与朝向默认 2 秒冷却；严重风险可以越过刚发生的普通风险提示。
- 不发送深度、坐标或安全路径，不显示悬崖底部，也不会移动或转向玩家。

## 生物声音环境感知

服务端在 `Entity#playSound` 的真实生物声音调用上分类脚步、环境叫声、受伤、攻击、死亡、飞行、游泳与溅水。它不会每 Tick 遍历全部生物，也不会让每只生物定时发射雷达。

- 脚步最多显示脚下及正交相邻的 7 个外露方块。
- 环境叫声最多显示 10 个外露方块。
- 受伤、攻击或死亡声音最多显示 12 个外露方块。
- 只向默认 12 格听觉范围内、启用失明症的玩家发送；每名玩家默认每秒最多 8 个普通声音事件。
- 每个方块必须在已加载区块内并通过玩家视线首碰撞面检查，因此墙后矿石不会被揭示。
- 声音轮廓使用已有真实模型渲染，亮度和持续时间弱于导盲杖；缓存按方块去重，导盲杖优先，满载时先淘汰旧的低优先级声音轮廓。
- 不绘制生物模型、名称、血量、数量、坐标、精确距离、HUD 箭头或雷达点。

## 模糊敌对危险提示与字幕

服务端每 10 Tick 对每名玩家附近默认 12 格 AABB 做一次空间查询，最多处理 32 个 `MobEntity`。标准 `HostileEntity`、继承该基类的模组生物，以及已经把玩家设为目标的生物才算危险；普通动物、村民和宠物不触发。

新威胁进入范围或开始以玩家为目标时会合并为一次“附近似乎有危险”，默认冷却 5 秒；近距离或开始攻击可以使用缩短后的冷却。反馈只包含模糊文本和随距离改变音量的低沉声音，不包含名称、数量、坐标、距离或方向箭头。

启用失明症时，原版敌对生物字幕会改为“附近有危险的声音”，普通生物字幕会改为“附近有生物的声音”；敌对字幕方向加入 25–45 度误差。`/blindness disable` 后恢复原版字幕。

## 地图和世界信息模组限制

默认严格检查使用 Fabric Loader 的真实 Mod ID，不读取 JAR 文件名，也不按名称是否包含 `map` 猜测。主菜单仍能正常显示；进入已有单人世界、创建世界或通过服务器列表、直接连接、快速连接、重连发起多人连接之前会打开冲突页面。Fabric 客户端加入事件还提供第二层保险并立即断开绕过路径。

默认地图类 ID：

`xaerominimap`、`xaeroworldmap`、`journeymap`、`voxelmap`、`ftbchunks`、`antiqueatlas`、`antique_atlas`、`antique_atlas_4`、`map_atlases`、`mapatlases`

默认信息 HUD ID：

`jade`、`wthit`、`hwyla`、`waila`、`theoneprobe`

冲突页面显示名称、Mod ID、版本和类别，提供返回标题、打开 mods 文件夹和复制结果；没有“仍然进入世界”按钮，不崩溃、不删除文件，也不修改其他模组配置。

JEI、REI、EMI 及配方/用途查询不受限制。硬允许 ID 为 `jei`、`roughlyenoughitems`、`roughlyenoughitems-api`、`emi`。

兼容检查可在 owo-lib 设置中配置总开关、类别开关、追加 ID 和忽略 ID；修改后需要重启。关闭总开关会在设置名称中明确提示体验会被削弱。默认列表仍由代码维护，配方查看器的硬允许规则优先。

没有在 `fabric.mod.json` 使用 `breaks`，因为那会让 Loader 在主菜单出现前阻止整个游戏启动，无法提供可读冲突页面。该本机客户端检查用于模组包与体验完整性，不是安全级反作弊；客户端报告理论上可以被修改或伪造。

## 配置与命令

按 `B` 或通过 Mod Menu 打开同一个 owo-lib 客户端配置界面。服务端配置位于 `config/blindness-server.json5`，负责悬崖阈值、声音距离/速率、敌对检测范围与冷却；客户端配置负责音量、文本、旁白、轻微镜头反馈、轮廓亮度、字幕模糊和本机兼容检查。

- `/blindness enable`：开启体验。
- `/blindness disable`：恢复正常视觉、字幕并清空临时轮廓。
- `/blindness status`：查看当前状态。
- `/blindness reset`：重置持久与临时状态。

## 构建与验证

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物位于 `build/libs/blindness-0.1.0.jar`。

## English summary

Blindness is a Fabric 1.21.1 mod built around a fully black world, server-authoritative four-block guidance-cane contact, depth-correct baked-model outlines, event-driven local block reveals from real creature sounds, vague hostile awareness, privacy-preserving entity subtitles, and pre-entry restrictions for map/world-information mods. Recipe viewers such as JEI, REI, and EMI remain allowed. The local compatibility check protects the intended experience; it is not security-grade anti-cheat.
