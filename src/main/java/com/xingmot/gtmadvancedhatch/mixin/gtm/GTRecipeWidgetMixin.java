package com.xingmot.gtmadvancedhatch.mixin.gtm;

import org.gtlcore.gtlcore.utils.NumberUtils;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.GTRecipeWidget;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.gtlcore.gtlcore.utils.TextUtil.GTL_CORE$VC;

import com.gtladd.gtladditions.utils.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = GTRecipeWidget.class, priority = 2200)
public abstract class GTRecipeWidgetMixin {

    /**
     * @author Savitor
     * @reason 无
     */
    @Overwrite(remap = false)
    private static @NotNull List<Component> getRecipeParaText(GTRecipe recipe, int duration, long inputEUt, long outputEUt) {
        DecimalFormat format = new DecimalFormat("0.00E0");
        BigDecimal base = new BigDecimal(1000);
        List<Component> texts = new ArrayList<>();
        if (!recipe.data.getBoolean("hide_duration")) {
            texts.add(Component.translatable("gtceu.recipe.duration", FormattingUtil.formatNumbers(duration / 20f)));
        }
        var EUt = inputEUt;
        boolean isOutput = false;
        if (EUt == 0) {
            EUt = outputEUt;
            isOutput = true;
        }
        if (EUt > 0) {
            BigInteger euTotal = BigInteger.valueOf(EUt).multiply(BigInteger.valueOf(duration));
            // sadly we still need a custom override here, since computation uses duration and EU/t very differently
            if (recipe.data.getBoolean("duration_is_total_cwu") &&
                    recipe.tickInputs.containsKey(CWURecipeCapability.CAP)) {
                int minimumCWUt = Math.max(recipe.tickInputs.get(CWURecipeCapability.CAP).stream()
                        .map(Content::getContent).mapToInt(CWURecipeCapability.CAP::of).sum(), 1);
                texts.add(Component.translatable("gtceu.recipe.max_eu",
                        FormattingUtil.formatNumbers(NumberUtils.getLongValue(euTotal) / minimumCWUt)));
            } else
                texts.add(Component.translatable("gtceu.recipe.total", euTotal.compareTo(base.toBigInteger()) < 0 ? euTotal.toString() : format.format(euTotal)));
            long absEUt = Math.abs(EUt);
            BigDecimal eut = new BigDecimal(absEUt);
            var tier = GTUtil.getTierByVoltage(absEUt);
            var component = Component.translatable(!isOutput ? "gtceu.recipe.eu" : "gtceu.recipe.eu_inverted",
                    eut.compareTo(base) < 0 ? eut.toString() : format.format(eut));
            if (!isOutput) component.append(
                    Component.literal(" (").withStyle(ChatFormatting.GREEN)
                            .append(Component
                                    .literal(CommonUtils.formatDouble((double) absEUt / GTValues.V[tier]) + "A")
                                    .withStyle(style -> style.withColor(GTL_CORE$VC[tier])))
                            .append(Component.literal(")").withStyle(ChatFormatting.GREEN)));
            texts.add(component);
        }

        return texts;
    }
}
