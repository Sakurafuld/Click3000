package com.sakurafuld.click3000.network;

import com.sakurafuld.click3000.Click3000;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ClickConnection {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE
            = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Click3000.ID, "network"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void initialize() {
        int id = 0;

        INSTANCE.registerMessage(id++, ClientboundClickSyncSeed.class, ClientboundClickSyncSeed::encode, ClientboundClickSyncSeed::decode, ClientboundClickSyncSeed::handle);
    }
}
