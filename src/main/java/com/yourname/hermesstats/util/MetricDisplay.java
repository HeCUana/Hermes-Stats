package com.yourname.hermesstats.util;

import themislib.api.Metric;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * 指标的中文名与数值格式化。
 *
 * <p>单位口径（与 Themis Lib 的数据库约定一致）：</p>
 * <ul>
 *   <li>{@code playtime_ticks} —— 20 tick = 1 秒，换算成「时 分 秒」；</li>
 *   <li>{@code playtime_seconds} —— 已是秒，换算成「时 分 秒」；</li>
 *   <li>{@code distance_*} —— 单位是厘米（1 格 = 100），除以 100 变成格；</li>
 *   <li>{@code damage_*} —— 单位是点，保留整数；</li>
 *   <li>其余 —— 计数，千分位显示。</li>
 * </ul>
 */
public final class MetricDisplay
{
    private static final Map<Metric, String> NAMES = new EnumMap<>(Metric.class);

    static
    {
        // ---- 活跃 ----
        NAMES.put(Metric.PLAYTIME_TICKS, "在线时长");
        NAMES.put(Metric.PLAYTIME_SECONDS, "在线时长");
        NAMES.put(Metric.JUMPS, "跳跃次数");

        // ---- 采集 ----
        NAMES.put(Metric.BLOCKS_MINED, "挖掘方块");
        NAMES.put(Metric.BLOCKS_PLACED, "放置方块");
        NAMES.put(Metric.ITEMS_CRAFTED, "合成物品");
        NAMES.put(Metric.ITEMS_PICKED_UP, "拾取物品");
        NAMES.put(Metric.ITEMS_DROPPED, "丢弃物品");
        NAMES.put(Metric.ITEMS_USED, "使用物品");
        NAMES.put(Metric.ITEMS_DAMAGED, "物品损耗");
        NAMES.put(Metric.ITEMS_BROKEN, "损坏物品");

        // ---- 战斗 ----
        NAMES.put(Metric.MOB_KILLS, "击杀生物");
        NAMES.put(Metric.PLAYER_KILLS, "击杀玩家");
        NAMES.put(Metric.DEATHS, "死亡次数");
        NAMES.put(Metric.DAMAGE_DEALT, "造成伤害");
        NAMES.put(Metric.DAMAGE_TAKEN, "受到伤害");

        // ---- 探索 ----
        NAMES.put(Metric.FISH_CAUGHT, "钓鱼次数");
        NAMES.put(Metric.DISTANCE_WALK, "步行距离");
        NAMES.put(Metric.DISTANCE_SPRINT, "疾跑距离");
        NAMES.put(Metric.DISTANCE_SWIM, "游泳距离");
        NAMES.put(Metric.DISTANCE_RIDE, "骑乘距离");
        NAMES.put(Metric.DISTANCE_ELYTRA, "鞘翅飞行距离");
        NAMES.put(Metric.DISTANCE_TOTAL, "移动总距离");

        // ---- 社交 ----
        NAMES.put(Metric.VILLAGER_TRADES, "村民交易");
        NAMES.put(Metric.ANIMALS_BRED, "繁殖动物");
        NAMES.put(Metric.ANIMALS_TAMED, "驯服动物");
        NAMES.put(Metric.ENCHANTMENTS, "附魔次数");
    }

    private MetricDisplay()
    {
    }

    /**
     * 指标的中文名（没有映射时回退到 dbName）。
     *
     * @param metric 指标
     * @return 中文名
     */
    public static String name(Metric metric)
    {
        if (metric == null)
        {
            return "未知指标";
        }
        return NAMES.getOrDefault(metric, metric.dbName());
    }

    /**
     * 按指标单位格式化数值。
     *
     * @param metric 指标
     * @param value  原始累计值
     * @return 面向玩家的可读文本
     */
    public static String format(Metric metric, long value)
    {
        if (metric == null)
        {
            return String.format(Locale.ROOT, "%,d", value);
        }

        String dbName = metric.dbName();
        if (dbName.equals("playtime_ticks"))
        {
            return duration(value / 20L);
        }
        if (dbName.equals("playtime_seconds"))
        {
            return duration(value);
        }
        if (dbName.startsWith("distance_"))
        {
            // 厘米 → 格（1 格 = 100 厘米）
            return String.format(Locale.ROOT, "%,.1f 格", value / 100.0);
        }
        if (dbName.startsWith("damage_"))
        {
            return String.format(Locale.ROOT, "%,d 点", value);
        }
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * 秒 → 「N 小时 M 分 S 秒」。
     *
     * @param totalSeconds 总秒数
     * @return 可读时长
     */
    private static String duration(long totalSeconds)
    {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L)
        {
            return String.format(Locale.ROOT, "%d 小时 %d 分 %d 秒", hours, minutes, seconds);
        }
        if (minutes > 0L)
        {
            return String.format(Locale.ROOT, "%d 分 %d 秒", minutes, seconds);
        }
        return String.format(Locale.ROOT, "%d 秒", seconds);
    }
}
