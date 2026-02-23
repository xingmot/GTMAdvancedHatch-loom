package com.xingmot.gtmadvancedhatch.mixin.gtlcore;

import org.gtlcore.gtlcore.common.machine.multiblock.part.ae.MEIOPartMachine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;

import net.minecraft.core.Direction;

import java.util.EnumSet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MEIOPartMachine.class)
public abstract class MEIOPartMachineMixin extends MultiblockPartMachine implements IGridConnectedMachine {

    public MEIOPartMachineMixin(IMachineBlockEntity holder) {
        super(holder);
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
