# Hermes-Stats

## 项目简介

Hermes-Stats 是一个**纯前端展示**的 Fabric 模组，为服务端提供一套基于聊天栏的统计排行榜界面。

模组本身不采集任何数据、不写数据库、不对外提供 API——它只做三件事：

1. 注册 `/hermes` 命令树；
2. 调用 **Hermes Lib** 的只读接口（`hermeslib.api.HermesApi.provider()`）读取统计数据；
3. 在聊天栏里分页渲染榜单，并给所有条目挂上可点击的下钻按钮。

统计数据的采集、分域（真人 / 假人）与持久化全部由 Hermes Lib 负责，Hermes-Stats 是它的展示层。
这样拆分的好处是：采集逻辑升级不会影响展示，展示换皮也不需要动底层数据。

设计上遵循**三级路径**：入口 → 分类 → 指标 → 榜单。27 个指标分成 6 大类，避免玩家面对一长串指标名无从下手；
翻页、切真人榜 / 假人榜时始终保留当前指标与页码，底部常驻「返回分类」和「切换域」按钮。

## 功能特点

- **零参数可用**：`/hermes` 直接输出入口菜单，不报用法错误。
- **6 大分类**：活跃 / 采集 / 战斗 / 探索 / 社交 / 其它，指标按语义自动归类，新增指标自动落到「其它」不会丢。
- **分页榜单**：每页 10 条，多请求 1 条精确判断「下一页」是否显示，页码越界时提示有效页码范围。
- **真 / 假人分榜**：`/hermes` 与 `/hermes fake` 两棵子树共用同一套节点构建器，一键切换数据域。
- **我的总览**：`/hermes me` 只读一次全部指标，**不做任何排名扫描**，开销恒定。
- **单项排名**：`/hermes me <指标>` 展示自己的数值与名次，未上榜时提示「未进入前 1000 名」。
- **自我高亮**：榜单里的自己以金色加粗显示（按 `effectiveUuid` 比对，假人 UUID 也不会错位）。
- **全量可点击**：每一处可点文本都同时带点击事件与悬停提示，路径不迷路。
- **结果缓存**：榜单查询走 5 秒 TTL 缓存，翻页来回切不会反复触发全量排序。
- **智能补全**：`metric` / `category` 参数补全带中文名与分类说明，大小写不敏感。

## 安装与使用

### 环境要求

- Minecraft **26.3**
- Java **25+**
- Fabric Loader **0.19.5**
- Fabric API **0.164.0+26.3**（提供 `fabric-command-api-v2` 与 `fabric-lifecycle-events-v1`）
- **Hermes Lib 1.0.0**（数据来源，必须安装）
  - 安装的模组 ID 是 **`hermes`**（jar 文件名 `hermes-lib-1.0.0.jar`），本模组的 `fabric.mod.json` 依赖的就是它
  - 仓库内已附带 `libs/hermes-lib-1.0.0.jar`，也可改用 Maven 坐标 `com.hermeslib:hermes-lib:1.0.0`

### 安装步骤

```bash
git clone <repo-url>
cd Hermes-Stats

# 首次构建前：把 Hermes Lib 的 jar 放进 libs/（见 libs/README.md）
./gradlew build
```

`libs/hermes-lib-1.0.0.jar` 属于第三方构建产物（约 11.7 MB），未纳入版本管理，
请按 [libs/README.md](libs/README.md) 里的两种方式之一放入。

把 `build/libs/hermes-stats-1.0.0.jar` 放进服务端的 `mods/` 目录，同时确保已安装 Hermes Lib 与 Fabric API，重启服务端即可。

### 命令一览

```
/hermes                          入口菜单（分类按钮 + 全部指标 / 我的总览 / 假人榜）
/hermes list                     分类目录，每行「▸ 分类名 (N 项)」
/hermes list <category>          该分类的指标列表，每行「▸ 中文名 (dbName) [查看榜]」
/hermes top                      提示选指标 + 分类按钮
/hermes top <metric>             榜单 第 1 页
/hermes top <metric> <page>      榜单 第 N 页
/hermes me                       我的总览（只调一次 getAll，不做排名扫描）
/hermes me <metric>              我的单项排名
/hermes fake                     假人域入口
/hermes fake list [<category>]   假人域分类目录 / 指标列表
/hermes fake top [<metric>] [<page>]  假人域榜单
```

`category` 可选值：`activity` `gather` `combat` `explore` `social` `other`；
`metric` 使用数据库取值（如 `blocks_mined`），枚举名（`BLOCKS_MINED`）同样认，大小写不敏感。

### 指标分类

| 分类 | id | 指标数 | 指标 |
|---|---|---|---|
| 活跃 | `activity` | 3 | `playtime_ticks` `playtime_seconds` `jumps` |
| 采集 | `gather` | 8 | `blocks_mined` `blocks_placed` `items_crafted` `items_picked_up` `items_dropped` `items_used` `items_damaged` `items_broken` |
| 战斗 | `combat` | 5 | `mob_kills` `player_kills` `deaths` `damage_dealt` `damage_taken` |
| 探索 | `explore` | 7 | `fish_caught` `distance_walk` `distance_sprint` `distance_swim` `distance_ride` `distance_elytra` `distance_total` |
| 社交 | `social` | 4 | `villager_trades` `animals_bred` `animals_tamed` `enchantments` |
| 其它 | `other` | 0 | 兜底分类 |

### 数值单位

| 指标 | 规则 | 示例 |
|---|---|---|
| `playtime_ticks` | 20 tick = 1 秒，换算时分秒 | `3 分 6 秒` |
| `playtime_seconds` | 秒，换算时分秒 | `1 小时 2 分 5 秒` |
| `distance_*` | 单位是厘米（1 格 = 100），除 100 变格 | `123.5 格` |
| `damage_*` | 单位是点，保留整数 | `1,234 点` |
| 其它 | 计数，千分位 | `987,654` |

### 开发说明

- **依赖声明**：Loom 1.17.21 已移除 `modImplementation` 配置，模组依赖统一用 `implementation` 声明。
- **命令树结构**：`/hermes fake` 与 `/hermes` 共用 `buildListNode(domain)` / `buildTopNode(domain)` 两个构建器，
  数据域在构建期就固定，因此不存在 Brigadier 的位置参数歧义。
- **缓存**：`TopCache` 以 `(指标, 数据域, limit)` 为键，TTL 5 秒，`ServerLifecycleEvents.SERVER_STOPPED` 时清空；
  所有 `top()` 调用统一经过它。
- **文案前缀**：`[Hermes-Stats]` 只加在失败消息上——标题本身已含 `✦ Hermes-Stats ✦` 标记，
  再叠一层会变成重复的 `[Hermes-Stats] ✦ Hermes-Stats ✦ ...`。

> ⚠️ 早前设计文档中提到「29 个指标」，Hermes Lib 1.0.0 的 `Metric` 枚举实际为 **27 个**，
> 上表已按真实枚举归类，27 个全部命中、无一遗漏。
