package com.xingmot.gtmadvancedhatch.mixin.gtm;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEHatchPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;

import net.minecraft.core.Direction;

import java.util.EnumSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEHatchPartMachine.class)
public abstract class MEHatchPartMachineMixin extends FluidHatchPartMachine implements IGridConnectedMachine {

    public MEHatchPartMachineMixin(IMachineBlockEntity holder, int tier, IO io, long initialCapacity, int slots, Object... args) {
        super(holder, tier, io, initialCapacity, slots, args);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initAllFacing(CallbackInfo ci) {
        // 默认设置为全向连接模式
        if (getMainNode() != null) {
            getMainNode().setExposedOnSides(EnumSet.allOf(Direction.class));
            holder.self().getPersistentData().putBoolean("isAllFacing", true);
        }
    }
}
