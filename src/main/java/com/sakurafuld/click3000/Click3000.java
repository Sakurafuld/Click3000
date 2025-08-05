package com.sakurafuld.click3000;

import com.sakurafuld.click3000.network.ClickConnection;
import com.sakurafuld.click3000.network.ClientboundClickSyncSeed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

@Mod(Click3000.ID)
public class Click3000 {
    public static final String ID = "click3000";
    public static final Logger LOG = LoggerFactory.getLogger(ID);
    public static final Random RANDOM = new Random();

    @SuppressWarnings("removal")
    public Click3000() {
        this(FMLJavaModLoadingContext.get());
    }

    public Click3000(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();

        bus.addListener(ClickCommonConfig::configChanged);
        bus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.addListener(this::logIn);
        MinecraftForge.EVENT_BUS.addListener(this::tick);

        context.registerConfig(ModConfig.Type.COMMON, ClickCommonConfig.SPEC, ID + "-common.toml");
    }

    public void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(ClickConnection::initialize);
    }

    public void logIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClickConnection.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundClickSyncSeed(ClickCommonConfig.PLAYER_SEEDS.get().getOrDefault(player.getUUID(), RANDOM.nextLong(Long.MIN_VALUE, 0))));
        }
    }

    public void tick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player && player.level().getGameTime() % 20 == 0) {
            ClickConnection.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ClientboundClickSyncSeed(ClickCommonConfig.PLAYER_SEEDS.get().getOrDefault(player.getUUID(), RANDOM.nextLong(Long.MIN_VALUE, 0))));
        }
    }
}
