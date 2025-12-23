package com.xingmot.gtmadvancedhatch.api.gui;

import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableItemStack;
import com.xingmot.gtmadvancedhatch.api.IConfigTransfer;
import com.xingmot.gtmadvancedhatch.common.data.MachinesConstants;
import com.xingmot.gtmadvancedhatch.util.AHFormattingUtil;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

// 超堆叠 + 过滤时半透明影子
public class ConfigSlotWidget extends SlotWidget {

    @Nullable
    ItemStack currentJEIRenderedIngredient;
    IConfigTransfer<ItemStackHandler, ItemStack, ItemStack> icItemTransfer;
    public int slot;
    public int lastSlotCapacity;

    @Override
    protected Slot createSlot(IItemTransfer itemHandler, int index) {
        return new ConfigWidgetSlotItemTransfer(itemHandler, index, 0, 0);
    }

    public ConfigSlotWidget(IConfigTransfer<ItemStackHandler, ItemStack, ItemStack> icItemTransfer, ItemStackHandler itemHandler, int slotIndex, int xPosition, int yPosition,
                            boolean canTakeItems, boolean canPutItems) {
        super(itemHandler, slotIndex, xPosition, yPosition, canTakeItems, canPutItems);
        this.icItemTransfer = icItemTransfer;
        this.slot = slotIndex;
    }

    // 超堆叠时禁用交换物品
    @Override
    public ItemStack slotClick(int dragType, ClickType clickTypeIn, Player player) {
        if (clickTypeIn == ClickType.SWAP && this.slotReference != null && this.slotReference.getItem().getCount() > this.slotReference.getItem().getMaxStackSize()) {
            return ItemStack.EMPTY;
        }
        return super.slotClick(dragType, clickTypeIn, player);
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
            if (slotReference != null)
                slotReference.set(ItemStack.EMPTY);
        } else {
            super.handleClientAction(id, buffer);
            return;
        }
        if (changeListener != null) {
            changeListener.run();
        }
    }

    public class ConfigWidgetSlotItemTransfer extends WidgetSlotItemTransfer implements IUnlimitedStack {

        public ConfigWidgetSlotItemTransfer(IItemTransfer itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public int getMaxStackSize(@Nonnull ItemStack stack) {
            return this.getItemHandler().getSlotLimit(this.getSlotIndex());
        }
    }

    // 同步容量
    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.icItemTransfer != null);
        if (this.icItemTransfer != null && icItemTransfer instanceof ConfigNotifiableItemStack cStack) {
            this.lastSlotCapacity = (int) this.icItemTransfer.getCapacity(this.slot);
            buffer.writeVarInt(this.lastSlotCapacity);
        }
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            this.lastSlotCapacity = buffer.readVarInt();
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (this.icItemTransfer != null && icItemTransfer instanceof ConfigNotifiableItemStack cStack) {
            int capacity = (int) this.icItemTransfer.getCapacity(this.slot);
            if (capacity != this.lastSlotCapacity) {
                this.lastSlotCapacity = capacity;
                this.writeUpdateInfo(0, (buffer) -> buffer.writeVarLong(this.lastSlotCapacity));
            }
        }
    }

    @Override
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case 0 -> this.lastSlotCapacity = buffer.readVarInt();
            default -> super.readUpdateInfo(id, buffer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.slotReference != null && this.drawHoverTips && this.isMouseOverElement((double) mouseX, (double) mouseY) && this.getHoverElement((double) mouseX, (double) mouseY) == this) {
            ItemStack stack = this.slotReference.getItem();
            if (this.gui != null) {
                this.gui.getModularUIGui().setHoveredSlot(this.slotReference);
                this.gui.getModularUIGui().setHoverTooltip(this.getFullTooltipTexts(), stack, (Font) null, (TooltipComponent) stack.getTooltipImage().orElse(null));
            } else {
                super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
            }
        } else {
            super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public List<Component> getFullTooltipTexts() {
        if (this.slotReference == null) return Collections.emptyList();
        // 缩略显示数值的阈值
        long min = 102400;
        int capacity = lastSlotCapacity;
        ArrayList<Component> tooltips = new ArrayList<>();
        ItemStack itemStack = this.currentJEIRenderedIngredient == null ? this.getRealStack(this.slotReference.getItem()) : this.currentJEIRenderedIngredient;
        if (itemStack != null && !itemStack.isEmpty()) {
            Pair<String, ChatFormatting> progressAndColor = AHUtil.getCapacityProgressAndColor(itemStack.getCount(), capacity, this.canPutItems);
            tooltips.add(Component.literal("").append(itemStack.getDisplayName())
                    .append(Component.literal(progressAndColor.getFirst()).withStyle(progressAndColor.getSecond())));
            if (!isShiftDown() && (itemStack.getCount() > min || capacity > min)) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", AHFormattingUtil.formatLongToShort(itemStack.getCount(), min), AHFormattingUtil.formatLongToShort(capacity, min)));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_content.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", itemStack.getCount(), capacity));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_content.tooltips").withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltips.add(Component.translatable("ldlib.fluid.empty"));
            if (!isShiftDown() && capacity > min) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, AHFormattingUtil.formatLongToShort(capacity, min)));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, capacity));
            }
        }

        tooltips.addAll(this.getTooltipTexts());
        return tooltips;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.drawBackgroundTexture(graphics, mouseX, mouseY);
        Position pos = this.getPosition();
        Size size = this.getSize();
        if (this.slotReference != null) {
            ItemStack itemStack = this.currentJEIRenderedIngredient == null ? this.getRealStack(this.slotReference.getItem()) : this.currentJEIRenderedIngredient;
            if (!itemStack.isEmpty()) {
                DrawerHelper.drawItemStack(graphics, itemStack, pos.x + 1, pos.y + 1, -1, (String) null);
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1.0F);
                Font fontRenderer = Minecraft.getInstance().font;
                Pair<String, ChatFormatting> progressAndColor = AHUtil.getCapacityProgressAndColor(itemStack.getCount(), lastSlotCapacity, this.canPutItems);
                graphics.drawString(fontRenderer, progressAndColor.getFirst(), (int) ((pos.x + (size.width / 3.0F)) * 2.0F - (float) fontRenderer.width(progressAndColor.getFirst()) + 21.0F) + 1,
                        (int) (pos.y + (size.height / 3.0F) + 6.0F) * 2.0F - 20, progressAndColor.getSecond().getColor(), true);
                graphics.pose().popPose();
            }
        }

        this.drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (this.drawHoverOverlay && this.isMouseOverElement((double) mouseX, (double) mouseY) && this.getHoverElement((double) mouseX, (double) mouseY) == this) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(graphics, this.getPosition().x + 1, this.getPosition().y + 1, 16, 16, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
        }
    }
}
