package com.yourname.hermesstats.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * {@code category} 参数的补全。
 *
 * <p>候选值取 {@link Category#id()}，悬停提示显示该分类的指标数量。匹配大小写不敏感。</p>
 */
public final class CategorySuggestion
{
    /** Brigadier 补全提供器。 */
    public static final SuggestionProvider<CommandSourceStack> PROVIDER = (context, builder) ->
    {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (Category category : Category.values())
        {
            String id = category.id();
            if (!remaining.isEmpty() && !id.startsWith(remaining))
            {
                continue;
            }
            builder.suggest(id, Component.literal(category.displayName() + " · " + category.size() + " 项指标"));
        }
        return builder.buildFuture();
    };

    private CategorySuggestion()
    {
    }
}
