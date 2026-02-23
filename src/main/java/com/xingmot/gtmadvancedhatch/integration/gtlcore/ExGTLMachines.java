package com.xingmot.gtmadvancedhatch.integration.gtlcore;

import com.xingmot.gtmadvancedhatch.common.AHRegistration;
import com.xingmot.gtmadvancedhatch.common.data.AHTabs;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import com.lowdragmc.lowdraglib.LDLib;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.hepdd.gtmthings.data.WirelessMachines.registerWirelessLaserHatch;
import static com.xingmot.gtmadvancedhatch.common.data.AHMachines.*;

import com.gtladd.gtladditions.common.machine.GTLAddMachines;

public class ExGTLMachines {

    static {
        AHRegistration.registrate.creativeModeTab(() -> AHTabs.BASE_TAB);
    }

    // 1600万安培以上 仅限最高的两个电压
    public static final int[] HIGHEST_TIERS = GTCEuAPI.isHighTier() ? GTValues.tiersBetween(OpV, MAX) : GTValues.tiersBetween(ZPM, UV);
    // 电网仓（非激光
    public static final MachineDefinition[] NET_ENERGY_INPUT_HATCH_262144A = registerNetEnergyHatch(IO.IN, 262144, PartAbility.INPUT_ENERGY, NET_HIGH_TIERS2);
    public static final MachineDefinition[] NET_ENERGY_INPUT_HATCH_1048576A = registerNetEnergyHatch(IO.IN, 1048576, PartAbility.INPUT_ENERGY, NET_HIGH_TIERS2);
    public static final MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_262144A = registerNetEnergyHatch(IO.OUT, 262144, PartAbility.OUTPUT_ENERGY, NET_HIGH_TIERS2);
    public static final MachineDefinition[] NET_ENERGY_OUTPUT_HATCH_1048576A = registerNetEnergyHatch(IO.OUT, 1048576, PartAbility.OUTPUT_ENERGY, NET_HIGH_TIERS2);
    // 电网激光仓
    public static final MachineDefinition[] NET_LASER_INPUT_HATCH_262144A = registerNetLaserHatch(IO.IN, 262144, PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static final MachineDefinition[] NET_LASER_INPUT_HATCH_1048576A = registerNetLaserHatch(IO.IN, 1048576, PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static final MachineDefinition[] NET_LASER_INPUT_HATCH_4194304A = registerNetLaserHatch(IO.IN, 4194304, PartAbility.INPUT_LASER, NET_HIGH_TIERS);
    public static final MachineDefinition[] NET_LASER_INPUT_HATCH_16777216A = registerNetLaserHatch(IO.IN, 16777216, PartAbility.INPUT_LASER, HIGHEST_TIERS);
    public static final MachineDefinition[] NET_LASER_INPUT_HATCH_67108864A = registerNetLaserHatch(IO.IN, 67108864, PartAbility.INPUT_LASER, HIGHEST_TIERS);
    public static final MachineDefinition[] NET_LASER_INPUT_HATCH_2147483647A = registerNetLaserHatch(IO.IN, 2147483647, PartAbility.INPUT_LASER, HIGHEST_TIERS);
    public static final MachineDefinition[] NET_LASER_OUTPUT_HATCH_262144A = registerNetLaserHatch(IO.OUT, 262144, PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static final MachineDefinition[] NET_LASER_OUTPUT_HATCH_1048576A = registerNetLaserHatch(IO.OUT, 1048576, PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static final MachineDefinition[] NET_LASER_OUTPUT_HATCH_4194304A = registerNetLaserHatch(IO.OUT, 4194304, PartAbility.OUTPUT_LASER, NET_HIGH_TIERS);
    public static final MachineDefinition[] NET_LASER_OUTPUT_HATCH_16777216A = registerNetLaserHatch(IO.OUT, 16777216, PartAbility.OUTPUT_LASER, HIGHEST_TIERS);
    public static final MachineDefinition[] NET_LASER_OUTPUT_HATCH_67108864A = registerNetLaserHatch(IO.OUT, 67108864, PartAbility.OUTPUT_LASER, HIGHEST_TIERS);
    public static final MachineDefinition[] NET_LASER_OUTPUT_HATCH_2147483647A = registerNetLaserHatch(IO.OUT, 2147483647, PartAbility.OUTPUT_LASER, HIGHEST_TIERS);
    // 普通无线仓
    public static MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_16777216A;
    public static MachineDefinition[] WIRELESS_ENERGY_INPUT_HATCH_67108864A;
    public static MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_16777216A;
    public static MachineDefinition[] WIRELESS_ENERGY_OUTPUT_HATCH_67108863A;

    public static void init() {
        if (!LDLib.isModLoaded("gtladditions")) {
            WIRELESS_ENERGY_INPUT_HATCH_16777216A = registerWirelessLaserHatch(IO.IN, 16777216, PartAbility.INPUT_LASER, HIGHEST_TIERS);
            WIRELESS_ENERGY_INPUT_HATCH_67108864A = registerWirelessLaserHatch(IO.IN, 67108864, PartAbility.INPUT_LASER, HIGHEST_TIERS);
            WIRELESS_ENERGY_OUTPUT_HATCH_16777216A = registerWirelessLaserHatch(IO.OUT, 16777216, PartAbility.OUTPUT_LASER, HIGHEST_TIERS);
            WIRELESS_ENERGY_OUTPUT_HATCH_67108863A = registerWirelessLaserHatch(IO.OUT, 67108863, PartAbility.OUTPUT_LASER, HIGHEST_TIERS);
        } else {
            WIRELESS_ENERGY_INPUT_HATCH_16777216A = GTLAddMachines.INSTANCE.getWIRELESS_LASER_INPUT_HATCH_16777216A();
            WIRELESS_ENERGY_INPUT_HATCH_67108864A = GTLAddMachines.INSTANCE.getWIRELESS_LASER_INPUT_HATCH_67108864A();
            WIRELESS_ENERGY_OUTPUT_HATCH_16777216A = GTLAddMachines.INSTANCE.getWIRELESS_LASER_OUTPUT_HATCH_16777216A();
            WIRELESS_ENERGY_OUTPUT_HATCH_67108863A = GTLAddMachines.INSTANCE.getWIRELESS_LASER_OUTPUT_HATCH_67108863A();
        }
    }
}
