package com.sakurafuld.click3000.mixin;

import com.sakurafuld.click3000.ClickCommonConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult handlerUseItemOnMixin(ServerPlayerGameMode instance, ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit) {
        InteractionResult result = InteractionResult.FAIL;
        int repeat = ClickCommonConfig.getRepeat(player);
        for (int count = 0; count < repeat; count++) {
            InteractionResult current = instance.useItemOn(player, level, stack, hand, hit);
            if (current.ordinal() < result.ordinal()) {
                result = current;
            }

            double reach = Math.max(player.getEntityReach(), player.getBlockReach());
            if (player.pick(reach, 1, false) instanceof BlockHitResult bhr && bhr.getType() != HitResult.Type.MISS) {
                hit = bhr;
            } else {
                break;
            }
        }
        return result;
    }

    @Redirect(method = "handleUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult handleUseItemMixin(ServerPlayerGameMode instance, ServerPlayer player, Level level, ItemStack stack, InteractionHand hand) {
        InteractionResult result = InteractionResult.FAIL;
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        }
        int repeat = ClickCommonConfig.getRepeat(player);
        for (int count = 0; count < repeat; count++) {
            if (player.isUsingItem()) {
                ((LivingEntityAccessor) player).invokeUpdateUsingItem(stack);
                if (count != 0) {
                    int perRepeating = ClickCommonConfig.getRelease(stack.getItem());
                    if (perRepeating > 0 && count % perRepeating == 0) {
                        player.releaseUsingItem();
                    }
                }
            } else {
                InteractionResult current = this.useItemIgnoreCooldown(player, level, stack, hand);
                if (current.ordinal() < result.ordinal()) {
                    result = current;
                }
            }
        }
        return result;
    }

    @Unique
    private InteractionResult useItemIgnoreCooldown(ServerPlayer player, Level pLevel, ItemStack pStack, InteractionHand pHand) {
        InteractionResult cancelResult = net.minecraftforge.common.ForgeHooks.onItemRightClick(player, pHand);
        if (cancelResult != null) return cancelResult;
        int i = pStack.getCount();
        int j = pStack.getDamageValue();
        InteractionResultHolder<ItemStack> interactionresultholder = pStack.use(pLevel, player, pHand);
        ItemStack itemstack = interactionresultholder.getObject();
        if (itemstack == pStack && itemstack.getCount() == i && itemstack.getUseDuration() <= 0 && itemstack.getDamageValue() == j) {
            return interactionresultholder.getResult();
        } else if (interactionresultholder.getResult() == InteractionResult.FAIL && itemstack.getUseDuration() > 0 && !player.isUsingItem()) {
            return interactionresultholder.getResult();
        } else {
            if (pStack != itemstack) {
                player.setItemInHand(pHand, itemstack);
            }

            if (player.isCreative() && itemstack != ItemStack.EMPTY) {
                itemstack.setCount(i);
                if (itemstack.isDamageableItem() && itemstack.getDamageValue() != j) {
                    itemstack.setDamageValue(j);
                }
            }

            if (itemstack.isEmpty()) {
                player.setItemInHand(pHand, ItemStack.EMPTY);
            }

            if (!player.isUsingItem()) {
                player.inventoryMenu.sendAllDataToRemote();
            }

            return interactionresultholder.getResult();
        }
    }
}
