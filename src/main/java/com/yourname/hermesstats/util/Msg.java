package com.yourname.hermesstats.util;

/**
 * 统一的文案前缀。
 *
 * <p>只有「失败 / 异常」类消息会带这个前缀；标题行本身已经含有
 * {@code ✦ Hermes-Stats ✦} 标记，再叠一层前缀会变成重复的
 * {@code [Hermes-Stats] ✦ Hermes-Stats ✦ ...}，所以正文行一律不加。</p>
 */
public final class Msg
{
    /** 失败消息前缀。 */
    public static final String PREFIX = "[Hermes-Stats] ";

    private Msg()
    {
    }

    /** 给失败消息加前缀。 */
    public static String prefixed(String text)
    {
        return PREFIX + text;
    }
}
