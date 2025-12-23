package com.xingmot.gtmadvancedhatch;

import com.xingmot.gtmadvancedhatch.common.AHRegistration;
import com.xingmot.gtmadvancedhatch.common.data.AHItems;
import com.xingmot.gtmadvancedhatch.common.data.AHMachines;
import com.xingmot.gtmadvancedhatch.common.data.AHTabs;
import com.xingmot.gtmadvancedhatch.config.AHConfig;
import com.xingmot.gtmadvancedhatch.integration.buildinggadgets.BuildingGadgetRegistration;
import com.xingmot.gtmadvancedhatch.integration.gtlcore.ExGTLMachines;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import static net.minecraft.resources.ResourceLocation.tryBuild;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(GTMAdvancedHatch.MODID)
public class GTMAdvancedHatch {

    public static final String MODID = "gtmadvancedhatch";
    public static final String NAME = "GTMAdvancedHatch";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation id(String name) {
        return tryBuild(MODID, FormattingUtil.toLowerCaseUnder(name));
    }

    @SuppressWarnings("removal")
    public GTMAdvancedHatch() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        AHRegistration.registrate.registerEventListeners(bus);
        // 注册GT机器
        bus.addGenericListener(MachineDefinition.class, this::registerMachines);
        // building gadget
        if (LDLib.isModLoaded("buildinggadgets2"))
            BuildingGadgetRegistration.init();
        // 注册配置
        AHConfig.init();
        bus.register(this);
    }

    public static boolean isClientSide() {
        return FMLEnvironment.dist.isClient();
    }

    @SubscribeEvent
    public void registerMachines(GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        AHMachines.init();
        AHItems.init();
        AHTabs.init();
        ExGTLMachines.init();
    }
}
