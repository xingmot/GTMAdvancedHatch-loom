package com.xingmot.gtmadvancedhatch.mixin.gtm;

import com.xingmot.gtmadvancedhatch.api.util.VoltageLevelLookup;

import org.gtlcore.gtlcore.integration.gtmt.NewGTValues;
import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.integration.jade.provider.RecipeLogicProvider;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import static net.minecraft.ChatFormatting.*;
import static org.gtlcore.gtlcore.utils.TextUtil.GTL_CORE$VC;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@Mixin(value = RecipeLogicProvider.class, priority = 2000)
public class RecipeLogicProviderMixin {

    /**
     * @author Savitor
     * @reason 无
     */
    @Overwrite(remap = false)
    protected void addTooltip(CompoundTag capData, ITooltip tooltip, Player player, BlockAccessor block,
                              BlockEntity blockEntity, IPluginConfig config) {
        BigDecimal base = new BigDecimal(1000);
        DecimalFormat format = new DecimalFormat("0.00E0");
        if (capData.getBoolean("Working")) {
            var recipeInfo = capData.getCompound("Recipe");
            if (!recipeInfo.isEmpty()) {
                var eut = recipeInfo.getLong("EUt");

                if (eut != 0) {
                    var isInput = recipeInfo.getBoolean("isInput");
                    long longEu = Math.abs(eut);
                    BigDecimal absEUt = new BigDecimal(Math.abs(eut));

                    var tier = longEu == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(longEu);
                    Component text = Component.literal(absEUt.compareTo(base) < 0 ? absEUt.toString() : format.format(absEUt))
                            .withStyle(RED)
                            .append(Component.literal(" EU/t").withStyle(RESET)
                                    .append(Component.literal(" (").withStyle(GREEN)
                                            .append(Component.literal(FormattingUtil.DECIMAL_FORMAT_2F.format(absEUt.divide(BigDecimal.valueOf(GTValues.VEX[tier]), 3, RoundingMode.DOWN)
                                                    .doubleValue()) + "A ")
                                                    .withStyle(style -> style.withColor(GTL_CORE$VC[Math.min(tier, 14)])))
                                            .append(Component.literal(VoltageLevelLookup.findVoltageLevel(absEUt)))
                                            .append(Component.literal(")").withStyle(GREEN))));

                    if (isInput) {
                        tooltip.add(Component.translatable("gtceu.top.energy_consumption").append(" ").append(text));
                    } else {
                        tooltip.add(Component.translatable("gtceu.top.energy_production").append(" ").append(text));
                    }
                } else if (capData.contains("wirelessTickInputs", Tag.TAG_BYTE_ARRAY)) {
                    BigInteger wirelessEut = new BigInteger(capData.getByteArray("wirelessTickInputs"));
                    BigInteger abs = wirelessEut.abs();
                    long longEu = NumberUtils.getLongValue(abs);
                    var tier = longEu == Long.MAX_VALUE ? GTValues.MAX_TRUE : NumberUtils.getFakeVoltageTier(longEu);
                    Component text = Component.literal(abs.compareTo(base.toBigInteger()) < 0 ? abs.toString() : format.format(abs))
                            .withStyle(RED)
                            .append(Component.literal(" EU/t").withStyle(RESET)
                                    .append(Component.literal(" (").withStyle(GREEN)
                                            .append(Component
                                                    .translatable("gtceu.top.electricity",
                                                            FormattingUtil.DECIMAL_FORMAT_2F.format(new BigDecimal(abs).divide(BigDecimal.valueOf(GTValues.VEX[tier]), 3, RoundingMode.DOWN)
                                                                    .doubleValue()),
                                                            NewGTValues.VNF[tier])
                                                    .withStyle(style -> style.withColor(GTL_CORE$VC[Math.min(tier, 14)])))
                                            .append(Component.literal(")").withStyle(GREEN))));

                    if (wirelessEut.signum() < 0) {
                        tooltip.add(Component.translatable("gtceu.top.energy_consumption").append(" ").append(text));
                    } else {
                        tooltip.add(Component.translatable("gtceu.top.energy_production").append(" ").append(text));
                    }
                }
            }
            String reason = capData.getString("work_reason");
            if (reason.isEmpty()) return;
            tooltip.add(Component.translatable("gtceu.recipe.fail.reason", reason).withStyle(RED));
        } else {
            String reason = capData.getString("reason");
            if (reason.isEmpty()) return;
            tooltip.add(Component.translatable("gtceu.recipe.fail.reason", reason).withStyle(RED));
        }
    }
}
