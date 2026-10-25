package com.yourname.hermesstats.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yourname.hermesstats.util.MetricDisplay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import hermeslib.api.Metric;

import java.util.Locale;

/**
 * {@code metric} 参数的补全。
 *
 * <p>候选值取 {@link Metric#dbName()}（同时也是 Themis 的数据库取值），悬停提示为
 * 「中文名 · 分类名」。匹配大小写不敏感。</p>
 */
public final class MetricSuggestion
{
    /** Brigadier 补全提供器。 */
    public static final SuggestionProvider<CommandSourceStack> PROVIDER = (context, builder) ->
    {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (Metric metric : Metric.values())
        {
            String dbName = metric.dbName();
            if (!remaining.isEmpty() && !dbName.startsWith(remaining))
            {
                continue;
            }
            builder.suggest(dbName, Component.literal(
                    MetricDisplay.name(metric) + " · " + Category.of(metric).displayName()));
        }
        return builder.buildFuture();
    };

    private MetricSuggestion()
    {
    }
}
