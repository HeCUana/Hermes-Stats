package com.yourname.hermesstats.command;

import hermeslib.api.Metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 指标分类。
 *
 * <p>29（实际 27）个指标按语义分成 6 类，降低玩家在聊天栏里的认知负担。
 * 归类规则基于 {@link Metric#dbName()} 的前缀 / 关键字，并且在 Hermes Lib
 * 以后新增指标时自动落到 {@link #OTHER}，不会丢指标、也不会让分类列表为空。</p>
 *
 * <p>每个分类持有一份 {@link Metric} 列表（静态初始化时按枚举顺序填充）。</p>
 */
public enum Category
{
    /** 活跃：在线时长、跳跃。 */
    ACTIVITY("activity", "活跃"),
    /** 采集：方块与物品。 */
    GATHER("gather", "采集"),
    /** 战斗：击杀、死亡、伤害。 */
    COMBAT("combat", "战斗"),
    /** 探索：移动距离、钓鱼。 */
    EXPLORE("explore", "探索"),
    /** 社交：交易、繁殖、驯服、附魔。 */
    SOCIAL("social", "社交"),
    /** 其它：未能归类的指标（兜底）。 */
    OTHER("other", "其它");

    /** 指标 → 分类 反查表。 */
    private static final Map<Metric, Category> BY_METRIC = new EnumMap<>(Metric.class);
    /** 命令参数 id → 分类。 */
    private static final Map<String, Category> BY_ID = new LinkedHashMap<>();

    /** 该分类持有的指标列表。 */
    private final List<Metric> metrics = new ArrayList<>();

    /** 命令行使用的标识（小写 ASCII）。 */
    private final String id;
    /** 中文显示名。 */
    private final String displayName;

    Category(String id, String displayName)
    {
        this.id = id;
        this.displayName = displayName;
    }

    static
    {
        for (Category category : values())
        {
            BY_ID.put(category.id.toLowerCase(Locale.ROOT), category);
        }
        for (Metric metric : Metric.values())
        {
            BY_METRIC.put(metric, classify(metric));
        }
        // 归类完成后再把指标塞进各自的列表（枚举常量此时已构造完毕）
        for (Metric metric : Metric.values())
        {
            BY_METRIC.get(metric).metrics.add(metric);
        }
    }

    /**
     * 按 {@link Metric#dbName()} 判定指标归属的分类。
     *
     * <p>规则按顺序匹配，命中即返回；全部不命中时落到 {@link #OTHER}。</p>
     *
     * @param metric 指标
     * @return 所属分类（永远非 null）
     */
    private static Category classify(Metric metric)
    {
        String name = metric.dbName();

        if (name.equals("playtime_ticks") || name.equals("playtime_seconds") || name.equals("jumps"))
        {
            return ACTIVITY;
        }
        if (name.startsWith("blocks_") || name.startsWith("items_"))
        {
            return GATHER;
        }
        if (name.equals("mob_kills") || name.equals("player_kills") || name.equals("deaths")
                || name.equals("damage_dealt") || name.equals("damage_taken"))
        {
            return COMBAT;
        }
        if (name.startsWith("distance_") || name.equals("fish_caught"))
        {
            return EXPLORE;
        }
        if (name.equals("villager_trades") || name.startsWith("animals_") || name.equals("enchantments"))
        {
            return SOCIAL;
        }
        return OTHER;
    }

    /**
     * 反查：指标属于哪个分类。
     *
     * @param metric 指标
     * @return 所属分类；{@code metric} 为 null 时返回 {@link #OTHER}
     */
    public static Category of(Metric metric)
    {
        if (metric == null)
        {
            return OTHER;
        }
        return BY_METRIC.getOrDefault(metric, OTHER);
    }

    /**
     * 按命令参数 id 查找分类（大小写不敏感）。
     *
     * @param id 命令参数，例如 {@code combat}
     * @return 分类；不存在时返回空
     */
    public static Optional<Category> byId(String id)
    {
        if (id == null)
        {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id.trim().toLowerCase(Locale.ROOT)));
    }

    /**
     * 命令行标识。
     *
     * @return 例如 {@code combat}
     */
    public String id()
    {
        return id;
    }

    /**
     * 中文显示名。
     *
     * @return 例如 {@code 战斗}
     */
    public String displayName()
    {
        return displayName;
    }

    /**
     * 该分类下的指标（只读）。
     *
     * @return 指标列表
     */
    public List<Metric> metrics()
    {
        return Collections.unmodifiableList(metrics);
    }

    /**
     * 该分类下的指标数量。
     *
     * @return 数量
     */
    public int size()
    {
        return metrics.size();
    }
}
