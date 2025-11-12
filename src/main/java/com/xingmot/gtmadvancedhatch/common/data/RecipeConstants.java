package com.xingmot.gtmadvancedhatch.common.data;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.MetaMachineItem;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.gregtechceu.gtceu.common.data.GTItems.*;
import static com.gregtechceu.gtceu.common.data.GTMachines.*;

public class RecipeConstants {
    private RecipeConstants(){}

    public static final List<MachineDefinition> HUGE_TANK = List.of(
            SUPER_TANK[1],
            SUPER_TANK[1],
            SUPER_TANK[2],
            SUPER_TANK[3],
            SUPER_TANK[4],
            QUANTUM_TANK[5],
            QUANTUM_TANK[6],
            QUANTUM_TANK[7],
            QUANTUM_TANK[8],
            QUANTUM_TANK[9],
            QUANTUM_TANK[10],
            QUANTUM_TANK[11],
            QUANTUM_TANK[12],
            QUANTUM_TANK[13],
            QUANTUM_TANK[13]
    );

    public static final List<MachineDefinition> HUGE_CHEST = List.of(
            SUPER_CHEST[1],
            SUPER_CHEST[1],
            SUPER_CHEST[2],
            SUPER_CHEST[3],
            SUPER_CHEST[4],
            QUANTUM_CHEST[5],
            QUANTUM_CHEST[6],
            QUANTUM_CHEST[7],
            QUANTUM_CHEST[8],
            QUANTUM_CHEST[9],
            QUANTUM_CHEST[10],
            QUANTUM_CHEST[11],
            QUANTUM_CHEST[12],
            QUANTUM_CHEST[13],
            QUANTUM_CHEST[13]
    );

