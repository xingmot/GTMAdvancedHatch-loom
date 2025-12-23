package com.xingmot.gtmadvancedhatch.integration.gtmt.newmonitor;

import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GTMAdvancedHatch.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeCommonEventListener {

    @SubscribeEvent
    public static void onServerTickEvent(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.getServer().getTickCount() % 20 == 0) {
            EnergyStat.GlobalEnergyStat.values().forEach(EnergyStat::tick);
        }
        if (event.phase != TickEvent.Phase.END)
            EnergyStat.observed = false;
    }

    @SubscribeEvent
    public static void serverSetup(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ServerLevel serverLevel = level.getServer().getLevel(Level.OVERWORLD);
            if (serverLevel == null) return;
            EnergyStat.server = event.getLevel().getServer();
        }
    }
}
