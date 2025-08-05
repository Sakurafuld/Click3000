package com.sakurafuld.click3000;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

public class ClickCommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue REPEAT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RELEASE;
    public static final ForgeConfigSpec.BooleanValue RANDOM;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RANDOM_REPETITION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Click3000");
        {
            REPEAT = builder
                    .defineInRange("Click count", 3000, 0, Integer.MAX_VALUE);
            RELEASE = builder
                    .defineList("Release per repeating", List.of("minecraft:bow=20", "minecraft:trident=20", "minecraft:crossbow=30", "botania:crystal_bow=20", "botania:slingshot=30", "avaritia:infinity_bow=20"), object -> {
                        if (object instanceof String string) {
                            if (string.startsWith("#")) {
                                string = string.substring(1);
                            }

                            String[] split = string.split("=");
                            return ResourceLocation.isValidResourceLocation(split[0]) && split[1].chars().allMatch(Character::isDigit);
                        } else {
                            return false;
                        }
                    });
            RANDOM = builder
                    .comment("Perform the number of clicks randomly selected from Weighted click counts config")
                    .define("Randomize click count", false);
            RANDOM_REPETITION = builder
                    .comment("Write the number of clicks on the left side of @ and the weight on the right side",
                            "Default: [\"1@10\", \"20@5\", \"0@5\", \"3000@1\"] ( = 1 click for 10/21, 20 clicks for 5/21, 0 click for 5/21, and 3000 clicks for 1/21 )")
                    .defineList("Weighted click counts", List.of("1@10", "20@5", "0@5", "3000@1"), object -> {
                        if (object instanceof String string) {
                            String[] split = string.split("@");
                            return split.length == 2 && split[0].chars().allMatch(Character::isDigit) && split[1].chars().allMatch(Character::isDigit);
                        } else {
                            return false;
                        }
                    });
        }
        builder.pop();

        SPEC = builder.build();
    }

    public static final ThreadLocal<Object2LongOpenHashMap<UUID>> PLAYER_SEEDS = ThreadLocal.withInitial(Object2LongOpenHashMap::new);
    private static SimpleWeightedRandomList<Integer> weightedRepetition = SimpleWeightedRandomList.<Integer>builder().build();
    private static final Object2IntOpenHashMap<ResourceLocation> ITEMS = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<TagKey<Item>> TAGS = new Object2IntOpenHashMap<>();

    public static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getModId().equals(Click3000.ID) && event.getConfig().getType() == ModConfig.Type.COMMON) {
            loadRandomRepetition();
            loadRelease();
        }
    }

    public static int getRepeat(Player player) {
        if (RANDOM.get()) {
            long seed = PLAYER_SEEDS.get().getOrDefault(player.getUUID(), Click3000.RANDOM.nextLong(Long.MIN_VALUE, 0));
            int ret = weightedRepetition.getRandomValue(RandomSource.create(seed)).orElse(1);

            Click3000.LOG.debug("repeat!:{}", weightedRepetition.unwrap().stream()
                    .map(wrapper -> Pair.of(wrapper.getData(), wrapper.getWeight()))
                    .toList());
            Click3000.LOG.debug("repeatSeed!:{}", seed);
            Click3000.LOG.debug("repeatRand!:{}", ret);

            PLAYER_SEEDS.get().put(player.getUUID(), ++seed);
            return ret;
        } else {
            return REPEAT.get();
        }
    }

    public static int getRelease(Item item) {
        ResourceLocation identifier = ForgeRegistries.ITEMS.getKey(item);
        if (ITEMS.containsKey(identifier)) {
            return ITEMS.getInt(identifier);
        } else {
            for (Object2IntMap.Entry<TagKey<Item>> entry : TAGS.object2IntEntrySet()) {
                if (item.builtInRegistryHolder().is(entry.getKey())) {
                    return entry.getIntValue();
                }
            }

            return 0;
        }
    }

    private static void loadRandomRepetition() {
        SimpleWeightedRandomList.Builder<Integer> builder = SimpleWeightedRandomList.builder();
        RANDOM_REPETITION.get().stream()
                .map(String.class::cast)
                .forEach(string -> {
                    String[] split = string.split("@");
                    int count = Integer.parseInt(split[0]);
                    int weight = Integer.parseInt(split[1]);
                    builder.add(count, weight);
                });
        weightedRepetition = builder.build();
    }

    private static void loadRelease() {
        ITEMS.clear();
        TAGS.clear();
        RELEASE.get().stream()
                .map(String.class::cast)
                .forEach(string -> {
                    boolean tag = string.startsWith("#");
                    if (tag) {
                        string = string.substring(1);
                    }

                    String[] split = string.split("=");
                    ResourceLocation identifier = ResourceLocation.parse(split[0]);
                    int value = Integer.parseInt(split[1]);
                    if (tag) {
                        TAGS.put(ItemTags.create(identifier), value);
                    } else {
                        ITEMS.put(identifier, value);
                    }
                });
    }
}
