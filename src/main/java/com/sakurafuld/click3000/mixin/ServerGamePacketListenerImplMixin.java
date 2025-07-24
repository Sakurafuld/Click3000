package com.sakurafuld.click3000.mixin;

import com.sakurafuld.click3000.Click3000;
import com.sakurafuld.click3000.Click3000CommonConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    private static boolean wasBlockPlacementAttempt(ServerPlayer pPlayer, ItemStack pStack) {
        throw new AssertionError();
    }

    @Redirect(method = "handleUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult handlerUseItemOnMixin(ServerPlayerGameMode instance, ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit) {
        Click3000.LOG.info("ServerUseItemOn!!!!");
        InteractionResult result = InteractionResult.FAIL;
        for (int count = 0; count < Click3000CommonConfig.REPEAT.get(); count++) {
            InteractionResult current = instance.useItemOn(player, level, stack, hand, hit);
            if (current.ordinal() < result.ordinal()) {
                result = current;
            }
            if (count != Click3000CommonConfig.REPEAT.get() - 1 && hit.getDirection() == Direction.UP && !result.consumesAction() && hit.getBlockPos().getY() >= level.getMaxBuildHeight() - 1 && wasBlockPlacementAttempt(player, stack)) {
                Component component = Component.translatable("build.tooHigh", level.getMaxBuildHeight() - 1).withStyle(ChatFormatting.RED);
                player.sendSystemMessage(component, true);
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
        Click3000.LOG.info("ServerUseItem!!!!!!!");
        InteractionResult result = InteractionResult.FAIL;
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        } else if (player.getCooldowns().isOnCooldown(stack.getItem())) {
            return InteractionResult.PASS;
        }
        for (int count = 0; count < Click3000CommonConfig.REPEAT.get(); count++) {
            if (player.isUsingItem()) {
                ((LivingEntityAccessor) player).invokeUpdateUsingItem(stack);
                if (count != 0) {
                    int perRepeating = Click3000CommonConfig.RELEASE.get().stream()
                            .map(String.class::cast)
                            .map(string -> {
                                boolean tag = string.startsWith("#");
                                if (tag) {
                                    string = string.substring(1);
                                }

                                String[] split = string.split("=");
                                ResourceLocation identifier = ResourceLocation.parse(split[0]);
                                int value = Integer.parseInt(split[1]);
                                return Triple.of(tag, identifier, value);
                            })
                            .filter(triple -> {
                                if (triple.getLeft()) {
                                    return stack.is(ItemTags.create(triple.getMiddle()));
                                } else {
                                    return ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(triple.getMiddle());
                                }
                            })
                            .map(Triple::getRight)
                            .findFirst()
                            .orElse(-1);
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
