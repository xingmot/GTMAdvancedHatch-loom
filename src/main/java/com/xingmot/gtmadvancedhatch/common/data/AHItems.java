package com.xingmot.gtmadvancedhatch.common.data;

import com.xingmot.gtmadvancedhatch.api.NetDataItemBehavior;
import com.xingmot.gtmadvancedhatch.common.AHRegistration;

import com.gregtechceu.gtceu.api.item.ComponentItem;

import static com.gregtechceu.gtceu.common.data.GTItems.attach;

import com.tterrag.registrate.util.entry.ItemEntry;

public class AHItems {

    static {
        AHRegistration.registrate.creativeModeTab(() -> AHTabs.BASE_TAB);
    }

    public static ItemEntry<ComponentItem> TOOL_NET_DATA_STICK = AHRegistration.registrate.item("net_data_stick", ComponentItem::create)
            .lang("Net Data Stick")
            .onRegister(attach(new NetDataItemBehavior()))
            .register();

    public static void init() {}
}
