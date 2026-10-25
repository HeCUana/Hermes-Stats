package com.yourname.hermesstats.command.exec;

import com.yourname.hermesstats.util.ChatRenderer;
import hermeslib.api.HermesApi;
import hermeslib.api.ImportResult;
import hermeslib.api.RankingDataProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@code /hermes import} 的执行器：导入原版（Vanilla）统计数据。
 *
 * <p>仅 OP（权限等级 2 / GAMEMASTERS）可用。导入本身会读写文件、解析 JSON，
 * 在 Hermes Lib 内部还会等待主线程把结果刷进内存，因此<b>绝不能在主线程上直接调用</b>——
 * 这里用 {@link CompletableFuture#supplyAsync} 丢到工作线程执行，完成后再切回服务器主线程反馈。</p>
 *
 * <p>导入是<b>覆盖</b>语义（原版统计本身是累计值，累加会让数值翻倍），重复执行幂等。</p>
 */
public final class ImportExecutor
{
    /** 最多逐条展示的错误数量，超出的只给一个汇总。 */
    private static final int MAX_ERRORS = 10;

    private ImportExecutor()
    {
    }

    /**
     * 启动一次原版统计导入。
     *
     * @param source 命令来源
     * @return 命令返回值（1 = 已受理，导入在后台进行）
     */
    public static int execute(CommandSourceStack source)
    {
        RankingDataProvider provider = HermesApi.provider();
        if (provider == null)
        {
            ChatRenderer.sendError(source, "Hermes Lib 数据尚未就绪，请稍后再试");
            return 0;
        }

        // 黄色提示：导入是异步的，先告诉玩家已经开始了
        ChatRenderer.sendLine(source,
                Component.literal("开始导入原版统计数据...").withStyle(ChatFormatting.YELLOW));

        MinecraftServer server = source.getServer();
        // server 理论上不会为空；万一拿不到就退回当前线程，避免 NPE
        Executor mainThread = server != null ? server : Runnable::run;

        CompletableFuture.supplyAsync(provider::importVanillaStats)
                .exceptionally(throwable ->
                {
                    String message = throwable.getMessage() == null ? throwable.toString() : throwable.getMessage();
                    ChatRenderer.sendError(source, "导入失败：" + message);
                    return null;
                })
                .thenAcceptAsync(result -> report(source, result), mainThread);

        return 1;
    }

    /**
     * 输出导入结果（在服务器主线程调用）。
     *
     * @param source 命令来源
     * @param result 导入结果
     */
    private static void report(CommandSourceStack source, ImportResult result)
    {
        if (result == null)
        {
            return;
        }

        ChatRenderer.sendLine(source, "导入完成：处理 " + result.playersProcessed()
                + " 名玩家，写入 " + result.metricsImported()
                + " 条指标，跳过 " + result.playersSkipped() + " 个文件");

        List<String> errors = result.errors();
        if (errors.isEmpty())
        {
            return;
        }

        for (String error : errors.subList(0, Math.min(errors.size(), MAX_ERRORS)))
        {
            ChatRenderer.sendLine(source, Component.literal("· " + error).withStyle(ChatFormatting.GRAY));
        }
        if (errors.size() > MAX_ERRORS)
        {
            ChatRenderer.sendLine(source, "…… 其余 " + (errors.size() - MAX_ERRORS) + " 条错误已省略");
        }
    }
}
