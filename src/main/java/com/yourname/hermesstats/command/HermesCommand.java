package com.yourname.hermesstats.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yourname.hermesstats.command.exec.EntryExecutor;
import com.yourname.hermesstats.command.exec.ListExecutor;
import com.yourname.hermesstats.command.exec.MeExecutor;
import com.yourname.hermesstats.command.exec.TopExecutor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import hermeslib.api.DataDomain;

/**
 * {@code /hermes} 命令树。
 *
 * <pre>
 * /hermes                       入口菜单
 * /hermes list [<category>]      分类目录 / 分类内指标列表
 * /hermes top [<metric>] [<page>] 选指标 / 榜单（分页）
 * /hermes me [<metric>]          我的总览 / 我的单项排名
 * /hermes fake                  假人域入口（独立子树，domain 固定 FAKE）
 * /hermes fake list [<category>]
 * /hermes fake top [<metric>] [<page>]
 * </pre>
 *
 * <p><b>为什么假人榜用独立子树而不是 domain 位置参数？</b>
 * {@code /hermes top <metric> <domain>} 与 {@code /hermes top <metric> <page>} 在
 * Brigadier 里会产生歧义（第二个词到底是域还是页码）。把假人榜拆到 {@code /hermes fake}
 * 子树下后，两侧共用同一套节点构建器，域在构建期就固定死了，不存在歧义。</p>
 *
 * <p>命令树本身不区分 REAL / FAKE 的实现细节：{@link #buildListNode} 与
 * {@link #buildTopNode} 是共用的构建器，{@link DataDomain} 只是被当作参数传下去。</p>
 */
public final class HermesCommand
{
    /** 根命令名。 */
    public static final String ROOT = "hermes";
    /** 玩家域的命令路径（不含前导斜杠）。 */
    public static final String REAL_PATH = "hermes";
    /** 假人域的命令路径（不含前导斜杠）。 */
    public static final String FAKE_PATH = "hermes fake";

    private HermesCommand()
    {
    }

    /**
     * 构建整棵命令树。
     *
     * @return 根命令节点
     */
    public static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return Commands.literal(ROOT)
                // 零参数可用：直接输出入口菜单，不报用法错误
                .executes(context -> EntryExecutor.execute(context.getSource(), DataDomain.REAL))
                .then(buildListNode(DataDomain.REAL))
                .then(buildTopNode(DataDomain.REAL))
                .then(buildMeNode())
                // 假人子树：复用同一套 list / top 构建器
                .then(Commands.literal("fake")
                        .executes(context -> EntryExecutor.execute(context.getSource(), DataDomain.FAKE))
                        .then(buildListNode(DataDomain.FAKE))
                        .then(buildTopNode(DataDomain.FAKE)));
    }

    /**
     * 某个数据域对应的命令路径。
     *
     * @param domain 数据域
     * @return 例如 {@code hermes fake}
     */
    public static String path(DataDomain domain)
    {
        return domain == DataDomain.FAKE ? FAKE_PATH : REAL_PATH;
    }

    /**
     * {@code list} 节点（两个域共用）。
     *
     * @param domain 数据域
     * @return 节点
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildListNode(DataDomain domain)
    {
        return Commands.literal("list")
                // 无参：分类目录
                .executes(context -> ListExecutor.execute(context.getSource(), domain, null))
                .then(Commands.argument("category", StringArgumentType.word())
                        .suggests(CategorySuggestion.PROVIDER)
                        .executes(context -> ListExecutor.execute(context.getSource(), domain,
                                StringArgumentType.getString(context, "category"))));
    }

    /**
     * {@code top} 节点（两个域共用）。
     *
     * @param domain 数据域
     * @return 节点
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildTopNode(DataDomain domain)
    {
        return Commands.literal("top")
                // 无参：提示选指标 + 分类按钮
                .executes(context -> TopExecutor.executeMenu(context.getSource(), domain))
                .then(Commands.argument("metric", StringArgumentType.word())
                        .suggests(MetricSuggestion.PROVIDER)
                        // 只给指标 → 第 1 页
                        .executes(context -> TopExecutor.execute(context.getSource(), domain,
                                StringArgumentType.getString(context, "metric"), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                // 指标 + 页码 → 第 N 页
                                .executes(context -> TopExecutor.execute(context.getSource(), domain,
                                        StringArgumentType.getString(context, "metric"),
                                        IntegerArgumentType.getInteger(context, "page")))));
    }

    /**
     * {@code me} 节点（只有真人域，玩家自己的数据）。
     *
     * @return 节点
     */
    private static LiteralArgumentBuilder<CommandSourceStack> buildMeNode()
    {
        return Commands.literal("me")
                // 无参：我的总览（只调一次 getAll，不做排名扫描）
                .executes(context -> MeExecutor.executeOverview(context.getSource()))
                .then(Commands.argument("metric", StringArgumentType.word())
                        .suggests(MetricSuggestion.PROVIDER)
                        .executes(context -> MeExecutor.executeMetric(context.getSource(),
                                StringArgumentType.getString(context, "metric"))));
    }
}
