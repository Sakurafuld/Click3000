package com.sakurafuld.click3000.mixin;

import com.sakurafuld.click3000.ClickCommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
@OnlyIn(Dist.CLIENT)
public abstract class MultiPlayerGameModeMixin {
    @Shadow
    protected abstract InteractionResult performUseItemOn(LocalPlayer pPlayer, InteractionHand pHand, BlockHitResult pResult);

    @Shadow
    public abstract float getPickRange();

    @Shadow
    protected abstract void startPrediction(ClientLevel pLevel, PredictiveAction pAction);

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V"), cancellable = true)
    private void useItemOnMixin(LocalPlayer pPlayer, InteractionHand pHand, BlockHitResult pResult, CallbackInfoReturnable<InteractionResult> cir) {
        MutableObject<InteractionResult> mutableobject = new MutableObject<>();
        this.startPrediction(this.minecraft.level, sequence -> {
            BlockHitResult hit = pResult;
            InteractionResult result = InteractionResult.FAIL;
            int repeat = ClickCommonConfig.getRepeat(pPlayer);
            for (int count = 0; count < repeat; count++) {
                InteractionResult current = this.performUseItemOn(pPlayer, pHand, hit);
                if (current.ordinal() < result.ordinal()) {
                    result = current;
                }

                double reach = Math.max(pPlayer.getEntityReach(), this.getPickRange());
                if (pPlayer.pick(reach, 1, false) instanceof BlockHitResult bhr && bhr.getType() != HitResult.Type.MISS) {
                    hit = bhr;
                } else {
                    break;
                }
            }
            mutableobject.setValue(result);
            return new ServerboundUseItemOnPacket(pHand, pResult, sequence);
        });
        cir.setReturnValue(mutableobject.getValue());
    }

    @Inject(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V"), cancellable = true)
    private void useItemMixin(Player pPlayer, InteractionHand pHand, CallbackInfoReturnable<InteractionResult> cir) {
        MutableObject<InteractionResult> mutableobject = new MutableObject<>();
        this.startPrediction(this.minecraft.level, (sequence) -> {
            ServerboundUseItemPacket serverbounduseitempacket = new ServerboundUseItemPacket(pHand, sequence);
            ItemStack itemstack = pPlayer.getItemInHand(pHand);
            if (pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
                mutableobject.setValue(InteractionResult.PASS);
            } else {
                InteractionResult result = InteractionResult.FAIL;
                int repeat = ClickCommonConfig.getRepeat(pPlayer);
                for (int count = 0; count < repeat; count++) {
                    if (pPlayer.isUsingItem()) {
                        ItemStack stack = pPlayer.getItemInHand(pHand);
                        ((LivingEntityAccessor) pPlayer).invokeUpdateUsingItem(stack);
                        if (count != 0) {
                            int perRepeating = ClickCommonConfig.getRelease(stack.getItem());
                            if (perRepeating > 0 && count % perRepeating == 0) {
                                pPlayer.releaseUsingItem();
                            }
                        }
                    } else {
                        InteractionResult cancelResult = ForgeHooks.onItemRightClick(pPlayer, pHand);
                        if (cancelResult != null) {
                            if (cancelResult.ordinal() < result.ordinal()) {
                                result = cancelResult;
                            }
                            continue;
                        }
                        InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(pPlayer.level(), pPlayer, pHand);
                        ItemStack itemstack1 = interactionresultholder.getObject();
                        if (itemstack1 != itemstack) {
                            pPlayer.setItemInHand(pHand, itemstack1);
                            if (itemstack1.isEmpty()) {
                                ForgeEventFactory.onPlayerDestroyItem(pPlayer, itemstack, pHand);
                            }
                        }

                        if (interactionresultholder.getResult().ordinal() < result.ordinal()) {
                            result = interactionresultholder.getResult();
                        }
                    }
                }

                mutableobject.setValue(result);
            }
            return serverbounduseitempacket;
        });
        cir.setReturnValue(mutableobject.getValue());
    }
}
