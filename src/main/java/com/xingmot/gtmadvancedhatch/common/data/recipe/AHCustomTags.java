package com.xingmot.gtmadvancedhatch.common.data.recipe;

import com.gregtechceu.gtceu.api.data.tag.TagUtil;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 添加自定义标签，方便魔改
 */
public class AHCustomTags {

    // 电网适配终端可被放入的物品的标签
    public static final TagKey<Item> ADAPTIVE_NET_ENERGY_OUT = TagUtil.createItemTag("adaptive_net/energy_out");
    public static final TagKey<Item> ADAPTIVE_NET_ENERGY_IN = TagUtil.createItemTag("adaptive_net/energy_in");
    public static final TagKey<Item> ADAPTIVE_NET_LASER_OUT = TagUtil.createItemTag("adaptive_net/laser_out");
    public static final TagKey<Item> ADAPTIVE_NET_LASER_IN = TagUtil.createItemTag("adaptive_net/laser_in");
}
