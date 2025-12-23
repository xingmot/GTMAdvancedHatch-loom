package com.xingmot.gtmadvancedhatch.api.gui;

import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.xingmot.gtmadvancedhatch.api.ConfigNotifiableItemStack;
import com.xingmot.gtmadvancedhatch.api.IConfigTransfer;
import com.xingmot.gtmadvancedhatch.common.data.MachinesConstants;
import com.xingmot.gtmadvancedhatch.util.AHFormattingUtil;
import com.xingmot.gtmadvancedhatch.util.AHUtil;

import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.ingredient.IGhostIngredientTarget;
import com.lowdragmc.lowdraglib.gui.ingredient.Target;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Setter;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PhantomItemCapacityWidget extends SlotWidget implements IPhantomAmountWidget<ItemStack>, IGhostIngredientTarget, IConfigurableWidget {

    @Setter
    private static IGuiTexture lockTexture = new GuiTextureGroup(new ResourceTexture("gtceu:textures/gui/widget/button_lock.png").scale(0.65F));
    public int slot;
    public int maxCapacity;
    public int lastSlotCapacity;
    public ItemStack lastPhantomStack;
    public IConfigTransfer<ItemStackHandler, ItemStack, ItemStack> icItemTransfer;
    /** 锁定滚动，每次打开gui时会自动锁定全部槽位 */
    @Setter
    protected boolean lockScroll = true;
    /** 截断警告，即将发生流体截断时会暂时锁定流体槽并设为true，解锁时设为false */
    @Setter
    protected boolean isWarned = false;
    ItemStack currentJEIRenderedIngredient;

    private final Supplier<ItemStack> phantomItemGetter;
    private final Consumer<ItemStack> phantomItemSetter;

    public PhantomItemCapacityWidget(IConfigTransfer<ItemStackHandler, ItemStack, ItemStack> icItemTransfer, IItemHandler itemHandler,
                                     int currentCapacity, int maxCapacity, int slotIndex, int xPosition, int yPosition) {
        this(icItemTransfer, currentCapacity, maxCapacity, slotIndex, xPosition, yPosition,
                () -> icItemTransfer.getLockedRef().getStackInSlot(slotIndex),
                (s) -> {
                    if (s != null && !s.isEmpty()) {
                        if (itemHandler.getStackInSlot(slotIndex).isEmpty() || itemHandler.getStackInSlot(slotIndex).is(s.getItem())) {
                            icItemTransfer.setLocked(true, slotIndex, s.copyWithCount(1));
                        }
                    } else {
                        icItemTransfer.setLocked(false, slotIndex);
                    }
                });
        this.slot = slotIndex;
    }

    public PhantomItemCapacityWidget(IConfigTransfer<ItemStackHandler, ItemStack, ItemStack> icItemTransfer,
                                     int currentCapacity, int maxCapacity, int slotIndex, int xPosition, int yPosition, Supplier<ItemStack> phantomItemGetter, Consumer<ItemStack> phantomItemSetter) {
        super(icItemTransfer.getLockedRef(), slotIndex, xPosition, yPosition);
        this.canPutItems = false;
        this.canTakeItems = false;

        this.icItemTransfer = icItemTransfer;
        this.lastSlotCapacity = currentCapacity;
        this.maxCapacity = maxCapacity;
        this.phantomItemGetter = phantomItemGetter;
        this.phantomItemSetter = phantomItemSetter;
    }

    @Override
    public int getAmount(int slot) {
        return this.lastSlotCapacity;
    }

    @Override
    public void setAmount(int slot, int capacity) {
        this.lastSlotCapacity = (int) capacity;
    }

    @Override
    public ItemStack getPhantomStack(int slot) {
        return this.lastPhantomStack;
    }

    @Override
    public @Nullable ItemStack slotClick(int dragType, ClickType clickTypeIn, Player player) {
        return null;
    }

    // 还原成流体虚拟槽的写法，这样方便修改
    protected void setLastPhantomStack(@Nullable ItemStack stack) {
        if (stack != null) {
            this.lastPhantomStack = stack.copyWithCount(1);
        } else {
            this.lastPhantomStack = null;
        }
    }

    public void writeInitialData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.icItemTransfer != null);
        if (this.icItemTransfer != null && icItemTransfer instanceof ConfigNotifiableItemStack cStack) {
            this.lastSlotCapacity = (int) this.icItemTransfer.getCapacity(this.slot);
            buffer.writeVarInt(this.lastSlotCapacity);
            ItemStack itemStack = cStack.storage.getStackInSlot(this.slot);
            this.lastPhantomStack = itemStack.copy();
            buffer.writeNbt(itemStack.save(new CompoundTag()));
        }
    }

    public void readInitialData(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            this.lastSlotCapacity = buffer.readVarInt();
            this.readUpdateInfo(2, buffer);
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (this.icItemTransfer != null && icItemTransfer instanceof ConfigNotifiableItemStack cStack) {
            ItemStack itemStack = cStack.storage.getStackInSlot(this.slot);
            int capacity = (int) this.icItemTransfer.getCapacity(this.slot);
            if (capacity != this.lastSlotCapacity) {
                this.lastSlotCapacity = capacity;
                this.writeUpdateInfo(0, (buffer) -> buffer.writeVarLong(this.lastSlotCapacity));
            }

            if (this.lastPhantomStack == null || !itemStack.is(this.lastPhantomStack.getItem())) {
                this.lastPhantomStack = itemStack.copy();
                CompoundTag fluidStackTag = itemStack.save(new CompoundTag());
                this.writeUpdateInfo(2, (buffer) -> buffer.writeNbt(fluidStackTag));
            } else {
                if (itemStack.getCount() == this.lastPhantomStack.getCount()) {
                    super.detectAndSendChanges();
                    return;
                }

                this.lastPhantomStack.setCount(itemStack.getCount());
                this.writeUpdateInfo(3, (buffer) -> buffer.writeVarLong(this.lastPhantomStack.getCount()));
            }

            if (this.changeListener != null) {
                this.changeListener.run();
            }
        }

        ItemStack stack = this.phantomItemGetter.get();
        if (stack != null && !stack.isEmpty()) {
            if (this.lastPhantomStack == null || !stack.is(this.lastPhantomStack.getItem())) {
                this.setLastPhantomStack(stack);
                Objects.requireNonNull(stack);
                this.writeUpdateInfo(5, buf -> writeToBuf(stack, buf));
            }
        } else if (this.lastPhantomStack != null) {
            this.setLastPhantomStack(null);
            this.writeUpdateInfo(4, (buf) -> {});
        }
    }

    public void writeToBuf(ItemStack stack, FriendlyByteBuf buf) {
        buf.writeUtf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        buf.writeInt(stack.getCount());
        buf.writeNbt(stack.getTag());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case 0 -> this.lastSlotCapacity = buffer.readVarInt();
            case 1 -> this.lastPhantomStack = null;
            case 2 -> this.lastPhantomStack = ItemStack.of(Objects.requireNonNull(buffer.readNbt()));
            case 3 -> {
                if (this.lastPhantomStack != null)
                    this.lastPhantomStack.setCount(buffer.readVarInt());
            }
            case 4 -> {
                ItemStack currentStack = this.gui.getModularUIContainer().getCarried();
                int newStackSize = buffer.readVarInt();
                currentStack.setCount(newStackSize);
                this.gui.getModularUIContainer().setCarried(currentStack);
            }
            case 6 -> this.lockScroll = buffer.readBoolean();
            case 7 -> this.isWarned = buffer.readBoolean();
            default -> super.readUpdateInfo(id, buffer);
        }
        if (this.changeListener != null) {
            this.changeListener.run();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (this.slotReference == null || !this.isMouseOverElement(mouseX, mouseY) || lockScroll) {
            return false;
        } else {
            long newCapacity = this.getNewCapacity(wheelDelta > (double) 0.0F ? 1 : -1);
            this.writeClientAction(MachinesConstants.SCROLL_ACTION_ID, (buf) -> buf.writeLong(newCapacity));
            return true;
        }
    }

    private long getNewCapacity(int wheel) {
        assert slotReference != null;
        if (GTUtil.isCtrlDown()) {
            long multi = 2;
            if (GTUtil.isShiftDown()) {
                multi *= 2;
            }
            if (GTUtil.isAltDown()) {
                multi *= 4;
            }
            return wheel > 0 ? Math.min(maxCapacity, AHUtil.multiplyWithLongBounds(this.getAmount(-1), multi)) : AHUtil.divWithLongBounds(this.getAmount(-1), multi);
        }
        long add = wheel;
        if (GTUtil.isShiftDown()) {
            add *= 10;
            if (GTUtil.isAltDown()) {
                add *= 10;
            }
        }

        if (!GTUtil.isAltDown()) {
            add *= 64;
        }
        return Math.min(maxCapacity, AHUtil.addWithLongBounds(this.getAmount(-1), add));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.slotReference != null && this.isMouseOverElement(mouseX, mouseY) && this.gui != null) {
            if (this.isClientSideWidget) {
                handlePhantomClick();
            } else {
                if (button == 0)
                    writeClientAction(MachinesConstants.MOUSE_LEFT_CLICK_ACTION_ID, buffer -> {});
                else if (button == 1)
                    writeClientAction(MachinesConstants.MOUSE_RIGHT_CLICK_ACTION_ID, buffer -> {});
                else if (button == 2)
                    writeClientAction(MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID, buffer -> buffer.writeBoolean(isCtrlDown()));
            }
            return true;
        }
        return false;
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        switch (id) {
            case MachinesConstants.SCROLL_ACTION_ID -> this.handleScrollAction(buffer.readInt());
            case 0 -> {
                if (this.phantomItemSetter != null) {
                    this.phantomItemSetter.accept(buffer.readItem());
                }
            }
            case MachinesConstants.MOUSE_LEFT_CLICK_ACTION_ID -> handlePhantomClick();
            case MachinesConstants.MOUSE_RIGHT_CLICK_ACTION_ID -> {}
            case MachinesConstants.MOUSE_MIDDLE_CLICK_ACTION_ID -> this.handleMiddleClick(buffer.readBoolean());
            default -> super.handleClientAction(id, buffer);
        }

        this.detectAndSendChanges();
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private void handleScrollAction(int newAmount) {
        if (this.getHandler() != null)
            if (icItemTransfer.isTruncate(this.slot, newAmount) && !isWarned) {
                setWarnedAndWrite(true);
                reverseLockScroll();
            } else {
                setWarnedAndWrite(false);
                icItemTransfer.newCapacity(this.slot, Math.max(newAmount, 0));
            }
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private void handleMiddleClick(boolean isCtrlDown) {
        if (!isCtrlDown || lockScroll) {
            reverseLockScroll();
        } else {
            if (this.getHandler() != null)
                if (icItemTransfer.isTruncate(this.slot, 0) && !isWarned) {
                    setWarnedAndWrite(true);
                    reverseLockScroll();
                } else {
                    setWarnedAndWrite(false);
                    icItemTransfer.newCapacity(this.slot, 0);
                }
        }
    }

    private void setWarnedAndWrite(boolean setTo) {
        this.isWarned = setTo;
        writeUpdateInfo(7, buf -> buf.writeBoolean(setTo));
    }

    private void reverseLockScroll() {
        this.lockScroll = !lockScroll;
        writeUpdateInfo(6, buf -> {
            buf.writeBoolean(lockScroll);
        });
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    private void handlePhantomClick() {
        ItemStack itemStack = this.gui.getModularUIContainer().getCarried().copy();
        if (!itemStack.isEmpty()) {
            itemStack.setCount(1);
            if (this.phantomItemSetter != null) {
                this.phantomItemSetter.accept(itemStack);
            }
        } else if (this.phantomItemSetter != null) {
            this.phantomItemSetter.accept(ItemStack.EMPTY);
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
        ArrayList<Component> tooltips = new ArrayList<>();
        ItemStack itemStack = this.currentJEIRenderedIngredient == null ? this.getRealStack(this.slotReference.getItem()) : this.currentJEIRenderedIngredient;
        if (itemStack != null && !itemStack.isEmpty()) {
            tooltips.add(itemStack.getDisplayName());
            if (!isShiftDown() && (itemStack.getCount() > min || this.getAmount(-1) > min)) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", AHFormattingUtil.formatLongToShort(itemStack.getCount(), min), AHFormattingUtil.formatLongToShort(this.getAmount(-1), min)));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_phantom_capacity.tooltips").withStyle(ChatFormatting.GOLD));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", itemStack.getCount(), this.getAmount(-1)));
                if (!isShiftDown() && getAmount(-1) != 0)
                    tooltips.add(Component.translatable("gtmadvancedhatch.gui.clear_phantom_capacity.tooltips").withStyle(ChatFormatting.GOLD));
            }
        } else {
            tooltips.add(Component.translatable("ldlib.fluid.empty"));
            if (!isShiftDown() && this.getAmount(-1) > min) {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, AHFormattingUtil.formatLongToShort(this.getAmount(-1), min)));
                tooltips.add(Component.translatable("gtmadvancedhatch.gui.shift_expand_tooltips").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, this.getAmount(-1)));
            }
        }

        tooltips.addAll(this.getTooltipTexts());
        return tooltips;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // 缩略显示数值的阈值
        long min = 102400;
        this.drawBackgroundTexture(graphics, mouseX, mouseY);
        Position pos = this.getPosition();
        Size size = getSize();
        ItemStack itemStack = phantomItemGetter.get();
        ModularUIGuiContainer modularUIGui = this.gui == null ? null : this.gui.getModularUIGui();
        if (itemStack != null && !itemStack.isEmpty()) {
            DrawerHelper.drawItemStack(graphics, itemStack, pos.x + 1, pos.y + 1, -1, (String) null);
        }
        this.drawOverlay(graphics, mouseX, mouseY, partialTicks);
        /* here lines */
        graphics.pose().pushPose();
        graphics.pose().scale(0.5F, 0.5F, 1.0F);
        String s = "/" + AHFormattingUtil.formatLongToShort(this.getAmount(-1), min);
        Font fontRenderer = Minecraft.getInstance().font;
        graphics.drawString(fontRenderer, s, (int) (((float) pos.x + (float) size.width / 3.0F) * 2.0F - (float) fontRenderer.width(s) + 21.0F), (int) (((float) pos.y + (float) size.height / 3.0F + 6.0F) * 2.0F), 16777215, true);
        graphics.pose().popPose();
        if (lockScroll && lockTexture != null) {
            lockTexture.draw(graphics, mouseX, mouseY, pos.x + 6, pos.y - 6, size.width, size.height);
        }
        if (isWarned) {
            graphics.pose().pushPose();
            float tickBy20 = AHUtil.tickAndWaitByNumberCycle(partialTicks, 10, 10);
            graphics.renderOutline(pos.x, pos.y, size.width, size.height, ColorUtils.color((int) (255 * tickBy20), 255, 0, 0));
            graphics.pose().popPose();

            if (MachinesConstants.debugGUI) {
                graphics.pose().pushPose();
                graphics.pose().scale(0.5F, 0.5F, 1);
                String s_tickBy20 = new DecimalFormat("#.##").format(tickBy20);
                graphics.drawString(fontRenderer, s_tickBy20, (int) ((pos.x + (size.width / 3.0F)) * 2.0F - (float) fontRenderer.width(s_tickBy20) + 21.0F),
                        (int) (pos.y + (size.height / 3.0F) + 6.0F) * 2.0F, ColorUtils.color(255, 255, 0, 0), true);
                graphics.pose().popPose();
            }
        }

        if (this.drawHoverOverlay && this.isMouseOverElement((double) mouseX, (double) mouseY) && this.getHoverElement((double) mouseX, (double) mouseY) == this) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(graphics, this.getPosition().x + 1, this.getPosition().y + 1, 16, 16, -2130706433);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public List<Target> getPhantomTargets(Object ingredient) {
        if (LDLib.isEmiLoaded() && ingredient instanceof EmiStack itemEmiStack) {
            Item item = (Item) itemEmiStack.getKeyOfType(Item.class);
            ingredient = item == null ? null : new ItemStack(item, (int) itemEmiStack.getAmount());
            if (ingredient instanceof ItemStack itemStack) {
                itemStack.setTag(itemEmiStack.getNbt());
            }
        }

        if (LDLib.isJeiLoaded() && ingredient instanceof ITypedIngredient<?> itemJeiStack) {
            ingredient = itemJeiStack.getItemStack().orElse(ItemStack.EMPTY);
        }

        if (!(ingredient instanceof ItemStack)) {
            return Collections.emptyList();
        } else {
            final Rect2i rectangle = this.toRectangleBox();
            return Lists.newArrayList(new Target[] { new Target() {

                @Nonnull
                public Rect2i getArea() {
                    return rectangle;
                }

                public void accept(@Nonnull Object ingredient) {
                    if (LDLib.isEmiLoaded() && ingredient instanceof EmiStack itemEmiStack) {
                        Item item = (Item) itemEmiStack.getKeyOfType(Item.class);
                        ingredient = item == null ? null : new ItemStack(item, (int) itemEmiStack.getAmount());
                        if (ingredient instanceof ItemStack itemStack) {
                            itemStack.setTag(itemEmiStack.getNbt());
                        }
                    }

                    if (LDLib.isJeiLoaded() && ingredient instanceof ITypedIngredient<?> itemJeiStack) {
                        ItemStack itemStack = (ItemStack) itemJeiStack.getItemStack().orElse(ItemStack.EMPTY);
                        if (!itemStack.isEmpty()) {
                            ingredient = itemStack;
                        }
                    }

                    if (slotReference != null && ingredient instanceof ItemStack stack) {
                        writeClientAction(0, (buffer) -> buffer.writeItem(stack));
                        if (isClientSideWidget && phantomItemSetter != null) {
                            phantomItemSetter.accept(stack);
                        }
                    }
                }
            } });
        }
    }
}
