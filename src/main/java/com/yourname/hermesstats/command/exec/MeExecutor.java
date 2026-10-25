package com.yourname.hermesstats.command.exec;

import com.yourname.hermesstats.cache.TopCache;
import com.yourname.hermesstats.command.Category;
import com.yourname.hermesstats.command.HermesCommand;
import com.yourname.hermesstats.util.ChatRenderer;
import com.yourname.hermesstats.util.MetricDisplay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import hermeslib.api.DataDomain;
import hermeslib.api.HermesApi;
import hermeslib.api.Metric;
import hermeslib.api.RankingDataProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code /hermes me} 的执行器。
 *
 * <ul>
 *   <li>无参 —— 我的总览：只调用一次 {@code getAll(uuid, REAL)}，逐行打印指标值，
 *       <b>不调用 {@code top()}、不做任何排名扫描</b>；</li>
 *   <li>带指标 —— 我的单项排名：{@code top(metric, REAL, 1000)} 遍历找自己。</li>
 * </ul>
 */
public final class MeExecutor
{
    /** 查自己排名时扫描的名次上限。 */
    public static final int RANK_SCAN_LIMIT = 1000;

    private MeExecutor()
    {
    }

    /**
     * 渲染「我的总览」。
     *
     * @param source 命令来源
     * @return 命令返回值
     */
    public static int executeOverview(CommandSourceStack source)
    {
        RankingDataProvider provider = HermesApi.provider();
        if (provider == null)
        {
            ChatRenderer.sendError(source, "Hermes Lib 数据尚未就绪，请稍后再试");
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            ChatRenderer.sendError(source, "该命令需要由玩家执行");
            return 0;
        }

        UUID uuid = provider.effectiveUuid(player);

        // 只调一次 getAll；注意 getAll 只包含非零指标
        Map<Metric, Long> all = provider.getAll(uuid, DataDomain.REAL);

        ChatRenderer.sendHeader(source, "✦ Hermes-Stats ✦ 我的总览");

        if (all.isEmpty())
        {
            ChatRenderer.sendLine(source, "暂无统计数据");
        }
        else
        {
            // 按分类顺序输出，方便和 /hermes list 的结构对照
            for (Category category : Category.values())
            {
                for (Metric metric : category.metrics())
                {
                    Long value = all.get(metric);
                    if (value == null)
                    {
                        continue;
                    }
                    ChatRenderer.sendLine(source,
                            MetricDisplay.name(metric) + "：" + MetricDisplay.format(metric, value));
                }
            }
        }

        ChatRenderer.sendLine(source, "提示：用 /hermes me <指标> 查看单项排名");
        return 1;
    }

    /**
     * 渲染「我的单项排名」。
     *
     * @param source    命令来源
     * @param metricArg 指标参数（大小写不敏感）
     * @return 命令返回值
     */
    public static int executeMetric(CommandSourceStack source, String metricArg)
    {
        Metric metric = Metric.byName(metricArg).orElse(null);
        if (metric == null)
        {
            ChatRenderer.sendError(source, "未知指标：" + metricArg
                    + "（用 /hermes list 查看全部指标）");
            return 0;
        }

        RankingDataProvider provider = HermesApi.provider();
        if (provider == null)
        {
            ChatRenderer.sendError(source, "Hermes Lib 数据尚未就绪，请稍后再试");
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            ChatRenderer.sendError(source, "该命令需要由玩家执行");
            return 0;
        }

        UUID uuid = provider.effectiveUuid(player);
        long value = provider.getValue(uuid, DataDomain.REAL, metric);

        // 只在这一条命令里做排名扫描（带缓存），总览命令不碰 top()
        List<RankingDataProvider.RankedEntry> list =
                TopCache.get(metric, DataDomain.REAL, RANK_SCAN_LIMIT, provider);

        int rank = 0;
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).uuid().equals(uuid))
            {
                rank = i + 1;
                break;
            }
        }

        ChatRenderer.sendHeader(source, "✦ Hermes-Stats ✦ " + MetricDisplay.name(metric));
        ChatRenderer.sendLine(source, "数值：" + MetricDisplay.format(metric, value));

        if (rank > 0)
        {
            String total = list.size() >= RANK_SCAN_LIMIT
                    ? (RANK_SCAN_LIMIT + "+")
                    : String.valueOf(list.size());
            ChatRenderer.sendLine(source, "排名：#" + rank + " / 共 " + total + " 人");
        }
        else
        {
            ChatRenderer.sendLine(source, "未进入前 " + RANK_SCAN_LIMIT + " 名");
        }

        ChatRenderer.sendNav(source, List.of(
                ChatRenderer.button("查看完整榜",
                        "/" + HermesCommand.REAL_PATH + " top " + metric.dbName(),
                        "打开 " + MetricDisplay.name(metric) + " 完整榜单")));

        return 1;
    }
}
