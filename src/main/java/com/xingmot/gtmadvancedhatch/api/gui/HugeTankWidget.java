package com.xingmot.gtmadvancedhatch.api.gui;

import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.client.TooltipsHandler;
import com.lowdragmc.lowdraglib.side.fluid.FluidHelper;
import com.lowdragmc.lowdraglib.side.fluid.forge.FluidHelperImpl;
import com.xingmot.gtmadvancedhatch.common.data.MachinesConstants;
import com.xingmot.gtmadvancedhatch.util.AHFormattingUtil;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraftforge.fluids.FluidStack;

public class HugeTankWidget extends TankWidget {

    FluidStack currentJEIRenderedIngredient;

    public HugeTankWidget(CustomFluidTank fluidTank, int x, int y, boolean allowClickContainerFilling, boolean allowClickContainerEmptying) {
        super(fluidTank, x, y, allowClickContainerFilling, allowClickContainerEmptying);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 2) {
                writeClientAction(MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID, buffer -> {});
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID && isCtrlDown()) {
            if (fluidTank != null)
                fluidTank.getFluidInTank(this.tank).setAmount(0);
        } else {
            super.handleClientAction(id, buffer);
            return;
        }
        if (changeListener != null) {
            changeListener.run();
        }
    }

    @Override
    public List<Component> getFullTooltipTexts() {
        // 缩略显示数值的阈值
        long min = 128000;
        ArrayList<Component> tooltips = new ArrayList<>();
        FluidStack fluidStack = this.currentJEIRenderedIngredient != null ? this.currentJEIRenderedIngredient : this.lastFluidInTank;
        if (fluidStack != null && !fluidStack.isEmpty()) {
            Pair<String, ChatFormatting> progressAndColor = AHUtil.getCapacityProgressAndColor(fluidStack.getAmount(), lastTankCapacity, this.allowClickDrained);
            tooltips.add(Component.literal("").append(fluidStack.getDisplayName())
                    .append(Component.literal(progressAndColor.getFirst()).withStyle(progressAndColor.getSecond())));
            if (!isShiftDown() && (fluidStack.getAmount() > min || this.lastTankCapacity > min)) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", AHFormattingUtil.formatLongBucketsToShort(fluidStack.getAmount(), min), AHFormattingUtil.formatLongBucketsToShort(this.lastTankCapacity, min)));
                if (!Platform.isForge()) {
                    tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(fluidStack.getAmount() * 1000L / FluidHelper.getBucket(), this.lastTankCapacity * 1000L / FluidHelper.getBucket())).append(" mB"));
                }
                TooltipsHandler.appendFluidTooltips(fluidStack, tooltips::add, null);
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_content.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", fluidStack.getAmount(), this.lastTankCapacity).append(" " + FluidHelper.getUnit()));
                if (!Platform.isForge()) {
                    tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(fluidStack.getAmount() * 1000L / FluidHelper.getBucket(), this.lastTankCapacity * 1000L / FluidHelper.getBucket())).append(" mB"));
                }
                TooltipsHandler.appendFluidTooltips(fluidStack, tooltips::add, null);
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_content.tooltips").withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltips.add(Component.translatable("ldlib.fluid.empty"));
            if (!isShiftDown() && this.lastTankCapacity > min) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, AHFormattingUtil.formatLongBucketsToShort(this.lastTankCapacity, min)));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, this.lastTankCapacity)
                        .append(" " + FluidHelper.getUnit()));
            }
            if (!Platform.isForge()) {
                tooltips.add(Component.literal("§6mB:§r %d/%d".formatted(0, this.lastTankCapacity * 1000L / FluidHelper.getBucket()))
                        .append(" mB"));
            }
        }

        tooltips.addAll(this.getTooltipTexts());
        return tooltips;
    }

    /** 修改数字显示 copy from gtm */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.drawBackgroundTexture(graphics, mouseX, mouseY);
        if (this.isClientSideWidget && this.fluidTank != null) {
            FluidStack fluidStack = this.fluidTank.getFluidInTank(this.tank);
            int capacity = this.fluidTank.getTankCapacity(this.tank);
            if (capacity != this.lastTankCapacity) {
                this.lastTankCapacity = capacity;
            }

            if (!fluidStack.isFluidEqual(this.lastFluidInTank)) {
                this.lastFluidInTank = fluidStack.copy();
            } else if (fluidStack.getAmount() != this.lastFluidInTank.getAmount()) {
                this.lastFluidInTank.setAmount(fluidStack.getAmount());
            }
        }

        Position pos = this.getPosition();
        Size size = this.getSize();
        FluidStack renderedFluid = this.currentJEIRenderedIngredient != null ? this.currentJEIRenderedIngredient : this.lastFluidInTank;
        if (renderedFluid != null) {
            RenderSystem.disableBlend();
            if (!renderedFluid.isEmpty()) {
                double progress = (double) renderedFluid.getAmount() * (double) 1.0F / (double) Math.max(Math.max(renderedFluid.getAmount(), this.lastTankCapacity), 1L);
                float drawnU = (float) this.fillDirection.getDrawnU(progress);
                float drawnV = (float) this.fillDirection.getDrawnV(progress);
                float drawnWidth = (float) this.fillDirection.getDrawnWidth(progress);
                float drawnHeight = (float) this.fillDirection.getDrawnHeight(progress);
                int width = size.width - 2;
                int height = size.height - 2;
                int x = pos.x + 1;
                int y = pos.y + 1;
                DrawerHelper.drawFluidForGui(graphics, FluidHelperImpl.toFluidStack(renderedFluid), renderedFluid.getAmount(), (int) ((float) x + drawnU * (float) width), (int) ((float) y + drawnV * (float) height), (int) ((float) width * drawnWidth), (int) ((float) height * drawnHeight));
            }

            if (this.showAmount && !renderedFluid.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1.0F);
                long fluidAmount = renderedFluid.getAmount();
                /* here 1 line */
                String s = AHFormattingUtil.formatLongBucketsCompactStringBuckets(fluidAmount) + "B";
                Font fontRenderer = Minecraft.getInstance().font;
                graphics.drawString(fontRenderer, s, (int) (((float) pos.x + (float) size.width / 3.0F) * 2.0F - (float) fontRenderer.width(s) + 21.0F), (int) (((float) pos.y + (float) size.height / 3.0F + 6.0F) * 2.0F), 16777215, true);
                /* here 2 lines */
                Pair<String, ChatFormatting> progressAndColor = AHUtil.getCapacityProgressAndColor(fluidAmount, lastTankCapacity, this.allowClickDrained);
                graphics.drawString(fontRenderer, progressAndColor.getFirst(), (int) ((pos.x + (size.width / 3.0F)) * 2.0F - (float) fontRenderer.width(progressAndColor.getFirst()) + 21.0F) + 1,
                        (int) (pos.y + (size.height / 3.0F) + 6.0F) * 2.0F - 20, progressAndColor.getSecond().getColor(), true);
                graphics.pose().popPose();
            }

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        this.drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (this.drawHoverOverlay && this.isMouseOverElement((double) mouseX, (double) mouseY) && this.getHoverElement((double) mouseX, (double) mouseY) == this) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(graphics, this.getPosition().x + 1, this.getPosition().y + 1, this.getSize().width - 2, this.getSize().height - 2, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
        }
    }
}
