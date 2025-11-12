package com.xingmot.gtmadvancedhatch.integration.gtlcore;

import com.xingmot.gtmadvancedhatch.GTMAdvancedHatch;

import org.gtlcore.gtlcore.common.data.GTLMachines;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.GTValues.VN;
import static com.gregtechceu.gtceu.common.data.GTRecipeTypes.ASSEMBLER_RECIPES;
import static com.xingmot.gtmadvancedhatch.common.data.AHMachines.NET_HIGH_TIERS;
import static com.xingmot.gtmadvancedhatch.common.data.AHMachines.NET_HIGH_TIERS2;
import static com.xingmot.gtmadvancedhatch.common.data.recipe.AHRecipes.getTierTransformer;
import static com.xingmot.gtmadvancedhatch.common.data.recipe.AHRecipes.getTierWorldAccelerator;
import static com.xingmot.gtmadvancedhatch.integration.gtlcore.ExGTLMachines.HIGHEST_TIERS;

public class ExGTLRecipes {

    public static void initRecipes(Consumer<FinishedRecipe> provider) {
        boolean gtladd_is_on = LDLib.isModLoaded("gtladditions");
        for (int tier : NET_HIGH_TIERS) {
            // region 262144A激光电网仓配方
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_262144a"))
                    .outputItems(ExGTLMachines.NET_LASER_INPUT_HATCH_262144A[tier],1)
                    .inputItems(GTLMachines.WIRELESS_ENERGY_INPUT_HATCH_262144A[tier],4)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_262144a"))
                    .outputItems(ExGTLMachines.NET_LASER_OUTPUT_HATCH_262144A[tier],1)
                    .inputItems(GTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_262144A[tier],4)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            // endregion
            // region 1048576A激光电网仓配方
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_1048576a"))
                    .outputItems(ExGTLMachines.NET_LASER_INPUT_HATCH_1048576A[tier],1)
                    .inputItems(GTLMachines.WIRELESS_ENERGY_INPUT_HATCH_1048576A[tier],4)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_1048576a"))
                    .outputItems(ExGTLMachines.NET_LASER_OUTPUT_HATCH_1048576A[tier],1)
                    .inputItems(GTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_1048576A[tier],4)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            // endregion
            // region 4194304A激光电网仓配方
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_4194304a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_4194304A[tier],1)
                    .inputItems(GTLMachines.WIRELESS_ENERGY_INPUT_HATCH_4194304A[tier],4)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_4194304a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_4194304A[tier],1)
                    .inputItems(GTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_4194304A[tier],4)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            // endregion
        }
        for (int tier : HIGHEST_TIERS) {
            if (gtladd_is_on) {
                // region 16777216A激光仓配方
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_16777216a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_16777216A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_INPUT_HATCH_16777216A[tier - 1],16)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_16777216a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_16777216A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_16777216A[tier-1],16)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                // endregion
                // region 67108864A激光仓配方
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_67108864a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_67108864A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_INPUT_HATCH_67108864A[tier-1],16)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_67108864a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_67108864A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_67108863A[tier-1],16)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                // endregion
            } else {
                // region 16777216A激光电网仓配方
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_16777216a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_16777216A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_INPUT_HATCH_16777216A[tier],4)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_16777216a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_16777216A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_16777216A[tier],4)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                // endregion
                // region 67108864A激光电网仓配方
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_67108864a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_67108864A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_INPUT_HATCH_67108864A[tier],4)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_67108864a"))
                        .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_67108864A[tier],1)
                        .inputItems(ExGTLMachines.WIRELESS_ENERGY_OUTPUT_HATCH_67108863A[tier],4)
                        .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                        .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                        .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                        .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                        .duration(1600)
                        .EUt(VA[tier])
                        .save(provider);
                // endregion
            }
            // region 2147483647A激光电网仓配方
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_input_hatch_" + VN[tier].toLowerCase() + "_2147483647a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_2147483647A[tier],1)
                    .inputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_67108864A[tier],16)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_laser_output_hatch_" + VN[tier].toLowerCase() + "_2147483647a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_2147483647A[tier],1)
                    .inputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_67108864A[tier],16)
                    .inputItems(GTMachines.WORLD_ACCELERATOR[getTierWorldAccelerator(tier)],1)
                    .inputItems(GTMachines.POWER_TRANSFORMER[getTierTransformer(tier)],1)
                    .inputItems(GTItems.COVER_ENERGY_DETECTOR_ADVANCED,1)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(576))
                    .duration(1600)
                    .EUt(VA[tier])
                    .save(provider);
            // endregion
        }
        // region 大安培电网仓
        for (int tier : NET_HIGH_TIERS2) {
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_energy_input_hatch_" + GTValues.VN[tier].toLowerCase() + "_262144a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_262144A[tier],1)
                    .inputItems(ExGTLMachines.NET_LASER_INPUT_HATCH_262144A[tier],1)
                    .duration(400)
                    .EUt(GTValues.VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_energy_input_hatch_" + GTValues.VN[tier].toLowerCase() + "_1048576a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_INPUT_HATCH_1048576A[tier],1)
                    .inputItems(ExGTLMachines.NET_LASER_INPUT_HATCH_1048576A[tier],1)
                    .duration(400)
                    .EUt(GTValues.VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_energy_output_hatch_" + GTValues.VN[tier].toLowerCase() + "_262144a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_262144A[tier],1)
                    .inputItems(ExGTLMachines.NET_LASER_OUTPUT_HATCH_262144A[tier],1)
                    .duration(400)
                    .EUt(GTValues.VA[tier])
                    .save(provider);
            ASSEMBLER_RECIPES.recipeBuilder(GTMAdvancedHatch.id("net_energy_output_hatch_" + GTValues.VN[tier].toLowerCase() + "_1048576a"))
                    .outputItems(ExGTLMachines.NET_ENERGY_OUTPUT_HATCH_1048576A[tier],1)
                    .inputItems(ExGTLMachines.NET_LASER_OUTPUT_HATCH_1048576A[tier],1)
                    .duration(400)
                    .EUt(GTValues.VA[tier])
                    .save(provider);
        }
        // endregion
    }
}
