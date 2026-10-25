package com.yourname.hermesstats;

import com.yourname.hermesstats.cache.TopCache;
import com.yourname.hermesstats.command.HermesCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hermes-Stats 入口。
 *
 * <p>本模组只做三件事：</p>
 * <ol>
 *   <li>注册 {@code /hermes} 命令树；</li>
 *   <li>调用 Themis Lib 的只读 API 取数据；</li>
 *   <li>在聊天栏分页渲染。</li>
 * </ol>
 *
 * <p>不采集数据、不写数据库、不对外提供 API。</p>
 */
public final class HermesStatsMod implements ModInitializer
{
    /** 模组 ID。 */
    public static final String MOD_ID = "hermes_stats";

    /** 日志记录器。 */
    public static final Logger LOGGER = LoggerFactory.getLogger("Hermes-Stats");

    @Override
    public void onInitialize()
    {
        // 注册命令树
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(HermesCommand.build()));

        // 服务器停止时清空榜单缓存（Themis 的 provider 此时也会被置空）
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TopCache.clear());

        LOGGER.info("[Hermes-Stats] 已初始化，命令：/hermes");
    }
}
