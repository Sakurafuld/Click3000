package com.sakurafuld.click3000;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Click3000.ID)
public class Click3000CommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue REPEAT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RELEASE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Click3000");
        {
            REPEAT = builder
                    .defineInRange("Click count", 3000, 0, Integer.MAX_VALUE);
            RELEASE = builder
                    .defineList("Release per repeating", List.of("minecraft:bow=20", "minecraft:trident=20", "minecraft:crossbow=30", "botania:crystal_bow=20", "botania:slingshot=30", "avaritia:infinity_bow=20"), object -> object instanceof String string && validateRelease(string));
        }
        builder.pop();

        SPEC = builder.build();
    }

    private static boolean validateRelease(String string) {
        if (string.startsWith("#")) {
            string = string.substring(1);
        }

        String[] split = string.split("=");
        return ResourceLocation.isValidResourceLocation(split[0]) && split[1].chars().allMatch(Character::isDigit);
    }
}
