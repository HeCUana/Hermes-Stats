package com.yourname.hermesstats.command.exec;

import com.yourname.hermesstats.command.Category;
import com.yourname.hermesstats.command.HermesCommand;
import com.yourname.hermesstats.util.ChatRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import hermeslib.api.DataDomain;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /hermes} 与 {@code /hermes fake} 的入口菜单。
 *
 * <p>输出：标题 + 分类按钮行 + 快捷入口行。分类按钮把玩家带到「分类 → 指标 → 榜单」
 * 的第二级，页码与指标上下文在后续层级中始终保留。</p>
 */
public final class EntryExecutor
{
    private EntryExecutor()
    {
    }

    /**
     * 渲染入口菜单。
     *
     * @param source 命令来源
     * @param domain 数据域（{@code REAL} 玩家榜 / {@code FAKE} 假人榜）
     * @return 命令返回值
     */
    public static int execute(CommandSourceStack source, DataDomain domain)
    {
        String path = HermesCommand.path(domain);

        ChatRenderer.sendHeader(source, "✦ Hermes-Stats ✦ 选择分类");

        // ---- 分类按钮行 ----
        List<Component> categoryButtons = new ArrayList<>();
        for (Category category : Category.values())
        {
            categoryButtons.add(ChatRenderer.button(
                    category.displayName() + " (" + category.size() + ")",
                    "/" + path + " list " + category.id(),
                    category.displayName() + " · " + category.size() + " 项指标"));
        }
        ChatRenderer.sendNav(source, categoryButtons);

        // ---- 快捷入口行 ----
        List<Component> shortcuts = new ArrayList<>();
        shortcuts.add(ChatRenderer.button("全部指标",
                "/" + path + " list",
                "查看全部分类与指标"));
        if (domain == DataDomain.FAKE)
        {
            shortcuts.add(ChatRenderer.button("玩家榜",
                    "/" + HermesCommand.REAL_PATH,
                    "切换回玩家数据域"));
        }
        else
        {
            shortcuts.add(ChatRenderer.button("我的总览",
                    "/" + HermesCommand.REAL_PATH + " me",
                    "查看你个人的全部指标"));
            shortcuts.add(ChatRenderer.button("假人榜",
                    "/" + HermesCommand.FAKE_PATH,
                    "切换到假人数据域"));
        }
        ChatRenderer.sendNav(source, shortcuts);

        return 1;
    }
}
