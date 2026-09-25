package com.istiak.equinox.items;

import com.istiak.equinox.EquinoxMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Map;

/**
 * Survival-progression loot distribution (tiered):
 *
 * OVERWORLD basic structures (desert temple, dungeons, jungle temple, ...):
 *   mostly leather + iron armor, golden uncommon, diamond VERY rare.
 *   Books: levels 1-3, level 1 most common.
 *
 * OVERWORLD rich structures (stronghold, mansion, trial chambers):
 *   balanced leather/iron/golden, diamond rare.
 *   Books: levels 2-4.
 *
 * VILLAGE smith chests: leather/iron/golden only, no diamond. Books 1-2.
 *
 * NETHER (fortress, bastions, ancient city): diamond is the common find here,
 *   netherite only appears in bastion treasure + ancient city.
 *   Books: levels 2-4.
 *
 * TREASURE chests (buried treasure, shipwreck treasure, bastion treasure):
 *   the place for HIGH-LEVEL books (III-V, level V most common there).
 *
 * Trades (data/equinox/villager_trade/...): armorer sells the armor tiers,
 * librarian sells the books - independent of these chest tables.
 */
public final class EquinoxLoot {

    private EquinoxLoot() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (source.isBuiltin()) {
                injectLoot(key.identifier().getPath(), builder);
            }
        });
    }

    private static void injectLoot(String path, LootTable.Builder builder) {
        switch (path) {
            // ==========================================================
            // OVERWORLD - basic exploration (leather/iron focus)
            // ==========================================================
            case "chests/simple_dungeon" -> {
                builder.withPool(armorPool(2, W_BASIC));
                builder.withPool(bookPool(0.35f, 1, 3, false));
            }
            case "chests/desert_pyramid" -> {
                builder.withPool(armorPool(2, W_BASIC));
                builder.withPool(bookPool(0.30f, 1, 3, false));
            }
            case "chests/jungle_temple" -> {
                builder.withPool(armorPool(2, W_BASIC));
                builder.withPool(bookPool(0.30f, 1, 3, false));
            }
            case "chests/igloo_chest" -> {
                builder.withPool(armorPool(1, W_BASIC));
                builder.withPool(bookPool(0.25f, 1, 2, false));
            }
            case "chests/ruined_portal" -> {
                builder.withPool(armorPool(1, W_BASIC));
                builder.withPool(bookPool(0.22f, 1, 3, false));
            }
            case "chests/abandoned_mineshaft" -> {
                builder.withPool(armorPool(1, W_BASIC));
                builder.withPool(bookPool(0.15f, 1, 2, false));
            }
            case "chests/underwater_ruin_small", "chests/underwater_ruin_big" -> {
                builder.withPool(armorPool(1, W_BASIC));
                builder.withPool(bookPool(0.18f, 1, 2, false));
            }
            case "chests/pillager_outpost" -> {
                builder.withPool(armorPool(2, W_RICH));
                builder.withPool(bookPool(0.22f, 1, 3, false));
            }

            // ==========================================================
            // OVERWORLD - rich/endgame structures (golden up, rare diamond)
            // ==========================================================
            case "chests/stronghold_crossing", "chests/stronghold_corridor" -> {
                builder.withPool(armorPool(2, W_RICH));
                builder.withPool(bookPool(0.28f, 2, 4, false));
            }
            case "chests/woodland_mansion" -> {
                builder.withPool(armorPool(3, W_RICH));
                builder.withPool(bookPool(0.35f, 2, 4, false));
            }
            case "chests/trial_chambers/reward" -> {
                builder.withPool(armorPool(2, W_RICH));
                builder.withPool(bookPool(0.30f, 2, 4, false));
            }
            case "chests/trial_chambers/corridor" -> {
                builder.withPool(armorPool(1, W_BASIC));
                builder.withPool(bookPool(0.20f, 1, 3, false));
            }
            case "chests/trial_chambers/intersection" -> {
                builder.withPool(armorPool(1, W_RICH));
                builder.withPool(bookPool(0.25f, 2, 3, false));
            }

            // ==========================================================
            // TREASURE chests - high-level enchant books live here
            // ==========================================================
            case "chests/buried_treasure" -> {
                builder.withPool(armorPool(2, W_TREASURE));
                builder.withPool(bookPool(0.45f, 3, 5, true));
            }
            case "chests/shipwreck_treasure" -> {
                builder.withPool(armorPool(2, W_TREASURE));
                builder.withPool(bookPool(0.35f, 3, 5, true));
            }

            // ==========================================================
            // VILLAGE smith chests - leather/iron/golden only, basic books
            // ==========================================================
            case "chests/village/village_armorer",
                 "chests/village/village_toolsmith",
                 "chests/village/village_weaponsmith" -> {
                builder.withPool(armorPool(1, W_VILLAGE));
                builder.withPool(bookPool(0.22f, 1, 2, false));
            }

            // ==========================================================
            // NETHER - diamond territory (netherite only in treasure)
            // ==========================================================
            case "chests/nether_bridge" -> {
                builder.withPool(armorPool(1, W_NETHER));
                builder.withPool(bookPool(0.25f, 2, 4, false));
            }
            case "chests/bastion_bridge" -> {
                builder.withPool(armorPool(2, W_NETHER));
                builder.withPool(bookPool(0.32f, 2, 4, false));
            }
            case "chests/bastion_hoglin_stable", "chests/bastion_other" -> {
                builder.withPool(armorPool(2, W_NETHER));
                builder.withPool(bookPool(0.28f, 2, 4, false));
            }
            case "chests/bastion_treasure" -> {
                builder.withPool(armorPool(2, W_NETHER_TREASURE));
                builder.withPool(bookPool(0.45f, 3, 5, true));
            }
            case "chests/ancient_city", "chests/ancient_city_ice_box" -> {
                builder.withPool(armorPool(2, W_NETHER_TREASURE));
                builder.withPool(bookPool(0.35f, 3, 5, true));
            }

            default -> {
            }
        }
    }

    // Armor weight tables: {leather, iron, golden, diamond, netherite}
    private static final int[] W_BASIC = {55, 30, 12, 3, 0};
    private static final int[] W_RICH = {30, 35, 25, 10, 0};
    private static final int[] W_VILLAGE = {45, 35, 20, 0, 0};
    private static final int[] W_TREASURE = {35, 35, 22, 8, 0};
    private static final int[] W_NETHER = {0, 30, 35, 35, 0};
    private static final int[] W_NETHER_TREASURE = {0, 25, 30, 40, 5};

    /**
     * One roll of 1..{@code maxCount} armors with the given tier weights.
     */
    private static LootPool.Builder armorPool(int maxCount, int[] w) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(maxCount > 1
                        ? UniformGenerator.between(1.0f, (float) maxCount)
                        : ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(EquinoxItems.LEATHER_EQUINOX_ARMOR).setWeight(w[0]))
                .add(LootItem.lootTableItem(EquinoxItems.IRON_EQUINOX_ARMOR).setWeight(w[1]))
                .add(LootItem.lootTableItem(EquinoxItems.GOLDEN_EQUINOX_ARMOR).setWeight(w[2]))
                .add(LootItem.lootTableItem(EquinoxItems.DIAMOND_EQUINOX_ARMOR).setWeight(w[3]));
        if (w[4] > 0) {
            pool.add(LootItem.lootTableItem(EquinoxItems.NETHERITE_EQUINOX_ARMOR).setWeight(w[4]));
        }
        return pool;
    }

    /**
     * Chance-based book pool over levels [{@code minLevel}..{@code maxLevel}].
     * {@code highTier} inverts the level bias so level V books are the most
     * common outcome inside treasure chests (and rare everywhere else).
     */
    private static LootPool.Builder bookPool(float chance, int minLevel, int maxLevel, boolean highTier) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .when(LootItemRandomChanceCondition.randomChance(chance));

        for (Map.Entry<Item, EquinoxItems.BookEnchant> e : EquinoxItems.BOOK_ENCHANTS.entrySet()) {
            int level = e.getValue().level();
            if (level < minLevel || level > maxLevel) {
                continue;
            }
            int typeWeight = switch (e.getValue().type()) {
                case SWIFT -> 3;
                case TITAN_LEAP -> 2;
                case VITALITY -> 2;
            };
            int levelWeight = highTier ? level : Math.max(1, 6 - level);
            pool.add(LootItem.lootTableItem(e.getKey()).setWeight(typeWeight * levelWeight));
        }
        return pool;
    }
}
