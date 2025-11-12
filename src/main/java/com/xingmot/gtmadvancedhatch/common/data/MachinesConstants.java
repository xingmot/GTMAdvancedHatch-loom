package com.xingmot.gtmadvancedhatch.common.data;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.item.ComponentItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.gregtechceu.gtceu.common.data.GTItems;
import com.hepdd.gtmthings.data.CustomItems;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

import static com.gregtechceu.gtceu.common.data.GTItems.*;

public final class MachinesConstants {

    private MachinesConstants() {}

    // region 调试用开关
    public static final boolean debugGUI = false;
    // endregion

    // region 常量
    public static final UUID UUID_ZERO = new UUID(0, 0);
    // 自用gui动作id，方便区分
    public static final int MOUSE_LEFT_CLICK_ACTION_ID = 1;
    public static final int MOUSE_RIGHT_CLICK_ACTION_ID = 2;
    public static final int MOUSE_MIDDLE_CLICK_ACTION_ID = 3;
    public static final int SCROLL_ACTION_ID = 65537;
    // endregion

    // region 公式方法
    // ULV:1*1 LV:2*3 MV:3*5 HV:4*7 EV:5*9 Luv:6*10 UV:7*10...
    public static int getLockItemOutputBusSlotRow(int tier) {
        return 1 + Math.min(9, 2 * tier);
    } // 行宽row 即列数

    public static int getLockItemOutputBusSlotCol(int tier) {
        return 1 + Math.min(9, tier);
    } // 列宽col 即行数

    public static int getLockItemOutputBusSlot(int tier) {
        return getLockItemOutputBusSlotRow(tier) * getLockItemOutputBusSlotCol(tier);
    }

    // 电仓容量计算
    public static long getMaxCapacity(IO io, long amps, long voltage) {
        long capacity = Long.MAX_VALUE - 1;
        if (io == IO.OUT) {
            if (amps < Long.MAX_VALUE / voltage / 128) {
                capacity = voltage * 64L * amps;
            }
        } else {
            if (amps < Long.MAX_VALUE / voltage / 32) {
                capacity = voltage * 16L * amps;
            }
        }
        return capacity;
    }
    // endregion

    // region GTMThings
    public static List<ItemEntry<ComponentItem>> WIRELESS_ENERGY_RECEIVE_COVER = List.of(
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_LV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_MV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_HV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_EV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_IV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_LUV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_ZPM,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UHV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UEV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UIV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UXV,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_OPV);

    public static List<ItemEntry<ComponentItem>> WIRELESS_ENERGY_RECEIVE_COVER_4A = List.of(
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_LV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_MV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_HV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_EV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_IV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_LUV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_ZPM_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UHV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UEV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UIV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_UXV_4A,
            CustomItems.WIRELESS_ENERGY_RECEIVE_COVER_OPV_4A);
    // endregion
}
