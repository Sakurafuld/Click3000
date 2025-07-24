package com.sakurafuld.click3000;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Click3000.ID)
public class Click3000 {
    public static final String ID = "click3000";

    @SuppressWarnings("removal")
    public Click3000() {
        this(FMLJavaModLoadingContext.get());
    }

    public Click3000(FMLJavaModLoadingContext context) {

        context.registerConfig(ModConfig.Type.COMMON, Click3000CommonConfig.SPEC, ID + "-common.toml");

    }
}
