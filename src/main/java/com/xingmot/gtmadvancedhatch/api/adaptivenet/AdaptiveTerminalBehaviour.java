package com.xingmot.gtmadvancedhatch.api.adaptivenet;

import com.xingmot.gtmadvancedhatch.common.data.AHMachines;
import com.xingmot.gtmadvancedhatch.common.data.recipe.AHCustomTags;

import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

/**
 * 自适配系统：
 * - 电网仓插槽过滤、支持魔改标签
 */
public class AdaptiveTerminalBehaviour {

    public static final List<ItemStack> ALL_NET_ENERGY_OUTPUT_HATCH_ITEM_LIST = AHMachines.ALL_NET_ENERGY_OUTPUT_HATCH.stream().filter(Objects::nonNull).map(MachineDefinition::asStack).toList();
    public static final List<ItemStack> ALL_NET_ENERGY_INPUT_HATCH_ITEM_LIST = AHMachines.ALL_NET_ENERGY_INPUT_HATCH.stream().filter(Objects::nonNull).map(MachineDefinition::asStack).toList();
    public static final List<ItemStack> ALL_NET_LASER_OUTPUT_HATCH_ITEM_LIST = AHMachines.ALL_NET_LASER_OUTPUT_HATCH.stream().filter(Objects::nonNull).map(MachineDefinition::asStack).toList();
    public static final List<ItemStack> ALL_NET_LASER_INPUT_HATCH_ITEM_LIST = AHMachines.ALL_NET_LASER_INPUT_HATCH.stream().filter(Objects::nonNull).map(MachineDefinition::asStack).toList();

    public static boolean isNetEnergyOutputHatch(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return hasItem(ALL_NET_ENERGY_OUTPUT_HATCH_ITEM_LIST, itemStack) || itemStack.is(AHCustomTags.ADAPTIVE_NET_ENERGY_OUT);
    }

    public static boolean isNetEnergyInputHatch(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return hasItem(ALL_NET_ENERGY_INPUT_HATCH_ITEM_LIST, itemStack) || itemStack.is(AHCustomTags.ADAPTIVE_NET_ENERGY_IN);
    }

    public static boolean isNetLaserOutputHatch(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return hasItem(ALL_NET_LASER_OUTPUT_HATCH_ITEM_LIST, itemStack) || itemStack.is(AHCustomTags.ADAPTIVE_NET_LASER_OUT);
    }

    public static boolean isNetLaserInputHatch(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return false;
        return hasItem(ALL_NET_LASER_INPUT_HATCH_ITEM_LIST, itemStack) || itemStack.is(AHCustomTags.ADAPTIVE_NET_LASER_IN);
    }

    public static boolean isAnyTag(ItemStack itemStack) {
        return itemStack.is(AHCustomTags.ADAPTIVE_NET_ENERGY_OUT) || itemStack.is(AHCustomTags.ADAPTIVE_NET_ENERGY_IN) || itemStack.is(AHCustomTags.ADAPTIVE_NET_LASER_OUT) || itemStack.is(AHCustomTags.ADAPTIVE_NET_LASER_IN);
    }

    private static boolean hasItem(List<ItemStack> itemList, ItemStack itemStack) {
        for (ItemStack item : itemList) {
            if (item.is(itemStack.getItem())) {
                return true;
            }
        }
        return false;
    }
}
