package com.xingmot.gtmadvancedhatch.common.data;

import cn.qiuye.gtmoremachine.GTmm;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;
import com.xingmot.gtmadvancedhatch.common.AHRegistration;
import com.xingmot.gtmadvancedhatch.common.machines.*;
import com.xingmot.gtmadvancedhatch.common.machines.adaptivehatch.AdaptiveNetEnergyHatchPartMachine;
import com.xingmot.gtmadvancedhatch.common.machines.adaptivehatch.AdaptiveNetEnergyTerminal;
import com.xingmot.gtmadvancedhatch.config.AHConfig;
import com.xingmot.gtmadvancedhatch.util.AHFormattingUtil;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.registry.registrate.MachineBuilder;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.BiFunction;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.*;
import static com.xingmot.gtmadvancedhatch.common.data.MachinesConstants.getLockItemOutputBusSlot;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AHMachines {

    static {
        AHRegistration.registrate.creativeModeTab(() -> AHTabs.BASE_TAB);
    }

    public static final int[] ALL_TIERS = GTValues.tiersBetween(ULV, GTCEuAPI.isHighTier() ? MAX : UV);
    public static final int[] NET_TIERS = GTValues.tiersBetween(LV, MAX);
    public static final int[] NET_HIGH_TIERS = GTValues.tiersBetween(EV, MAX); // 64A 电网能源仓 + 激光仓
    public static final int[] NET_HIGH_TIERS2 = GTValues.tiersBetween(LuV, MAX); // 大安培 电网能源仓
    // region 》物品仓室
    public static final MachineDefinition[] LOCK_ITEM_OUTPUT_BUS = registerTieredMachines("lock_item_output_bus",
            LockItemOutputBus::new,
            (tier, builder) -> builder.langValue(VNF[tier] + " Output Bus")
                    .rotationState(RotationState.ALL)
                    .abilities(PartAbility.EXPORT_ITEMS)
                    .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                    .colorOverlayTieredHullModel(OVERLAY_ITEM_HATCH_OUTPUT,"overlay_pipe", "overlay_pipe_out_emissive")
                    .tooltips(Component.translatable("gtmadvancedhatch.machine.lock_item_output.tooltip"),
                            Component.translatable("gtmadvancedhatch.machine.lock_item_output.tooltip2"),
                            Component.translatable("gtceu.machine.item_bus.export.tooltip"),
                            Component.empty().append(Component.translatable("gtmadvancedhatch.machine.item_storage_slot_capacity").withStyle(ChatFormatting.GOLD))
                                    .append(Component.literal("%d (x%d)".formatted(LockItemOutputBus.getLockItemOutputBusSlotLimit(tier),
                                            LockItemOutputBus.getLockItemOutputBusSlotLimit(tier) / 64))),
                            Component.translatable("gtceu.universal.tooltip.item_storage_capacity",
                                    getLockItemOutputBusSlot(tier)))
                    .register(),
            GTValues.tiersBetween(ULV, GTCEuAPI.isHighTier() ? MAX : UV));
    // 可配置总线
    public static final MachineDefinition[] CONFIGURABLE_ITEM_INPUT_BUS_4X = registerConfigurableItemBuses("configurable_item_bus",
            IO.IN, 4, ALL_TIERS, PartAbility.IMPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_ITEM_INPUT_BUS_8X = registerConfigurableItemBuses("configurable_item_bus",
            IO.IN, 8, NET_HIGH_TIERS, PartAbility.IMPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_ITEM_INPUT_BUS_16X = registerConfigurableItemBuses("configurable_item_bus",
            IO.IN, 16, NET_HIGH_TIERS, PartAbility.IMPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_ITEM_OUTPUT_BUS_4X = registerConfigurableItemBuses("configurable_item_bus",
            IO.OUT, 4, ALL_TIERS, PartAbility.EXPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_ITEM_OUTPUT_BUS_8X = registerConfigurableItemBuses("configurable_item_bus",
            IO.OUT, 8, NET_HIGH_TIERS, PartAbility.EXPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_ITEM_OUTPUT_BUS_16X = registerConfigurableItemBuses("configurable_item_bus",
            IO.OUT, 16, NET_HIGH_TIERS, PartAbility.EXPORT_ITEMS);

    // endregion
    // region 》流体仓室
    public static final MachineDefinition[] CONFIGURABLE_FLUID_HATCH_IMPORT_1X = registerConfigurableFluidHatches("configurable_fluid_hatch",
            IO.IN, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 1,
            ALL_TIERS, PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_1X);
    public static final MachineDefinition[] CONFIGURABLE_FLUID_HATCH_IMPORT_8X = registerConfigurableFluidHatches("configurable_fluid_hatch",
            IO.IN, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 8,
            NET_HIGH_TIERS, PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_1X);
    public static final MachineDefinition[] CONFIGURABLE_FLUID_HATCH_IMPORT_16X = registerConfigurableFluidHatches("configurable_fluid_hatch",
            IO.IN, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 16,
            NET_HIGH_TIERS, PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_FLUIDS_1X);
    public static final MachineDefinition[] CONFIGURABLE_FLUID_HATCH_EXPORT_1X = registerConfigurableFluidHatches("configurable_fluid_hatch",
            IO.OUT, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 1,
            ALL_TIERS, PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_FLUIDS_1X);
    public static final MachineDefinition[] CONFIGURABLE_FLUID_HATCH_EXPORT_8X = registerConfigurableFluidHatches("configurable_fluid_hatch",
            IO.OUT, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 8,
            NET_HIGH_TIERS, PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_FLUIDS_1X);
    public static final MachineDefinition[] CONFIGURABLE_FLUID_HATCH_EXPORT_16X = registerConfigurableFluidHatches("configurable_fluid_hatch",
            IO.OUT, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X, 16,
            NET_HIGH_TIERS, PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_FLUIDS_1X);

    // endregion
    // 可配置总成
    public static final MachineDefinition[] CONFIGURABLE_DUAL_HATCH_IMPORT_1P = registerConfigurableDualHatches("configurable_dual_hatch",
            IO.IN, 1, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X,
            NET_HIGH_TIERS, PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_DUAL_HATCH_EXPORT_1P = registerConfigurableDualHatches("configurable_dual_hatch",
            IO.OUT, 1, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X,
            NET_HIGH_TIERS, PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_DUAL_HATCH_IMPORT_16P = registerConfigurableDualHatches("configurable_dual_hatch",
            IO.IN, 16, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X,
            NET_HIGH_TIERS, PartAbility.IMPORT_FLUIDS, PartAbility.IMPORT_ITEMS);
    public static final MachineDefinition[] CONFIGURABLE_DUAL_HATCH_EXPORT_16P = registerConfigurableDualHatches("configurable_dual_hatch",
            IO.OUT, 16, FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X,
            NET_HIGH_TIERS, PartAbility.EXPORT_FLUIDS, PartAbility.EXPORT_ITEMS);

    // region 》能源仓室
    public static final Set<MachineDefinition> ALL_NET_ENERGY_OUTPUT_HATCH = new HashSet<>();
    public static final Set<MachineDefinition> ALL_NET_ENERGY_INPUT_HATCH = new HashSet<>();
    public static final Set<MachineDefinition> ALL_NET_LASER_OUTPUT_HATCH = new HashSet<>();
    public static final Set<MachineDefinition> ALL_NET_LASER_INPUT_HATCH = new HashSet<>();
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH = registerNetEnergyHatch(IO.IN, 2,
            PartAbility.INPUT_ENERGY, NET_TIERS);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_4A = registerNetEnergyHatch(IO.IN, 4,
            PartAbility.INPUT_ENERGY, NET_TIERS);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_16A = registerNetEnergyHatch(IO.IN, 16,
            PartAbility.INPUT_ENERGY, NET_TIERS);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_64A = registerNetEnergyHatch(IO.IN, 64,
            PartAbility.INPUT_ENERGY, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH = registerNetEnergyHatch(IO.OUT, 2,
            PartAbility.OUTPUT_ENERGY, NET_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_4A = registerNetEnergyHatch(IO.OUT, 4,
            PartAbility.OUTPUT_ENERGY, NET_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_16A = registerNetEnergyHatch(IO.OUT, 16,
            PartAbility.OUTPUT_ENERGY, NET_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_64A = registerNetEnergyHatch(IO.OUT, 64,
            PartAbility.OUTPUT_ENERGY, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_16384A = registerNetEnergyHatch(IO.IN, 16384,
            PartAbility.INPUT_ENERGY, NET_HIGH_TIERS2);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_65536A = registerNetEnergyHatch(IO.IN, 65536,
            PartAbility.INPUT_ENERGY, NET_HIGH_TIERS2);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_16384A = registerNetEnergyHatch(IO.OUT, 16384,
            PartAbility.OUTPUT_ENERGY, NET_HIGH_TIERS2);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_65536A = registerNetEnergyHatch(IO.OUT, 65536,
            PartAbility.OUTPUT_ENERGY, NET_HIGH_TIERS2);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_256A = registerNetLaserHatch(IO.IN, 256,
            PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_1024A = registerNetLaserHatch(IO.IN, 1024,
            PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_INPUT_HATCH_4096A = registerNetLaserHatch(IO.IN, 4096,
            PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_LASER_INPUT_HATCH_16384A = registerNetLaserHatch(IO.IN, 16384,
            PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_LASER_INPUT_HATCH_65536A = registerNetLaserHatch(IO.IN, 65536,
            PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_256A = registerNetLaserHatch(IO.OUT, 256,
            PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_1024A = registerNetLaserHatch(IO.OUT, 1024,
            PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_4096A = registerNetLaserHatch(IO.OUT, 4096,
            PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_LASER_OUTPUT_HATCH_16384A = registerNetLaserHatch(IO.OUT, 16384,
            PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static MachineDefinition[] NET_LASER_OUTPUT_HATCH_65536A = registerNetLaserHatch(IO.OUT, 65536,
            PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    // region 电网适配系统
    public static MachineDefinition ADAPTIVE_NET_ENERGY_INPUT_HATCH = AHRegistration.registrate.machine(
            "adaptive_net_energy_input_hatch", holder -> new AdaptiveNetEnergyHatchPartMachine(holder, IO.IN,false))
            .rotationState(RotationState.ALL)
            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
            .overlayTieredHullModel(GTmm.id("block/machine/part/energy_input_hatch"))
            .abilities(PartAbility.INPUT_ENERGY)
            .tier(14)
            .register();
    public static MachineDefinition ADAPTIVE_NET_ENERGY_OUTPUT_HATCH = AHRegistration.registrate.machine(
            "adaptive_net_energy_output_hatch", holder -> new AdaptiveNetEnergyHatchPartMachine(holder, IO.OUT,false))
            .rotationState(RotationState.ALL)
            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
            .overlayTieredHullModel(GTmm.id("block/machine/part/energy_output_hatch"))
            .abilities(PartAbility.OUTPUT_ENERGY)
            .tier(14)
            .register();
    public static MachineDefinition ADAPTIVE_NET_LASER_INPUT_HATCH = AHRegistration.registrate.machine(
            "adaptive_net_laser_target_hatch", holder -> new AdaptiveNetEnergyHatchPartMachine(holder, IO.IN,true))
            .rotationState(RotationState.ALL)
            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
            .overlayTieredHullModel(GTmm.id("block/machine/part/laser_target_hatch"))
            .abilities(PartAbility.INPUT_LASER)
            .tier(14)
            .register();
    public static MachineDefinition ADAPTIVE_NET_LASER_OUTPUT_HATCH = AHRegistration.registrate.machine(
            "adaptive_net_laser_source_hatch", holder -> new AdaptiveNetEnergyHatchPartMachine(holder, IO.OUT,true))
            .rotationState(RotationState.ALL)
            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
            .overlayTieredHullModel(GTmm.id("block/machine/part/laser_target_hatch"))
            .abilities(PartAbility.OUTPUT_LASER)
            .tier(14)
            .register();
    public static MachineDefinition ADAPTIVE_NET_ENERGY_TERMINAL = AHRegistration.registrate.machine(
            "adaptive_net_energy_terminal", AdaptiveNetEnergyTerminal::new)
            .rotationState(RotationState.NON_Y_AXIS)
            .workableTieredHullModel(GTmm.id("block/machines/wireless_monitor"))
            .tier(14)
            .register();
    // endregion

    public static MachineDefinition[] registerTieredMachines(String name, BiFunction<IMachineBlockEntity, Integer, MetaMachine> factory, BiFunction<Integer, MachineBuilder<MachineDefinition>, MachineDefinition> builder, int... tiers) {
        MachineDefinition[] definitions = new MachineDefinition[GTValues.TIER_COUNT];
        for (int tier : tiers) {
            var register = AHRegistration.registrate.machine(GTValues.VN[tier].toLowerCase(Locale.ROOT) + "_" + name,
                    holder -> factory.apply(holder, tier))
                    .tier(tier);
            definitions[tier] = builder.apply(tier, register);
        }
        return definitions;
    }

    public static MachineDefinition[] registerConfigurableItemBuses(String name, IO io, int slots,
                                                                    int[] tiers, PartAbility... abilities) {
        var multi = (slots == 1 ? "" : "_%dx".formatted(slots));
        final ResourceLocation overlay = GTCEu.id("block/overlay/machine/"+ (io == IO.IN? OVERLAY_ITEM_HATCH_INPUT:OVERLAY_ITEM_HATCH_OUTPUT));
        final ResourceLocation pipeOverlay = GTCEu.id("block/overlay/machine/overlay_pipe");
        final ResourceLocation emissiveOverlay = GTCEu.id("block/overlay/machine/"+ (io == IO.IN? "overlay_pipe_in_emissive":"overlay_pipe_out_emissive"));
        return registerTieredMachines(name + (io == IO.IN ? "_input" : "_output") + multi,
                (holder, tier) -> new ConfigurableItemBusPartMachine(holder, tier, io, slots),
                (tier, builder) -> {
                    builder.rotationState(RotationState.ALL)
                            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                            .colorOverlayTieredHullModel(overlay, pipeOverlay, emissiveOverlay)
                            .abilities(abilities)
                            .tooltipBuilder(
                                    (itemStack, components) -> {
                                        if (tier == MAX && slots == 16) {
                                            components.add(AHFormattingUtil.getRainbowScrollComponent(Component.translatable("gtmadvancedhatch.machine.great_job.tooltip2")
                                                    .getString(), AHFormattingUtil.RainbowSpeed.FAST2, false)
                                                    .withStyle(ChatFormatting.BOLD));
                                        }
                                        components.addAll(List.of(
                                                Component.translatable("gtceu.machine.fluid_hatch." + (io == IO.IN ? "import" : "export") + ".tooltip"),
                                                Component.translatable("gtmadvancedhatch.machine.configurable_hatch.tooltip"),
                                                Component.translatable("gtceu.machine.item_bus.export.tooltip"),
                                                Component.empty().append(Component.translatable("gtmadvancedhatch.machine.item_storage_slot_capacity").withStyle(ChatFormatting.GOLD))
                                                        .append(Component.literal("%s (x%d)".formatted(
                                                                AHFormattingUtil.formatLongToShort(ConfigurableItemBusPartMachine.getSlotCapacity(tier), 10240),
                                                                ConfigurableItemBusPartMachine.getSlotCapacity(tier) / 64))),
                                                Component.translatable("gtceu.universal.tooltip.item_storage_capacity", slots)));
                                    });
                    return builder.register();
                }, tiers);
    }

    public static MachineDefinition[] registerConfigurableFluidHatches(String name, IO io, int initialCapacity, int slots,
                                                                       int[] tiers, PartAbility... abilities) {
        var multi = (slots == 1 ? "" : "_%dx".formatted(slots));
        final ResourceLocation overlay = GTCEu.id("block/overlay/machine/"+ (io == IO.IN? OVERLAY_FLUID_HATCH_INPUT:OVERLAY_FLUID_HATCH_OUTPUT));
        final ResourceLocation pipeOverlay;
        if (slots >= 9) {
            pipeOverlay = GTCEu.id("block/overlay/machine/overlay_pipe_9x");
        } else if (slots >= 4) {
            pipeOverlay = GTCEu.id("block/overlay/machine/overlay_pipe_4x");
        } else {
            pipeOverlay = null;
        }
        final ResourceLocation emissiveOverlay = GTCEu.id("block/overlay/machine/"+ (io == IO.IN? "overlay_pipe_in_emissive":"overlay_pipe_out_emissive"));
        return registerTieredMachines(name + (io == IO.IN ? "_input" : "_output") + multi,
                (holder, tier) -> new ConfigurableFluidHatchPartMachine(holder, tier, io, initialCapacity, slots),
                (tier, builder) -> {
                    builder.rotationState(RotationState.ALL)
                            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                            .colorOverlayTieredHullModel(overlay,pipeOverlay, emissiveOverlay)
                            .abilities(abilities)
                            .tooltipBuilder(
                                    (itemStack, components) -> {
                                        if (tier == MAX && slots == 16) {
                                            components.add(AHFormattingUtil.getRainbowScrollComponent(Component.translatable("gtmadvancedhatch.machine.great_job.tooltip2")
                                                    .getString(), AHFormattingUtil.RainbowSpeed.FAST2, false)
                                                    .withStyle(ChatFormatting.BOLD));
                                        }
                                        components.addAll(List.of(
                                                Component.translatable("gtceu.machine.fluid_hatch." + (io == IO.IN ? "import" : "export") + ".tooltip"),
                                                Component.translatable("gtmadvancedhatch.machine.configurable_hatch.tooltip"),
                                                Component.translatable("gtmadvancedhatch.machine.configurable_fluid_hatch.tooltip.0")));
                                        if (slots == 1)
                                            components.add(Component.translatable("gtmadvancedhatch.machine.fluid_storage_capacity.tooltip",
                                                    AHFormattingUtil.formatLongBucketsToShort(ConfigurableFluidHatchPartMachine.getTankCapacity(initialCapacity, tier), 1024000)));
                                        else components.add(Component.translatable("gtmadvancedhatch.machine.fluid_storage_capacity_multi.tooltip",
                                                slots, AHFormattingUtil.formatLongBucketsToShort(ConfigurableFluidHatchPartMachine.getTankCapacity(initialCapacity, tier), 1024000)));
                                    });

                    return builder.register();
                },
                tiers);
    }

    public static MachineDefinition[] registerConfigurableDualHatches(String name, IO io, int page, int initialCapacity,
                                                                      int[] tiers, PartAbility... abilities) {
        var multi = (page == 1 ? "" : "_%dp".formatted(page));
        var renderPath = "block/machine/part/dual_hatch." + (io == IO.IN ? "import" : "export");
        return registerTieredMachines(name + (io == IO.IN ? "_input" : "_output") + multi,
                (holder, tier) -> new ConfigurableDualHatchPartMachine(holder, tier, io, page, initialCapacity),
                (tier, builder) -> {
                    builder.rotationState(RotationState.ALL)
                            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                            .workableTieredHullModel(GTCEu.id(renderPath))
                            .abilities(abilities)
                            .tooltipBuilder(
                                    (itemStack, components) -> {
                                        if (tier == MAX && page == 16) {
                                            components.add(AHFormattingUtil.getRainbowScrollComponent(Component.translatable("gtmadvancedhatch.machine.great_job.tooltip3")
                                                    .getString(), AHFormattingUtil.RainbowSpeed.FAST2, false)
                                                    .withStyle(ChatFormatting.BOLD));
                                        }
                                        components.addAll(List.of(
                                                Component.translatable("gtceu.machine.dual_hatch." + (io == IO.IN ? "import" : "export") + ".tooltip"),
                                                Component.translatable("gtmadvancedhatch.machine.configurable_hatch.tooltip"),
                                                Component.translatable("gtmadvancedhatch.machine.configurable_fluid_hatch.tooltip.0"),
                                                Component.translatable("gtmadvancedhatch.machine.fluid_storage_capacity_multi.tooltip",
                                                        page * 8, AHFormattingUtil.formatLongBucketsToShort(ConfigurableFluidHatchPartMachine.getTankCapacity(initialCapacity, tier), 1024000)),
                                                Component.empty().append(Component.translatable("gtmadvancedhatch.machine.item_storage_slot_capacity").withStyle(ChatFormatting.GOLD))
                                                        .append(Component.literal("%s (x%d)".formatted(
                                                                AHFormattingUtil.formatLongToShort(ConfigurableItemBusPartMachine.getSlotCapacity(tier), 10240),
                                                                ConfigurableItemBusPartMachine.getSlotCapacity(tier) / 64))),
                                                Component.translatable("gtceu.universal.tooltip.item_storage_capacity", page * 8)));
                                    });

                    return builder.register();
                },
                tiers);
    }

    public static MachineDefinition[] registerNetEnergyHatch(IO io, int amperage, PartAbility ability, int[] tiers) {
        var name = io == IO.IN ? "input" : "output";
        String finalRender = getRender(amperage);
        MachineDefinition[] energyHatches = registerTieredMachines(amperage + "a_net_energy_" + name + "_hatch",
                (holder, tier) -> new NetEnergyHatchPartMachine(holder, tier, io, amperage,false),
                (tier, builder) -> builder
                        .langValue(VNF[tier] + (io == IO.IN ? " Energy Hatch" : " Dynamo Hatch"))
                        .rotationState(RotationState.ALL)
                        .abilities(ability)
                        .tooltips(Component.translatable(GTMAdvancedHatch.MODID + ".machine.net_energy_hatch." + name + ".tooltip"),
                                Component.translatable(GTmm.MOD_ID + ".machine.energy_hatch." + name + ".tooltip"),
                                Component.translatable(GTmm.MOD_ID + ".machine.wireless_energy_hatch." + name + ".tooltip"))
                        .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                        .overlayTieredHullModel(GTmm.id("block/machine/part/" + finalRender))
                        .register(),
                tiers);
        if (io == IO.IN) {
            Collections.addAll(ALL_NET_ENERGY_INPUT_HATCH, energyHatches);
        } else {
            Collections.addAll(ALL_NET_ENERGY_OUTPUT_HATCH, energyHatches);
        }
        return energyHatches;
    }

    public static MachineDefinition[] registerNetLaserHatch(IO io, int amperage, PartAbility ability, int[] tiers) {
        var name = io == IO.IN ? "target" : "source";
        String finalRender = getRender(amperage);
        MachineDefinition[] laserHatches = registerTieredMachines(amperage + "a_net_laser_" + name + "_hatch",
                (holder, tier) -> new NetEnergyHatchPartMachine(holder, tier, io, amperage,true), (tier, builder) -> {
                    MachineBuilder<MachineDefinition> machineBuilder = builder.langValue(VNF[tier] + " " + FormattingUtil.formatNumbers(amperage) + "A Laser " + FormattingUtil.toEnglishName(name) + " Hatch")
                            .rotationState(RotationState.ALL)
                            .abilities(ability)
                            .overlayTieredHullModel(GTmm.id("block/machine/part/" + finalRender))
                            .modelProperty(GTMachineModelProperties.IS_FORMED, false)
                            /* 彩色滚动字参考这里 */
                            .tooltipBuilder(
                                    (itemStack, components) -> {
                                        if (amperage == 2147483647) {
                                            components.add(AHFormattingUtil.getRainbowScrollComponent(Component.translatable("gtmadvancedhatch.machine.great_job.tooltip")
                                                    .getString(), AHFormattingUtil.RainbowSpeed.FAST2, false)
                                                    .withStyle(ChatFormatting.BOLD));
                                        }
                                        components.addAll(List.of(
                                                Component.translatable(GTMAdvancedHatch.MODID + ".machine.net_energy_hatch." + name + ".tooltip"),
                                                Component.translatable(GTmm.MOD_ID + ".machine.energy_hatch." + name + ".tooltip"),
                                                Component.translatable(GTmm.MOD_ID + ".machine.wireless_energy_hatch." + name + ".tooltip")));
                                        if (amperage >= 16777216 && !LDLib.isModLoaded("gtlcore") && AHConfig.INSTANCE.isDisplayNoFixCrashWarning)
                                            components.add(Component.translatable(GTMAdvancedHatch.MODID + ".machine.no_fix_crash_warning.tooltip"));
                                    });
                    return machineBuilder.register();
                }, tiers);
        if (io == IO.IN) {
            Collections.addAll(ALL_NET_LASER_INPUT_HATCH, laserHatches);
        } else {
            Collections.addAll(ALL_NET_LASER_OUTPUT_HATCH, laserHatches);
        }
        return laserHatches;
    }

    public static @NotNull String getRender(int amperage) {
        String render = "wireless_energy_hatch";
        render = switch (amperage) {
            case 2 -> render;
            case 4 -> render + "_4a";
            case 16 -> render + "_16a";
            case 64 -> render + "_64a";
            default -> "wireless_laser_hatch.target";
        };
        return render;
    }
    // endregion

    public static void init() {}
}
