package com.yourname.hermesstats.cache;

import themislib.api.DataDomain;
import themislib.api.Metric;
import themislib.api.RankingDataProvider;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 排行榜结果缓存。
 *
 * <p>Themis 的 {@code top()} 是 O(玩家数 × 指标数) 的全量排序，翻页时每点一次
 * 「下一页」都会重复一次排序。这里按 {@code (指标, 数据域, limit)} 做一层 5 秒 TTL
 * 的短缓存：翻页来回切、多个玩家同时看同一个榜时都能命中。</p>
 *
 * <p>TTL 刻意取得很短（5 秒），保证榜单数据不会明显滞后；服务器停止时整体清空。</p>
 */
public final class TopCache
{
    /** 缓存有效期（毫秒）。 */
    private static final long TTL_MILLIS = 5_000L;

    /** 缓存表。 */
    private static final ConcurrentHashMap<Key, Entry> CACHE = new ConcurrentHashMap<>();

    /** 缓存键：指标 + 数据域 + 条数上限。 */
    private record Key(Metric metric, DataDomain domain, int limit)
    {
    }

    /** 缓存值：结果快照 + 过期时间戳。 */
    private record Entry(List<RankingDataProvider.RankedEntry> entries, long expireAt)
    {
    }

    private TopCache()
    {
    }

    /**
     * 取榜单（统一入口，所有 {@code top()} 调用都必须走这里）。
     *
     * @param metric   指标
     * @param domain   数据域
     * @param limit    条数上限（必须 > 0）
     * @param provider Themis 数据提供者
     * @return 榜单快照（不可变）
     */
    public static List<RankingDataProvider.RankedEntry> get(Metric metric,
                                                            DataDomain domain,
                                                            int limit,
                                                            RankingDataProvider provider)
    {
        if (metric == null || domain == null || provider == null || limit <= 0)
        {
            return List.of();
        }

        Key key = new Key(metric, domain, limit);
        long now = System.currentTimeMillis();

        Entry cached = CACHE.get(key);
        if (cached != null && cached.expireAt() > now)
        {
            return cached.entries();
        }

        List<RankingDataProvider.RankedEntry> fresh = provider.top(metric, domain, limit);
        if (fresh == null)
        {
            fresh = List.of();
        }
        List<RankingDataProvider.RankedEntry> snapshot = List.copyOf(fresh);
        CACHE.put(key, new Entry(snapshot, now + TTL_MILLIS));
        return snapshot;
    }

    /** 清空缓存（服务器停止时调用）。 */
    public static void clear()
    {
        CACHE.clear();
    }

    /**
     * 当前缓存条目数（调试用）。
     *
     * @return 条目数
     */
    public static int size()
    {
        return CACHE.size();
    }
}
