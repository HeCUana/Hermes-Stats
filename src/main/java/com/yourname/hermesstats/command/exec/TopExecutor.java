package com.yourname.hermesstats.command.exec;

import com.yourname.hermesstats.cache.TopCache;
import com.yourname.hermesstats.command.Category;
import com.yourname.hermesstats.command.HermesCommand;
import com.yourname.hermesstats.util.ChatRenderer;
import com.yourname.hermesstats.util.MetricDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import hermeslib.api.DataDomain;
import hermeslib.api.HermesApi;
import hermeslib.api.Metric;
import hermeslib.api.RankingDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * {@code /hermes top} 与 {@code /hermes fake top} 的执行器。
 *
 * <p>分页策略：</p>
 * <ul>
 *   <li>每页 {@link #PAGE_SIZE} 条；</li>
 *   <li>请求时多取 1 条（{@code page * PAGE_SIZE + 1}）来精确判断是否还有下一页；</li>
 *   <li>本页数据取 {@code subList((page-1)*PAGE_SIZE, min(offset+PAGE_SIZE, size))}。</li>
 * </ul>
 *
 * <p>所有 {@code top()} 调用都走 {@link TopCache}，翻页来回切不会反复触发全量排序。</p>
 */
public final class TopExecutor
{
    /** 每页条数。 */
    public static final int PAGE_SIZE = 10;

    private TopExecutor()
    {
    }

    /**
     * {@code /hermes top} 无参：提示选指标 + 分类按钮。
     *
     * @param source 命令来源
     * @param domain 数据域
     * @return 命令返回值
     */
    public static int executeMenu(CommandSourceStack source, DataDomain domain)
    {
        String path = HermesCommand.path(domain);

        ChatRenderer.sendHeader(source, "✦ Hermes-Stats ✦ 请选择要查看的指标");

        List<Component> buttons = new ArrayList<>();
        for (Category category : Category.values())
        {
            buttons.add(ChatRenderer.button(
                    category.displayName() + " (" + category.size() + ")",
                    "/" + path + " list " + category.id(),
                    category.displayName() + " · " + category.size() + " 项指标"));
        }
        ChatRenderer.sendNav(source, buttons);

        ChatRenderer.sendLine(source, "或直接输入：" + "/" + path + " top <指标名>");
        return 1;
    }

    /**
     * 渲染榜单。
     *
     * @param source   命令来源
     * @param domain   数据域
     * @param metricArg 指标参数（{@code dbName} 或枚举名，大小写不敏感）
     * @param page     页码（从 1 开始）
     * @return 命令返回值
     */
    public static int execute(CommandSourceStack source, DataDomain domain, String metricArg, int page)
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

        int safePage = Math.max(1, page);
        int offset = (safePage - 1) * PAGE_SIZE;

        // 多请求 1 条，用于精确判断是否还有下一页
        List<RankingDataProvider.RankedEntry> entries =
                TopCache.get(metric, domain, safePage * PAGE_SIZE + 1, provider);

        String path = HermesCommand.path(domain);
        String header = "✦ Hermes-Stats ✦ " + MetricDisplay.name(metric)
                + " · " + domainLabel(domain) + " · 第 " + safePage + " 页";

        // ---------------- 空页处理 ----------------
        if (entries.isEmpty())
        {
            ChatRenderer.sendHeader(source, header);
            ChatRenderer.sendError(source, "暂无数据");
            return 0;
        }
        if (entries.size() <= offset)
        {
            int maxPage = (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE;
            ChatRenderer.sendHeader(source, header);
            ChatRenderer.sendError(source, "第 " + safePage + " 页没有数据（有效页码范围：1 - " + maxPage + "）");
            return 0;
        }

        // ---------------- 本页数据 ----------------
        List<RankingDataProvider.RankedEntry> pageEntries =
                entries.subList(offset, Math.min(entries.size(), offset + PAGE_SIZE));
        boolean hasNext = entries.size() > offset + PAGE_SIZE;

        ChatRenderer.sendHeader(source, header);

        UUID self = selfUuid(source, provider);
        int rank = offset + 1;
        for (RankingDataProvider.RankedEntry entry : pageEntries)
        {
            boolean isSelf = self != null && self.equals(entry.uuid());
            ChatFormatting color = isSelf ? ChatRenderer.SELF_COLOR : ChatRenderer.BODY_COLOR;

            MutableComponent line = Component.literal("#" + rank + ". " + entry.name() + " - ")
                    .withStyle(color);
            line.append(Component.literal(MetricDisplay.format(metric, entry.value())).withStyle(color));
            if (isSelf)
            {
                line.withStyle(ChatFormatting.BOLD);
            }
            ChatRenderer.sendLine(source, line);
            rank++;
        }

        // ---------------- 底部导航 ----------------
        List<Component> nav = new ArrayList<>();
        if (safePage > 1)
        {
            nav.add(ChatRenderer.button("上一页",
                    "/" + path + " top " + metric.dbName() + " " + (safePage - 1),
                    "第 " + (safePage - 1) + " 页"));
        }
        if (hasNext)
        {
            nav.add(ChatRenderer.button("下一页",
                    "/" + path + " top " + metric.dbName() + " " + (safePage + 1),
                    "第 " + (safePage + 1) + " 页"));
        }

        Category category = Category.of(metric);
        nav.add(ChatRenderer.button("返回分类",
                "/" + path + " list " + category.id(),
                "返回 " + category.displayName() + " 类指标"));

        if (domain == DataDomain.FAKE)
        {
            nav.add(ChatRenderer.button("玩家榜",
                    "/" + HermesCommand.REAL_PATH + " top " + metric.dbName() + " " + safePage,
                    "切换到玩家榜（第 " + safePage + " 页）"));
        }
        else
        {
            nav.add(ChatRenderer.button("假人榜",
                    "/" + HermesCommand.FAKE_PATH + " top " + metric.dbName() + " " + safePage,
                    "切换到假人榜（第 " + safePage + " 页）"));
        }
        ChatRenderer.sendNav(source, nav);

        return 1;
    }

    /**
     * 数据域的中文标签。
     *
     * @param domain 数据域
     * @return {@code 玩家榜} 或 {@code 假人榜}
     */
    private static String domainLabel(DataDomain domain)
    {
        return domain == DataDomain.FAKE ? "假人榜" : "玩家榜";
    }

    /**
     * 取查看者自己的有效 UUID（用于在榜单里高亮自己）。
     *
     * @param source   命令来源
     * @param provider 数据提供者
     * @return UUID；命令控制台等无玩家场景返回 {@code null}
     */
    private static UUID selfUuid(CommandSourceStack source, RankingDataProvider provider)
    {
        ServerPlayer player = source.getPlayer();
        if (player == null)
        {
            return null;
        }
        return provider.effectiveUuid(player);
    }
}
