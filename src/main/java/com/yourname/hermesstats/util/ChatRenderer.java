package com.yourname.hermesstats.util;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * 聊天栏渲染工具。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>{@link #sendHeader} —— 标题行，金色加粗，不加 {@link Msg#PREFIX}；</li>
 *   <li>{@link #sendLine} —— 正文行，灰色；</li>
 *   <li>{@link #sendNav} —— 导航行，多个按钮用 {@code | } 连接；</li>
 *   <li>{@link #sendError} —— 失败行，红色，统一带 {@link Msg#PREFIX}。</li>
 * </ul>
 *
 * <p>所有按钮都同时带 {@link ClickEvent}（点击执行命令）与
 * {@link HoverEvent}（悬停提示），保证「可点击文本必有悬停反馈」。</p>
 */
public final class ChatRenderer
{
    /** 标题颜色。 */
    public static final ChatFormatting HEADER_COLOR = ChatFormatting.GOLD;
    /** 正文颜色。 */
    public static final ChatFormatting BODY_COLOR = ChatFormatting.GRAY;
    /** 「自己」高亮颜色（金色）。 */
    public static final ChatFormatting SELF_COLOR = ChatFormatting.GOLD;
    /** 按钮颜色（绿色）。 */
    public static final ChatFormatting BUTTON_COLOR = ChatFormatting.GREEN;
    /** 失败消息颜色。 */
    public static final ChatFormatting ERROR_COLOR = ChatFormatting.RED;

    private ChatRenderer()
    {
    }

    /**
     * 发送标题行。
     *
     * @param source 命令来源
     * @param text   标题文本（一般形如 {@code ✦ Hermes-Stats ✦ 选择分类}）
     */
    public static void sendHeader(CommandSourceStack source, String text)
    {
        source.sendSuccess(() -> Component.literal(text)
                .withStyle(HEADER_COLOR)
                .withStyle(ChatFormatting.BOLD), false);
    }

    /**
     * 发送一行正文。
     *
     * @param source   命令来源
     * @param component 正文组件
     */
    public static void sendLine(CommandSourceStack source, Component component)
    {
        source.sendSuccess(component::copy, false);
    }

    /**
     * 发送一行灰色正文。
     *
     * @param source 命令来源
     * @param text   正文文本
     */
    public static void sendLine(CommandSourceStack source, String text)
    {
        sendLine(source, Component.literal(text).withStyle(BODY_COLOR));
    }

    /**
     * 构造一个可点击按钮。
     *
     * @param text     按钮文本
     * @param command  点击后执行的命令（带前导 {@code /}）
     * @param hoverText 悬停提示
     * @return 带 ClickEvent + HoverEvent 的按钮组件
     */
    public static MutableComponent button(String text, String command, String hoverText)
    {
        Style style = Style.EMPTY
                .withColor(BUTTON_COLOR)
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal(hoverText).withStyle(BODY_COLOR)));
        return Component.literal(text).withStyle(style);
    }

    /**
     * 发送一行导航按钮。
     *
     * @param source  命令来源
     * @param buttons 按钮列表（为空则不发送）
     */
    public static void sendNav(CommandSourceStack source, List<Component> buttons)
    {
        if (buttons == null || buttons.isEmpty())
        {
            return;
        }

        MutableComponent line = Component.empty();
        for (int i = 0; i < buttons.size(); i++)
        {
            if (i > 0)
            {
                line.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY));
            }
            line.append(buttons.get(i));
        }
        source.sendSuccess(() -> line, false);
    }

    /**
     * 发送一条失败消息（红色 + 统一前缀）。
     *
     * @param source 命令来源
     * @param text   失败原因
     */
    public static void sendError(CommandSourceStack source, String text)
    {
        source.sendFailure(Component.literal(Msg.prefixed(text)).withStyle(ERROR_COLOR));
    }
}
