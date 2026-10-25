package com.yourname.hermesstats.command.exec;

import com.yourname.hermesstats.command.Category;
import com.yourname.hermesstats.command.HermesCommand;
import com.yourname.hermesstats.util.ChatRenderer;
import com.yourname.hermesstats.util.MetricDisplay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import hermeslib.api.DataDomain;
import hermeslib.api.Metric;

import java.util.List;
import java.util.Optional;

/**
 * {@code /hermes list} 与 {@code /hermes fake list} 的执行器。
 *
 * <ul>
 *   <li>无参 —— 分类目录，每行 {@code ▸ 分类名 (N 项)}，点击进入该分类；</li>
 *   <li>带分类 —— 该分类的指标列表，每行 {@code ▸ 中文名 (dbName) [查看榜]}。</li>
 * </ul>
 */
public final class ListExecutor
{
    private ListExecutor()
    {
    }

    /**
     * 渲染分类目录或分类内指标列表。
     *
     * @param source      命令来源
     * @param domain      数据域
     * @param categoryArg 分类参数；{@code null} 表示输出分类目录
     * @return 命令返回值
     */
    public static int execute(CommandSourceStack source, DataDomain domain, String categoryArg)
    {
        String path = HermesCommand.path(domain);

        // ---------------- 分类目录 ----------------
        if (categoryArg == null || categoryArg.isBlank())
        {
            ChatRenderer.sendHeader(source, "✦ Hermes-Stats ✦ 指标分类");

            for (Category category : Category.values())
            {
                ChatRenderer.sendLine(source, ChatRenderer.button(
                        "▸ " + category.displayName() + " (" + category.size() + " 项)",
                        "/" + path + " list " + category.id(),
                        category.displayName() + " · " + category.size() + " 项指标"));
            }

            ChatRenderer.sendNav(source, List.of(
                    ChatRenderer.button("返回主菜单",
                            "/" + HermesCommand.REAL_PATH,
                            "回到入口菜单")));
            return 1;
        }

        // ---------------- 分类内指标列表 ----------------
        Optional<Category> found = Category.byId(categoryArg);
        if (found.isEmpty())
        {
            ChatRenderer.sendError(source, "未知分类：" + categoryArg
                    + "（可用：activity / gather / combat / explore / social / other）");
            return 0;
        }

        Category category = found.get();
        ChatRenderer.sendHeader(source, "✦ Hermes-Stats ✦ " + category.displayName() + "类指标");

        for (Metric metric : category.metrics())
        {
            String command = "/" + path + " top " + metric.dbName();

            // 一行内同时给出指标信息与 [查看榜] 按钮：▸ 中文名 (dbName) [查看榜]
            MutableComponent row = ChatRenderer.button(
                    "▸ " + MetricDisplay.name(metric) + " (" + metric.dbName() + ")",
                    command,
                    MetricDisplay.name(metric) + " · " + category.displayName());
            row.append(Component.literal(" "));
            row.append(ChatRenderer.button(
                    "[查看榜]",
                    command,
                    "查看 " + MetricDisplay.name(metric) + " 榜单"));
            ChatRenderer.sendLine(source, row);
        }

        ChatRenderer.sendNav(source, List.of(
                ChatRenderer.button("返回分类目录",
                        "/" + path + " list",
                        "回到分类目录"),
                ChatRenderer.button("主菜单",
                        "/" + HermesCommand.REAL_PATH,
                        "回到入口菜单")));

        return 1;
    }
}
