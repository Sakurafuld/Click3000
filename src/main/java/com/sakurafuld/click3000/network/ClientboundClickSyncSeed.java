package com.sakurafuld.click3000.network;

import com.sakurafuld.click3000.ClickCommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


public class ClientboundClickSyncSeed {
    private final long seed;

    public ClientboundClickSyncSeed(long seed) {
        this.seed = seed;
    }

    public static void encode(ClientboundClickSyncSeed msg, FriendlyByteBuf buf) {
        buf.writeVarLong(msg.seed);
    }

    public static ClientboundClickSyncSeed decode(FriendlyByteBuf buf) {
        return new ClientboundClickSyncSeed(buf.readVarLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::handle));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handle() {
        ClickCommonConfig.PLAYER_SEEDS.get().put(Minecraft.getInstance().player.getUUID(), this.seed);
    }
}