    public static final List<ItemEntry<Item>> ELECTRIC_MOTOR = List.of(
            ELECTRIC_MOTOR_LV,
            ELECTRIC_MOTOR_LV,
            ELECTRIC_MOTOR_MV,
            ELECTRIC_MOTOR_HV,
            ELECTRIC_MOTOR_EV,
            ELECTRIC_MOTOR_IV,
            ELECTRIC_MOTOR_LuV,
            ELECTRIC_MOTOR_ZPM,
            ELECTRIC_MOTOR_UV,
            ELECTRIC_MOTOR_UHV,
            ELECTRIC_MOTOR_UEV,
            ELECTRIC_MOTOR_UIV,
            ELECTRIC_MOTOR_UXV,
            ELECTRIC_MOTOR_OpV,
            ELECTRIC_MOTOR_OpV
    );
    public static final List<ItemEntry<ComponentItem>> ELECTRIC_PUMP = List.of(
            ELECTRIC_PUMP_LV,
            ELECTRIC_PUMP_LV,
            ELECTRIC_PUMP_MV,
            ELECTRIC_PUMP_HV,
            ELECTRIC_PUMP_EV,
            ELECTRIC_PUMP_IV,
            ELECTRIC_PUMP_LuV,
            ELECTRIC_PUMP_ZPM,
            ELECTRIC_PUMP_UV,
            ELECTRIC_PUMP_UHV,
            ELECTRIC_PUMP_UEV,
            ELECTRIC_PUMP_UIV,
            ELECTRIC_PUMP_UXV,
            ELECTRIC_PUMP_OpV,
            ELECTRIC_PUMP_OpV
    );
    public static final List<ItemEntry<ComponentItem>> FLUID_REGULATOR = List.of(
            FLUID_REGULATOR_LV,
            FLUID_REGULATOR_LV,
            FLUID_REGULATOR_MV,
            FLUID_REGULATOR_HV,
            FLUID_REGULATOR_EV,
            FLUID_REGULATOR_IV,
            FLUID_REGULATOR_LUV,
            FLUID_REGULATOR_ZPM,
            FLUID_REGULATOR_UV,
            FLUID_REGULATOR_UHV,
            FLUID_REGULATOR_UEV,
            FLUID_REGULATOR_UIV,
            FLUID_REGULATOR_UXV,
            FLUID_REGULATOR_OpV,
            FLUID_REGULATOR_OpV
    );
    public static final List<ItemEntry<ComponentItem>> CONVEYOR_MODULE = List.of(
            CONVEYOR_MODULE_LV,
            CONVEYOR_MODULE_LV,
            CONVEYOR_MODULE_MV,
            CONVEYOR_MODULE_HV,
            CONVEYOR_MODULE_EV,
            CONVEYOR_MODULE_IV,
            CONVEYOR_MODULE_LuV,
            CONVEYOR_MODULE_ZPM,
            CONVEYOR_MODULE_UV,
            CONVEYOR_MODULE_UHV,
            CONVEYOR_MODULE_UEV,
            CONVEYOR_MODULE_UIV,
            CONVEYOR_MODULE_UXV,
            CONVEYOR_MODULE_OpV,
            CONVEYOR_MODULE_OpV
    );
    public static final List<ItemEntry<Item>> ELECTRIC_PISTON = List.of(
            ELECTRIC_PISTON_LV,
            ELECTRIC_PISTON_LV,
            ELECTRIC_PISTON_MV,
            ELECTRIC_PISTON_HV,
            ELECTRIC_PISTON_EV,
            ELECTRIC_PISTON_IV,
            ELECTRIC_PISTON_LUV, // 怎么大小写突然错了
            ELECTRIC_PISTON_ZPM,
            ELECTRIC_PISTON_UV,
            ELECTRIC_PISTON_UHV,
            ELECTRIC_PISTON_UEV,
            ELECTRIC_PISTON_UIV,
            ELECTRIC_PISTON_UXV,
            ELECTRIC_PISTON_OpV,
            ELECTRIC_PISTON_OpV
    );
    public static final List<ItemEntry<ComponentItem>> ROBOT_ARM = List.of(
            ROBOT_ARM_LV,
            ROBOT_ARM_LV,
            ROBOT_ARM_MV,
            ROBOT_ARM_HV,
            ROBOT_ARM_EV,
            ROBOT_ARM_IV,
            ROBOT_ARM_LuV,
            ROBOT_ARM_ZPM,
            ROBOT_ARM_UV,
            ROBOT_ARM_UHV,
            ROBOT_ARM_UEV,
            ROBOT_ARM_UIV,
            ROBOT_ARM_UXV,
            ROBOT_ARM_OpV,
            ROBOT_ARM_OpV
    );
    public static final List<ItemEntry<Item>> FIELD_GENERATOR = List.of(
            FIELD_GENERATOR_LV,
            FIELD_GENERATOR_LV,
            FIELD_GENERATOR_MV,
            FIELD_GENERATOR_HV,
            FIELD_GENERATOR_EV,
            FIELD_GENERATOR_IV,
            FIELD_GENERATOR_LuV,
            FIELD_GENERATOR_ZPM,
            FIELD_GENERATOR_UV,
            FIELD_GENERATOR_UHV,
            FIELD_GENERATOR_UEV,
            FIELD_GENERATOR_UIV,
            FIELD_GENERATOR_UXV,
            FIELD_GENERATOR_OpV,
            FIELD_GENERATOR_OpV
    );
    public static final List<ItemEntry<Item>> EMITTER = List.of(
            EMITTER_LV,
            EMITTER_LV,
            EMITTER_MV,
            EMITTER_HV,
            EMITTER_EV,
            EMITTER_IV,
            EMITTER_LuV,
            EMITTER_ZPM,
            EMITTER_UV,
            EMITTER_UHV,
            EMITTER_UEV,
            EMITTER_UIV,
            EMITTER_UXV,
            EMITTER_OpV,
            EMITTER_OpV
    );
    public static final List<ItemEntry<Item>> SENSOR = List.of(
            SENSOR_LV,
            SENSOR_LV,
            SENSOR_MV,
            SENSOR_HV,
            SENSOR_EV,
            SENSOR_IV,
            SENSOR_LuV,
            SENSOR_ZPM,
            SENSOR_UV,
            SENSOR_UHV,
            SENSOR_UEV,
            SENSOR_UIV,
            SENSOR_UXV,
            SENSOR_OpV,
            SENSOR_OpV
    );
}
