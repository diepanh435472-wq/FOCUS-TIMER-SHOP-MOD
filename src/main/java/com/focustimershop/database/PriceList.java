package com.focustimershop.database;

import java.util.HashMap;
import java.util.Map;

/**
 * Price list for shop items
 * Key = Minecraft item ID (e.g. "minecraft:stone")
 * Value = Price in Silver coins
 */
public class PriceList {
	
	private String version = "1.0.2";
	private String category;  // "building_blocks", "colored_blocks", etc.
	private Map<String, Integer> prices = new HashMap<>();
	
	public static PriceList createBuildingBlocksDefaults() {
		PriceList list = new PriceList();
		list.category = "building_blocks";
		
		// ===== REBALANCED PRICES - Based on Real Minecraft Gathering Time =====
		// Logic: Price = How long it takes to get this block in survival
		// - Very Easy (<30s): 1 silver (chop wood, mine stone)
		// - Easy (1-2min): 2-3 silver (craft, smelt)
		// - Medium (5-10min): 5-8 silver (find rare biome, deep mine)
		// - Hard (10-20min): 10-15 silver (Nether, ocean monument)
		// - Very Hard (20-30min): 15-25 silver (obsidian, quartz mining)
		// - Rare minerals: Based on ore rarity (iron=25, gold=45, diamond=100, netherite=250)
		
		// === WOOD FAMILY (1 silver = chop tree ~10 seconds) ===
		String[] woodTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry"};
		for (String wood : woodTypes) {
			list.prices.put("minecraft:" + wood + "_log", 1);
			list.prices.put("minecraft:" + wood + "_wood", 1);
			list.prices.put("minecraft:stripped_" + wood + "_log", 1);
			list.prices.put("minecraft:stripped_" + wood + "_wood", 1);
			list.prices.put("minecraft:" + wood + "_planks", 1);
			list.prices.put("minecraft:" + wood + "_stairs", 1);
			list.prices.put("minecraft:" + wood + "_slab", 1);
			list.prices.put("minecraft:" + wood + "_fence", 1);
			list.prices.put("minecraft:" + wood + "_fence_gate", 1);
		}
		
		// Bamboo family (same as wood)
		list.prices.put("minecraft:bamboo_block", 1);
		list.prices.put("minecraft:stripped_bamboo_block", 1);
		list.prices.put("minecraft:bamboo_planks", 1);
		list.prices.put("minecraft:bamboo_mosaic", 1);
		list.prices.put("minecraft:bamboo_stairs", 1);
		list.prices.put("minecraft:bamboo_mosaic_stairs", 1);
		list.prices.put("minecraft:bamboo_slab", 1);
		list.prices.put("minecraft:bamboo_mosaic_slab", 1);
		list.prices.put("minecraft:bamboo_fence", 1);
		list.prices.put("minecraft:bamboo_fence_gate", 1);
		
		// Crimson & Warped (Nether wood) - 10 silver (need portal, travel to Nether)
		list.prices.put("minecraft:crimson_stem", 10);
		list.prices.put("minecraft:crimson_hyphae", 10);
		list.prices.put("minecraft:stripped_crimson_stem", 10);
		list.prices.put("minecraft:stripped_crimson_hyphae", 10);
		list.prices.put("minecraft:crimson_planks", 10);
		list.prices.put("minecraft:crimson_stairs", 10);
		list.prices.put("minecraft:crimson_slab", 5);
		list.prices.put("minecraft:crimson_fence", 10);
		list.prices.put("minecraft:crimson_fence_gate", 10);
		
		list.prices.put("minecraft:warped_stem", 10);
		list.prices.put("minecraft:warped_hyphae", 10);
		list.prices.put("minecraft:stripped_warped_stem", 10);
		list.prices.put("minecraft:stripped_warped_hyphae", 10);
		list.prices.put("minecraft:warped_planks", 10);
		list.prices.put("minecraft:warped_stairs", 10);
		list.prices.put("minecraft:warped_slab", 5);
		list.prices.put("minecraft:warped_fence", 10);
		list.prices.put("minecraft:warped_fence_gate", 10);
		
		// === STONE FAMILY (1 silver = mine ~5 seconds) ===
		list.prices.put("minecraft:stone", 1);
		list.prices.put("minecraft:smooth_stone", 2);  // Need to smelt
		list.prices.put("minecraft:smooth_stone_slab", 1);
		list.prices.put("minecraft:cobblestone", 1);
		list.prices.put("minecraft:cobblestone_stairs", 1);
		list.prices.put("minecraft:cobblestone_slab", 1);
		list.prices.put("minecraft:cobblestone_wall", 1);
		list.prices.put("minecraft:mossy_cobblestone", 2);  // Need to find or craft
		list.prices.put("minecraft:mossy_cobblestone_stairs", 2);
		list.prices.put("minecraft:mossy_cobblestone_slab", 1);
		list.prices.put("minecraft:mossy_cobblestone_wall", 2);
		
		list.prices.put("minecraft:stone_bricks", 2);
		list.prices.put("minecraft:stone_brick_stairs", 2);
		list.prices.put("minecraft:stone_brick_slab", 1);
		list.prices.put("minecraft:stone_brick_wall", 2);
		list.prices.put("minecraft:mossy_stone_bricks", 3);
		list.prices.put("minecraft:mossy_stone_brick_stairs", 3);
		list.prices.put("minecraft:mossy_stone_brick_slab", 2);
		list.prices.put("minecraft:mossy_stone_brick_wall", 3);
		list.prices.put("minecraft:cracked_stone_bricks", 2);
		list.prices.put("minecraft:chiseled_stone_bricks", 2);
		list.prices.put("minecraft:infested_stone_bricks", 3);
		
		// Granite, Diorite, Andesite (common underground)
		list.prices.put("minecraft:granite", 1);
		list.prices.put("minecraft:granite_stairs", 1);
		list.prices.put("minecraft:granite_slab", 1);
		list.prices.put("minecraft:granite_wall", 1);
		list.prices.put("minecraft:polished_granite", 2);
		list.prices.put("minecraft:polished_granite_stairs", 2);
		list.prices.put("minecraft:polished_granite_slab", 1);
		
		list.prices.put("minecraft:diorite", 1);
		list.prices.put("minecraft:diorite_stairs", 1);
		list.prices.put("minecraft:diorite_slab", 1);
		list.prices.put("minecraft:diorite_wall", 1);
		list.prices.put("minecraft:polished_diorite", 2);
		list.prices.put("minecraft:polished_diorite_stairs", 2);
		list.prices.put("minecraft:polished_diorite_slab", 1);
		
		list.prices.put("minecraft:andesite", 1);
		list.prices.put("minecraft:andesite_stairs", 1);
		list.prices.put("minecraft:andesite_slab", 1);
		list.prices.put("minecraft:andesite_wall", 1);
		list.prices.put("minecraft:polished_andesite", 2);
		list.prices.put("minecraft:polished_andesite_stairs", 2);
		list.prices.put("minecraft:polished_andesite_slab", 1);
		
		// === DEEPSLATE FAMILY (5 silver = deep mine Y<0, slower to break) ===
		list.prices.put("minecraft:deepslate", 5);
		list.prices.put("minecraft:cobbled_deepslate", 5);
		list.prices.put("minecraft:cobbled_deepslate_stairs", 5);
		list.prices.put("minecraft:cobbled_deepslate_slab", 3);
		list.prices.put("minecraft:cobbled_deepslate_wall", 5);
		list.prices.put("minecraft:polished_deepslate", 6);
		list.prices.put("minecraft:polished_deepslate_stairs", 6);
		list.prices.put("minecraft:polished_deepslate_slab", 3);
		list.prices.put("minecraft:polished_deepslate_wall", 6);
		list.prices.put("minecraft:chiseled_deepslate", 6);
		list.prices.put("minecraft:deepslate_bricks", 6);
		list.prices.put("minecraft:deepslate_brick_stairs", 6);
		list.prices.put("minecraft:deepslate_brick_slab", 3);
		list.prices.put("minecraft:deepslate_brick_wall", 6);
		list.prices.put("minecraft:cracked_deepslate_bricks", 6);
		list.prices.put("minecraft:deepslate_tiles", 6);
		list.prices.put("minecraft:deepslate_tile_stairs", 6);
		list.prices.put("minecraft:deepslate_tile_slab", 3);
		list.prices.put("minecraft:deepslate_tile_wall", 6);
		list.prices.put("minecraft:cracked_deepslate_tiles", 6);
		
		// === BRICKS & CLAY (3 silver = need to find clay, smelt) ===
		list.prices.put("minecraft:bricks", 3);
		list.prices.put("minecraft:brick_stairs", 3);
		list.prices.put("minecraft:brick_slab", 2);
		list.prices.put("minecraft:brick_wall", 3);
		
		list.prices.put("minecraft:mud_bricks", 3);
		list.prices.put("minecraft:mud_brick_stairs", 3);
		list.prices.put("minecraft:mud_brick_slab", 2);
		list.prices.put("minecraft:mud_brick_wall", 3);
		list.prices.put("minecraft:packed_mud", 2);
		list.prices.put("minecraft:mud", 1);
		
		// === SANDSTONE FAMILY (2 silver = find desert/beach) ===
		list.prices.put("minecraft:sandstone", 2);
		list.prices.put("minecraft:sandstone_stairs", 2);
		list.prices.put("minecraft:sandstone_slab", 1);
		list.prices.put("minecraft:sandstone_wall", 2);
		list.prices.put("minecraft:smooth_sandstone", 3);
		list.prices.put("minecraft:smooth_sandstone_stairs", 3);
		list.prices.put("minecraft:smooth_sandstone_slab", 2);
		list.prices.put("minecraft:cut_sandstone", 2);
		list.prices.put("minecraft:cut_sandstone_slab", 1);
		list.prices.put("minecraft:chiseled_sandstone", 2);
		
		list.prices.put("minecraft:red_sandstone", 2);
		list.prices.put("minecraft:red_sandstone_stairs", 2);
		list.prices.put("minecraft:red_sandstone_slab", 1);
		list.prices.put("minecraft:red_sandstone_wall", 2);
		list.prices.put("minecraft:smooth_red_sandstone", 3);
		list.prices.put("minecraft:smooth_red_sandstone_stairs", 3);
		list.prices.put("minecraft:smooth_red_sandstone_slab", 2);
		list.prices.put("minecraft:cut_red_sandstone", 2);
		list.prices.put("minecraft:cut_red_sandstone_slab", 1);
		list.prices.put("minecraft:chiseled_red_sandstone", 2);
		
		// === PRISMARINE FAMILY (12 silver = find ocean monument, hard to get) ===
		list.prices.put("minecraft:prismarine", 12);
		list.prices.put("minecraft:prismarine_stairs", 12);
		list.prices.put("minecraft:prismarine_slab", 6);
		list.prices.put("minecraft:prismarine_wall", 12);
		list.prices.put("minecraft:prismarine_bricks", 12);
		list.prices.put("minecraft:prismarine_brick_stairs", 12);
		list.prices.put("minecraft:prismarine_brick_slab", 6);
		list.prices.put("minecraft:dark_prismarine", 12);
		list.prices.put("minecraft:dark_prismarine_stairs", 12);
		list.prices.put("minecraft:dark_prismarine_slab", 6);
		list.prices.put("minecraft:sea_lantern", 15);
		
		// === NETHER FAMILY (10 silver = need portal, Nether travel, mining) ===
		list.prices.put("minecraft:nether_bricks", 10);
		list.prices.put("minecraft:nether_brick_stairs", 10);
		list.prices.put("minecraft:nether_brick_slab", 5);
		list.prices.put("minecraft:nether_brick_wall", 10);
		list.prices.put("minecraft:nether_brick_fence", 10);
		list.prices.put("minecraft:chiseled_nether_bricks", 10);
		list.prices.put("minecraft:cracked_nether_bricks", 10);
		
		list.prices.put("minecraft:red_nether_bricks", 10);
		list.prices.put("minecraft:red_nether_brick_stairs", 10);
		list.prices.put("minecraft:red_nether_brick_slab", 5);
		list.prices.put("minecraft:red_nether_brick_wall", 10);
		
		list.prices.put("minecraft:blackstone", 10);
		list.prices.put("minecraft:blackstone_stairs", 10);
		list.prices.put("minecraft:blackstone_slab", 5);
		list.prices.put("minecraft:blackstone_wall", 10);
		list.prices.put("minecraft:polished_blackstone", 10);
		list.prices.put("minecraft:polished_blackstone_stairs", 10);
		list.prices.put("minecraft:polished_blackstone_slab", 5);
		list.prices.put("minecraft:polished_blackstone_wall", 10);
		list.prices.put("minecraft:chiseled_polished_blackstone", 10);
		list.prices.put("minecraft:polished_blackstone_bricks", 10);
		list.prices.put("minecraft:polished_blackstone_brick_stairs", 10);
		list.prices.put("minecraft:polished_blackstone_brick_slab", 5);
		list.prices.put("minecraft:polished_blackstone_brick_wall", 10);
		list.prices.put("minecraft:cracked_polished_blackstone_bricks", 10);
		list.prices.put("minecraft:gilded_blackstone", 12);
		
		list.prices.put("minecraft:basalt", 8);
		list.prices.put("minecraft:polished_basalt", 8);
		list.prices.put("minecraft:smooth_basalt", 8);
		
		list.prices.put("minecraft:netherrack", 6);  // Very common in Nether
		list.prices.put("minecraft:magma_block", 8);
		list.prices.put("minecraft:glowstone", 10);
		list.prices.put("minecraft:soul_sand", 7);
		list.prices.put("minecraft:soul_soil", 7);
		
		// === END FAMILY (15 silver = need to beat dragon, very hard to get) ===
		list.prices.put("minecraft:end_stone", 15);
		list.prices.put("minecraft:end_stone_bricks", 15);
		list.prices.put("minecraft:end_stone_brick_stairs", 15);
		list.prices.put("minecraft:end_stone_brick_slab", 8);
		list.prices.put("minecraft:end_stone_brick_wall", 15);
		list.prices.put("minecraft:purpur_block", 15);
		list.prices.put("minecraft:purpur_pillar", 15);
		list.prices.put("minecraft:purpur_stairs", 15);
		list.prices.put("minecraft:purpur_slab", 8);
		
		// === QUARTZ FAMILY (12 silver = mine in Nether, takes time) ===
		list.prices.put("minecraft:quartz_block", 12);
		list.prices.put("minecraft:quartz_stairs", 12);
		list.prices.put("minecraft:quartz_slab", 6);
		list.prices.put("minecraft:smooth_quartz", 12);
		list.prices.put("minecraft:smooth_quartz_stairs", 12);
		list.prices.put("minecraft:smooth_quartz_slab", 6);
		list.prices.put("minecraft:quartz_bricks", 12);
		list.prices.put("minecraft:quartz_pillar", 12);
		list.prices.put("minecraft:chiseled_quartz_block", 12);
		
		// === COPPER FAMILY (8 silver = mine copper ore, not super rare) ===
		String[] copperStates = {"", "exposed_", "weathered_", "oxidized_"};
		for (String state : copperStates) {
			list.prices.put("minecraft:" + state + "copper", 8);
			list.prices.put("minecraft:" + state + "cut_copper", 8);
			list.prices.put("minecraft:" + state + "cut_copper_stairs", 8);
			list.prices.put("minecraft:" + state + "cut_copper_slab", 4);
			list.prices.put("minecraft:waxed_" + state + "copper", 9);
			list.prices.put("minecraft:waxed_" + state + "cut_copper", 9);
			list.prices.put("minecraft:waxed_" + state + "cut_copper_stairs", 9);
			list.prices.put("minecraft:waxed_" + state + "cut_copper_slab", 5);
		}
		
		// === MISC BUILDING BLOCKS ===
		list.prices.put("minecraft:calcite", 3);
		list.prices.put("minecraft:tuff", 3);
		list.prices.put("minecraft:dripstone_block", 5);
		list.prices.put("minecraft:pointed_dripstone", 3);
		
		list.prices.put("minecraft:amethyst_block", 20);  // Rare geode
		list.prices.put("minecraft:budding_amethyst", 40);  // Cannot be obtained legit
		
		list.prices.put("minecraft:moss_block", 5);
		list.prices.put("minecraft:moss_carpet", 2);
		
		list.prices.put("minecraft:clay", 2);
		list.prices.put("minecraft:packed_ice", 4);
		list.prices.put("minecraft:blue_ice", 8);
		list.prices.put("minecraft:snow_block", 2);
		
		list.prices.put("minecraft:honeycomb_block", 10);
		list.prices.put("minecraft:slime_block", 12);  // Hard to get slime
		list.prices.put("minecraft:honey_block", 10);
		
		list.prices.put("minecraft:obsidian", 20);  // Need diamond pickaxe, water+lava
		list.prices.put("minecraft:crying_obsidian", 25);  // Only in Nether
		
		// === MINERAL BLOCKS (based on ore rarity) ===
		list.prices.put("minecraft:coal_block", 5);   // Common
		list.prices.put("minecraft:raw_iron_block", 20);
		list.prices.put("minecraft:raw_copper_block", 12);
		list.prices.put("minecraft:raw_gold_block", 30);
		list.prices.put("minecraft:iron_block", 25);  // Common ore
		list.prices.put("minecraft:copper_block", 12);
		list.prices.put("minecraft:gold_block", 45);  // Rare ore
		list.prices.put("minecraft:lapis_block", 35);
		list.prices.put("minecraft:redstone_block", 30);
		list.prices.put("minecraft:emerald_block", 80);  // Very rare
		list.prices.put("minecraft:diamond_block", 100); // Extremely rare (was 320)
		list.prices.put("minecraft:netherite_block", 250); // Ultra rare (was 900)
		
		return list;
	}
	
	public static PriceList createColoredBlocksDefaults() {
		PriceList list = new PriceList();
		list.category = "colored_blocks";
		
		// Wool (all colors) - 3 silver (need sheep, easy to farm)
		String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", 
		                   "pink", "gray", "light_gray", "cyan", "purple", "blue", 
		                   "brown", "green", "red", "black"};
		
		for (String color : colors) {
			list.prices.put("minecraft:" + color + "_wool", 3);
			list.prices.put("minecraft:" + color + "_carpet", 1);
			list.prices.put("minecraft:" + color + "_terracotta", 4);  // Need clay + smelt
			list.prices.put("minecraft:" + color + "_concrete", 5);  // Need gravel + sand + dye
			list.prices.put("minecraft:" + color + "_concrete_powder", 4);
			list.prices.put("minecraft:" + color + "_glazed_terracotta", 6);  // Smelt colored terracotta
			list.prices.put("minecraft:" + color + "_stained_glass", 5);  // Glass + dye
			list.prices.put("minecraft:" + color + "_stained_glass_pane", 2);
			list.prices.put("minecraft:" + color + "_shulker_box", 80);  // Very rare (need End + shulker shell)
			list.prices.put("minecraft:" + color + "_bed", 5);
			list.prices.put("minecraft:" + color + "_candle", 3);
			list.prices.put("minecraft:" + color + "_banner", 5);
		}
		
		// Plain terracotta
		list.prices.put("minecraft:terracotta", 3);
		
		// Candle (no color)
		list.prices.put("minecraft:candle", 3);
		
		return list;
	}
	
	public static PriceList createNaturalBlocksDefaults() {
		PriceList list = new PriceList();
		list.category = "natural_blocks";
		
		// ===== NATURAL BLOCKS PRICING - Based on Gathering Time =====
		// Tier 0 (1-2 silver): Surface, instant mine
		// Tier 1 (3-5 silver): Easy to find/farm
		// Tier 2 (6-10 silver): Need specific biome/conditions
		// Tier 3 (15-25 silver): Common ores (coal, iron, copper)
		// Tier 4 (30-50 silver): Rare ores (gold, lapis, redstone)
		// Tier 5 (80-120 silver): Very rare (diamond, emerald)
		// Tier 6 (200+ silver): Ultra rare (ancient debris)
		
		// === DIRT & GRASS (1 silver = instant mine) ===
		list.prices.put("minecraft:dirt", 1);
		list.prices.put("minecraft:coarse_dirt", 1);
		list.prices.put("minecraft:rooted_dirt", 1);
		list.prices.put("minecraft:grass_block", 1);
		list.prices.put("minecraft:podzol", 2);  // Need silk touch
		list.prices.put("minecraft:mycelium", 3);  // Rare biome
		list.prices.put("minecraft:dirt_path", 1);
		
		// === SAND & GRAVEL (1 silver = beach/river) ===
		list.prices.put("minecraft:sand", 1);
		list.prices.put("minecraft:red_sand", 2);  // Badlands only
		list.prices.put("minecraft:gravel", 1);
		list.prices.put("minecraft:suspicious_sand", 5);  // Desert temples
		list.prices.put("minecraft:suspicious_gravel", 5);  // Underwater ruins
		
		// === CLAY (2 silver = underwater) ===
		list.prices.put("minecraft:clay", 2);
		
		// === ICE & SNOW (2-4 silver = cold biomes) ===
		list.prices.put("minecraft:ice", 2);
		list.prices.put("minecraft:packed_ice", 4);
		list.prices.put("minecraft:blue_ice", 8);  // Already in building_blocks but repeat for completeness
		list.prices.put("minecraft:snow", 1);
		list.prices.put("minecraft:snow_block", 2);
		list.prices.put("minecraft:powder_snow", 3);
		
		// === COMMON ORES (15-25 silver = underground mining) ===
		list.prices.put("minecraft:coal_ore", 15);
		list.prices.put("minecraft:deepslate_coal_ore", 18);
		list.prices.put("minecraft:iron_ore", 20);
		list.prices.put("minecraft:deepslate_iron_ore", 25);
		list.prices.put("minecraft:copper_ore", 18);
		list.prices.put("minecraft:deepslate_copper_ore", 22);
		
		// === RARE ORES (30-50 silver = deeper + rarer) ===
		list.prices.put("minecraft:gold_ore", 35);
		list.prices.put("minecraft:deepslate_gold_ore", 40);
		list.prices.put("minecraft:lapis_ore", 30);
		list.prices.put("minecraft:deepslate_lapis_ore", 35);
		list.prices.put("minecraft:redstone_ore", 30);
		list.prices.put("minecraft:deepslate_redstone_ore", 35);
		
		// === NETHER ORES (25-35 silver = Nether required) ===
		list.prices.put("minecraft:nether_gold_ore", 30);
		list.prices.put("minecraft:nether_quartz_ore", 25);
		
		// === VERY RARE ORES (80-120 silver) ===
		list.prices.put("minecraft:diamond_ore", 100);
		list.prices.put("minecraft:deepslate_diamond_ore", 110);
		list.prices.put("minecraft:emerald_ore", 90);
		list.prices.put("minecraft:deepslate_emerald_ore", 100);
		
		// === ULTRA RARE (200+ silver) ===
		list.prices.put("minecraft:ancient_debris", 250);
		
		// === LEAVES (1 silver = chop tree) ===
		String[] leafTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "azalea", "flowering_azalea"};
		for (String leaf : leafTypes) {
			list.prices.put("minecraft:" + leaf + "_leaves", 1);
		}
		
		// === FLOWERS - COMMON (2 silver = plains/forest) ===
		list.prices.put("minecraft:dandelion", 2);
		list.prices.put("minecraft:poppy", 2);
		list.prices.put("minecraft:blue_orchid", 3);
		list.prices.put("minecraft:allium", 2);
		list.prices.put("minecraft:azure_bluet", 2);
		list.prices.put("minecraft:red_tulip", 2);
		list.prices.put("minecraft:orange_tulip", 2);
		list.prices.put("minecraft:white_tulip", 2);
		list.prices.put("minecraft:pink_tulip", 2);
		list.prices.put("minecraft:oxeye_daisy", 2);
		list.prices.put("minecraft:cornflower", 2);
		list.prices.put("minecraft:lily_of_the_valley", 3);
		list.prices.put("minecraft:torchflower", 5);  // Rare from archaeology
		list.prices.put("minecraft:pitcher_plant", 5);  // Rare from archaeology
		
		// === FLOWERS - TALL (3 silver) ===
		list.prices.put("minecraft:sunflower", 3);
		list.prices.put("minecraft:lilac", 3);
		list.prices.put("minecraft:rose_bush", 3);
		list.prices.put("minecraft:peony", 3);
		
		// === FLOWERS - RARE (5-10 silver) ===
		list.prices.put("minecraft:wither_rose", 10);  // Kill mob with wither
		list.prices.put("minecraft:spore_blossom", 8);  // Lush caves only
		list.prices.put("minecraft:pink_petals", 4);  // Cherry groves
		
		// === GRASS & FERNS (1 silver) ===
		list.prices.put("minecraft:short_grass", 1);
		list.prices.put("minecraft:tall_grass", 1);
		list.prices.put("minecraft:fern", 1);
		list.prices.put("minecraft:large_fern", 1);
		list.prices.put("minecraft:dead_bush", 1);
		
		// === UNDERWATER PLANTS (3-5 silver) ===
		list.prices.put("minecraft:seagrass", 2);
		list.prices.put("minecraft:tall_seagrass", 2);
		list.prices.put("minecraft:kelp", 3);
		list.prices.put("minecraft:sea_pickle", 8);  // Warm ocean
		
		// === BAMBOO & SUGAR CANE (2 silver) ===
		list.prices.put("minecraft:bamboo", 2);
		list.prices.put("minecraft:sugar_cane", 2);
		
		// === CACTUS (2 silver = desert) ===
		list.prices.put("minecraft:cactus", 2);
		
		// === MUSHROOMS (3 silver = dark areas) ===
		list.prices.put("minecraft:brown_mushroom", 3);
		list.prices.put("minecraft:red_mushroom", 3);
		list.prices.put("minecraft:brown_mushroom_block", 4);
		list.prices.put("minecraft:red_mushroom_block", 4);
		list.prices.put("minecraft:mushroom_stem", 4);
		
		// === NETHER FUNGI (5 silver = Nether) ===
		list.prices.put("minecraft:crimson_fungus", 5);
		list.prices.put("minecraft:warped_fungus", 5);
		list.prices.put("minecraft:crimson_roots", 4);
		list.prices.put("minecraft:warped_roots", 4);
		list.prices.put("minecraft:nether_sprouts", 4);
		list.prices.put("minecraft:weeping_vines", 5);
		list.prices.put("minecraft:twisting_vines", 5);
		
		// === NETHER WART & BLOCKS (6 silver) ===
		list.prices.put("minecraft:nether_wart", 6);
		list.prices.put("minecraft:nether_wart_block", 8);
		list.prices.put("minecraft:warped_wart_block", 8);
		list.prices.put("minecraft:shroomlight", 10);
		
		// === CORAL - LIVE (10 silver = warm ocean + water breathing) ===
		String[] coralTypes = {"tube", "brain", "bubble", "fire", "horn"};
		for (String coral : coralTypes) {
			list.prices.put("minecraft:" + coral + "_coral", 10);
			list.prices.put("minecraft:" + coral + "_coral_fan", 10);
			list.prices.put("minecraft:" + coral + "_coral_block", 12);
			list.prices.put("minecraft:dead_" + coral + "_coral", 8);
			list.prices.put("minecraft:dead_" + coral + "_coral_fan", 8);
			list.prices.put("minecraft:dead_" + coral + "_coral_block", 10);
		}
		
		// === SPONGE (15 silver = ocean monument) ===
		list.prices.put("minecraft:sponge", 15);
		list.prices.put("minecraft:wet_sponge", 15);
		
		// === MELON & PUMPKIN (3 silver = jungle/plains) ===
		list.prices.put("minecraft:melon", 3);
		list.prices.put("minecraft:pumpkin", 3);
		list.prices.put("minecraft:carved_pumpkin", 4);
		list.prices.put("minecraft:jack_o_lantern", 5);
		
		// === HAY & DRIED KELP (3 silver = farm) ===
		list.prices.put("minecraft:hay_block", 3);
		list.prices.put("minecraft:dried_kelp_block", 4);
		
		// === MOSS (4 silver = lush caves) ===
		list.prices.put("minecraft:moss_block", 5);
		list.prices.put("minecraft:moss_carpet", 2);
		list.prices.put("minecraft:hanging_roots", 3);
		
		// === VINES (2 silver = jungle/swamp) ===
		list.prices.put("minecraft:vine", 2);
		list.prices.put("minecraft:glow_lichen", 4);
		list.prices.put("minecraft:sculk", 8);  // Deep dark
		list.prices.put("minecraft:sculk_vein", 6);
		list.prices.put("minecraft:sculk_catalyst", 20);  // Rare
		list.prices.put("minecraft:sculk_shrieker", 20);  // Dangerous
		list.prices.put("minecraft:sculk_sensor", 15);
		
		// === DRIPLEAF (5 silver = lush caves) ===
		list.prices.put("minecraft:small_dripleaf", 4);
		list.prices.put("minecraft:big_dripleaf", 5);
		
		// === TURTLE EGG (10 silver = beach + silk touch) ===
		list.prices.put("minecraft:turtle_egg", 10);
		
		// === FROGLIGHT (12 silver = swamp + frog) ===
		list.prices.put("minecraft:ochre_froglight", 12);
		list.prices.put("minecraft:verdant_froglight", 12);
		list.prices.put("minecraft:pearlescent_froglight", 12);
		
		// === CHORUS (8 silver = End) ===
		list.prices.put("minecraft:chorus_plant", 8);
		list.prices.put("minecraft:chorus_flower", 10);
		
		return list;
	}
	
	public static PriceList createFunctionalBlocksDefaults() {
		PriceList list = new PriceList();
		list.category = "functional_blocks";
		
		// ===== FUNCTIONAL BLOCKS PRICING =====
		// Formula: Base_Rate × Material_Tier_Multiplier × Utility_Multiplier
		// Base_Rate = 5 Silver Coin (simplest crafted block)
		// Material: x1 wood/stone, x2 iron, x4 diamond/obsidian, x6 emerald/rare
		// Utility: x1 decor, x2 workstation basic, x3 workstation advanced
		
		// === LIGHTING - Tier 1 (3-15 silver = crafting simple) ===
		list.prices.put("minecraft:torch", 1);
		list.prices.put("minecraft:soul_torch", 2);
		list.prices.put("minecraft:lantern", 5);
		list.prices.put("minecraft:soul_lantern", 6);
		list.prices.put("minecraft:sea_lantern", 15);  // From ocean monument
		list.prices.put("minecraft:glowstone", 10);  // From Nether
		list.prices.put("minecraft:shroomlight", 10);  // From Nether fungi
		list.prices.put("minecraft:end_rod", 8);  // From End
		list.prices.put("minecraft:redstone_lamp", 12);  // Redstone + glowstone
		
		// Froglights (from natural_blocks but also functional)
		list.prices.put("minecraft:ochre_froglight", 12);
		list.prices.put("minecraft:verdant_froglight", 12);
		list.prices.put("minecraft:pearlescent_froglight", 12);
		
		// === WORKSTATIONS - Basic (20-40 silver = wood/stone materials) ===
		list.prices.put("minecraft:crafting_table", 5);
		list.prices.put("minecraft:furnace", 20);
		list.prices.put("minecraft:smoker", 25);
		list.prices.put("minecraft:blast_furnace", 30);
		list.prices.put("minecraft:loom", 20);
		list.prices.put("minecraft:cartography_table", 20);
		list.prices.put("minecraft:fletching_table", 20);
		list.prices.put("minecraft:smithing_table", 25);
		list.prices.put("minecraft:grindstone", 25);
		list.prices.put("minecraft:stonecutter", 20);
		list.prices.put("minecraft:composter", 15);
		list.prices.put("minecraft:barrel", 15);
		list.prices.put("minecraft:campfire", 12);
		list.prices.put("minecraft:soul_campfire", 15);
		
		// === WORKSTATIONS - Advanced (50-100 silver = iron/obsidian/diamond) ===
		list.prices.put("minecraft:anvil", 80);  // 31 iron ingots
		list.prices.put("minecraft:chipped_anvil", 70);
		list.prices.put("minecraft:damaged_anvil", 60);
		list.prices.put("minecraft:enchanting_table", 100);  // Diamond + obsidian
		list.prices.put("minecraft:brewing_stand", 60);  // Blaze rod
		list.prices.put("minecraft:beacon", 200);  // Nether star (boss drop)
		list.prices.put("minecraft:conduit", 150);  // 8 nautilus shells
		
		// === STORAGE - Basic (15-30 silver) ===
		list.prices.put("minecraft:chest", 20);
		list.prices.put("minecraft:trapped_chest", 25);
		list.prices.put("minecraft:ender_chest", 80);  // 8 obsidian + eye of ender
		
		// Shulker boxes (all colors) - 80 silver (already in colored_blocks, repeat for completeness)
		String[] colors = {"white", "orange", "magenta", "light_blue", "yellow", "lime", 
		                   "pink", "gray", "light_gray", "cyan", "purple", "blue", 
		                   "brown", "green", "red", "black"};
		for (String color : colors) {
			list.prices.put("minecraft:" + color + "_shulker_box", 80);
		}
		list.prices.put("minecraft:shulker_box", 80);  // Undyed
		
		// === DOORS - by material (3-10 silver) ===
		String[] woodTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"};
		for (String wood : woodTypes) {
			int price = (wood.equals("crimson") || wood.equals("warped")) ? 10 : 3;
			list.prices.put("minecraft:" + wood + "_door", price);
			list.prices.put("minecraft:" + wood + "_trapdoor", price);
		}
		list.prices.put("minecraft:iron_door", 25);
		list.prices.put("minecraft:iron_trapdoor", 25);
		
		// Copper doors/trapdoors (1.21 feature, may not exist in 1.20.1 - skip for now)
		
		// === BEDS (all colors) - 5 silver (wood + wool) ===
		for (String color : colors) {
			list.prices.put("minecraft:" + color + "_bed", 8);
		}
		
		// === SIGNS & HANGING SIGNS - 3-5 silver ===
		for (String wood : woodTypes) {
			int price = (wood.equals("crimson") || wood.equals("warped")) ? 8 : 3;
			list.prices.put("minecraft:" + wood + "_sign", price);
			list.prices.put("minecraft:" + wood + "_hanging_sign", price + 1);
		}
		
		// === BANNERS (all colors) - 5 silver ===
		for (String color : colors) {
			list.prices.put("minecraft:" + color + "_banner", 8);
		}
		
		// === DECORATIVE BLOCKS - 5-15 silver ===
		list.prices.put("minecraft:bookshelf", 10);
		list.prices.put("minecraft:chiseled_bookshelf", 12);
		list.prices.put("minecraft:lectern", 15);
		list.prices.put("minecraft:painting", 5);
		list.prices.put("minecraft:item_frame", 5);
		list.prices.put("minecraft:glow_item_frame", 8);
		list.prices.put("minecraft:armor_stand", 15);
		list.prices.put("minecraft:flower_pot", 3);
		list.prices.put("minecraft:decorated_pot", 8);
		
		// === CANDLES (all colors) - 3 silver ===
		list.prices.put("minecraft:candle", 3);
		for (String color : colors) {
			list.prices.put("minecraft:" + color + "_candle", 3);
		}
		
		// === CHAINS (iron) - 8 silver ===
		list.prices.put("minecraft:chain", 8);
		
		// === GLASS & TINTED GLASS - 2-10 silver ===
		list.prices.put("minecraft:glass", 2);
		list.prices.put("minecraft:glass_pane", 1);
		list.prices.put("minecraft:tinted_glass", 10);  // Amethyst shard
		
		// Stained glass (already in colored_blocks, skip)
		
		// === BELLS & MISC - 10-20 silver ===
		list.prices.put("minecraft:bell", 20);  // Villager trades or raid
		list.prices.put("minecraft:lodestone", 40);  // 8 chiseled stone bricks + netherite
		list.prices.put("minecraft:respawn_anchor", 50);  // 6 crying obsidian + 3 glowstone
		
		// === MOB HEADS - 30-60 silver (rare drops) ===
		list.prices.put("minecraft:skeleton_skull", 40);
		list.prices.put("minecraft:wither_skeleton_skull", 60);  // Rare Nether mob
		list.prices.put("minecraft:zombie_head", 40);
		list.prices.put("minecraft:creeper_head", 50);
		list.prices.put("minecraft:piglin_head", 50);
		// Player Head - EXCLUDED (creative-only, requires commands)
		// Dragon Head - EXCLUDED (unique, 1 per End ship)
		
		// === POTS & PLANTS - 3-8 silver ===
		list.prices.put("minecraft:flower_pot", 3);
		list.prices.put("minecraft:decorated_pot", 8);
		
		// === RAILS (not in Redstone tab, might be here) - 5-12 silver ===
		// Actually these belong to Redstone Blocks tab, skip
		
		// === INFESTED BLOCKS - EXCLUDED (bất hợp pháp) ===
		// minecraft:infested_stone
		// minecraft:infested_cobblestone
		// minecraft:infested_stone_bricks
		// minecraft:infested_mossy_stone_bricks
		// minecraft:infested_cracked_stone_bricks
		// minecraft:infested_chiseled_stone_bricks
		// minecraft:infested_deepslate
		
		// === END PORTAL FRAME - EXCLUDED (bất hợp pháp, creative-only) ===
		// === DRAGON EGG - EXCLUDED (unique item, 1 per world) ===
		// === BEDROCK - EXCLUDED (admin block) ===
		// === SPAWNER - EXCLUDED (creative-only) ===
		// === COMMAND BLOCK - EXCLUDED (admin) ===
		// === BARRIER - EXCLUDED (admin) ===
		// === STRUCTURE BLOCK/VOID - EXCLUDED (admin) ===
		// === JIGSAW - EXCLUDED (admin) ===
		
		return list;
	}
	
	public static PriceList createRedstoneBlocksDefaults() {
		PriceList list = new PriceList();
		list.category = "redstone_blocks";
		
		// ===== REDSTONE BLOCKS PRICING =====
		// Formula: Base_Rate × Material_Tier_Multiplier × Mechanism_Complexity_Multiplier
		// Base_Rate = 5 Silver Coin
		// Material: x1 basic (redstone/iron), x2 rare ingredients (slime/quartz), x3 very rare
		// Complexity: x1 simple (dust/torch), x2 logic (repeater/comparator), x3 mechanical (piston/hopper)
		
		// === REDSTONE BASICS - Tier 1 (5-10 silver = simple input/output) ===
		list.prices.put("minecraft:redstone_dust", 1);  // Per unit (sold in stacks)
		list.prices.put("minecraft:redstone_torch", 5);
		list.prices.put("minecraft:redstone_block", 25);  // 9 redstone dust
		list.prices.put("minecraft:redstone_lamp", 12);  // Glowstone + 4 redstone
		
		// === INPUT DEVICES - Tier 1 (5-8 silver = buttons/levers/plates) ===
		list.prices.put("minecraft:lever", 5);
		
		// Buttons (all wood types + stone + polished blackstone)
		String[] woodTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"};
		for (String wood : woodTypes) {
			int price = (wood.equals("crimson") || wood.equals("warped")) ? 8 : 5;
			list.prices.put("minecraft:" + wood + "_button", price);
		}
		list.prices.put("minecraft:stone_button", 5);
		list.prices.put("minecraft:polished_blackstone_button", 8);
		
		// Pressure plates (all wood types + stone variants + weighted)
		for (String wood : woodTypes) {
			int price = (wood.equals("crimson") || wood.equals("warped")) ? 8 : 5;
			list.prices.put("minecraft:" + wood + "_pressure_plate", price);
		}
		list.prices.put("minecraft:stone_pressure_plate", 5);
		list.prices.put("minecraft:polished_blackstone_pressure_plate", 8);
		list.prices.put("minecraft:light_weighted_pressure_plate", 20);  // Gold
		list.prices.put("minecraft:heavy_weighted_pressure_plate", 20);  // Iron
		
		// === SENSORS - Tier 2 (15-30 silver = detection mechanisms) ===
		list.prices.put("minecraft:daylight_detector", 15);
		list.prices.put("minecraft:tripwire_hook", 12);
		list.prices.put("minecraft:trapped_chest", 25);
		list.prices.put("minecraft:observer", 30);  // Cobblestone + redstone + quartz
		list.prices.put("minecraft:target", 20);  // Hay bale + redstone
		list.prices.put("minecraft:sculk_sensor", 40);  // Deep dark, rare
		list.prices.put("minecraft:calibrated_sculk_sensor", 50);  // 1.20 feature
		
		// === LOGIC COMPONENTS - Tier 2 (15-20 silver = repeater/comparator) ===
		list.prices.put("minecraft:repeater", 15);  // 3 stone + 2 redstone torch + 1 redstone
		list.prices.put("minecraft:comparator", 20);  // 3 redstone torch + 3 stone + 1 nether quartz
		
		// === MECHANICAL DEVICES - Tier 3 (25-45 silver = movement/items) ===
		list.prices.put("minecraft:piston", 25);  // Wood + cobblestone + iron + redstone
		list.prices.put("minecraft:sticky_piston", 35);  // Piston + slime ball
		list.prices.put("minecraft:dispenser", 30);  // Cobblestone + bow + redstone
		list.prices.put("minecraft:dropper", 30);  // Cobblestone + redstone
		list.prices.put("minecraft:hopper", 45);  // 5 iron ingots + chest
		
		// === RAILS - Tier 1-2 (3-12 silver = transportation) ===
		list.prices.put("minecraft:rail", 3);  // 6 iron + 1 stick
		list.prices.put("minecraft:powered_rail", 12);  // 6 gold + 1 stick + 1 redstone
		list.prices.put("minecraft:detector_rail", 10);  // 6 iron + 1 stone plate + 1 redstone
		list.prices.put("minecraft:activator_rail", 10);  // 6 iron + 2 sticks + 1 redstone torch
		
		// === SPECIAL BLOCKS - Tier 2-3 (15-30 silver) ===
		list.prices.put("minecraft:note_block", 15);  // 8 planks + 1 redstone
		list.prices.put("minecraft:jukebox", 20);  // 8 planks + 1 diamond
		list.prices.put("minecraft:lectern", 15);  // Bookshelf + slab (also in functional)
		
		// === TNT - Tier 2 (20 silver = explosive) ===
		list.prices.put("minecraft:tnt", 20);  // 5 gunpowder + 4 sand
		
		// === DOORS/TRAPDOORS (iron only - redstone compatible) ===
		list.prices.put("minecraft:iron_door", 25);  // 6 iron ingots
		list.prices.put("minecraft:iron_trapdoor", 25);  // 4 iron ingots
		
		// === COPPER REDSTONE (1.21 features - may not exist in 1.20.1) ===
		// Copper Bulb, Copper Door, Copper Trapdoor - skip for 1.20.1
		
		// === MISC REDSTONE ===
		list.prices.put("minecraft:lightning_rod", 15);  // 3 copper ingots
		
		return list;
	}
	
	public static PriceList createToolsUtilitiesDefaults() {
		PriceList list = new PriceList();
		list.category = "tools_utilities";
		
		// ===== TOOLS & UTILITIES PRICING =====
		// Formula: Base_Rate × Material_Tier_Multiplier
		// Base_Rate = 4 Silver Coin (wooden tool)
		// Material: x1 wood, x1.5 stone, x3 iron, x4 gold, x8 diamond (GOLD COIN), x15 netherite (GOLD COIN)
		
		// NOTE: Diamond+ items use CURRENCY_TYPE field to indicate Gold Coin
		// For now, we store price in Silver Coin equivalent, conversion handled in shop UI
		
		// === PICKAXES ===
		list.prices.put("minecraft:wooden_pickaxe", 4);
		list.prices.put("minecraft:stone_pickaxe", 6);
		list.prices.put("minecraft:iron_pickaxe", 12);
		list.prices.put("minecraft:golden_pickaxe", 16);
		list.prices.put("minecraft:diamond_pickaxe", 500);  // 5 Gold Coin (100:1 rate)
		list.prices.put("minecraft:netherite_pickaxe", 1000);  // 10 Gold Coin
		
		// === AXES ===
		list.prices.put("minecraft:wooden_axe", 4);
		list.prices.put("minecraft:stone_axe", 6);
		list.prices.put("minecraft:iron_axe", 12);
		list.prices.put("minecraft:golden_axe", 16);
		list.prices.put("minecraft:diamond_axe", 500);  // 5 Gold Coin
		list.prices.put("minecraft:netherite_axe", 1000);  // 10 Gold Coin
		
		// === SHOVELS ===
		list.prices.put("minecraft:wooden_shovel", 4);
		list.prices.put("minecraft:stone_shovel", 6);
		list.prices.put("minecraft:iron_shovel", 12);
		list.prices.put("minecraft:golden_shovel", 16);
		list.prices.put("minecraft:diamond_shovel", 500);  // 5 Gold Coin
		list.prices.put("minecraft:netherite_shovel", 1000);  // 10 Gold Coin
		
		// === HOES ===
		list.prices.put("minecraft:wooden_hoe", 4);
		list.prices.put("minecraft:stone_hoe", 6);
		list.prices.put("minecraft:iron_hoe", 12);
		list.prices.put("minecraft:golden_hoe", 16);
		list.prices.put("minecraft:diamond_hoe", 500);  // 5 Gold Coin
		list.prices.put("minecraft:netherite_hoe", 1000);  // 10 Gold Coin
		
		// === SWORDS (also in Combat, but include here for Tools tab) ===
		list.prices.put("minecraft:wooden_sword", 4);
		list.prices.put("minecraft:stone_sword", 6);
		list.prices.put("minecraft:iron_sword", 12);
		list.prices.put("minecraft:golden_sword", 16);
		list.prices.put("minecraft:diamond_sword", 500);  // 5 Gold Coin
		list.prices.put("minecraft:netherite_sword", 1000);  // 10 Gold Coin
		
		// === ARMOR - HELMETS ===
		list.prices.put("minecraft:leather_helmet", 10);
		list.prices.put("minecraft:chainmail_helmet", 15);  // Rare (not craftable)
		list.prices.put("minecraft:iron_helmet", 20);
		list.prices.put("minecraft:golden_helmet", 25);
		list.prices.put("minecraft:diamond_helmet", 700);  // 7 Gold Coin
		list.prices.put("minecraft:netherite_helmet", 1400);  // 14 Gold Coin
		list.prices.put("minecraft:turtle_helmet", 30);  // Scute from turtles
		
		// === ARMOR - CHESTPLATES ===
		list.prices.put("minecraft:leather_chestplate", 16);
		list.prices.put("minecraft:chainmail_chestplate", 24);
		list.prices.put("minecraft:iron_chestplate", 32);
		list.prices.put("minecraft:golden_chestplate", 40);
		list.prices.put("minecraft:diamond_chestplate", 1100);  // 11 Gold Coin
		list.prices.put("minecraft:netherite_chestplate", 2200);  // 22 Gold Coin
		
		// === ARMOR - LEGGINGS ===
		list.prices.put("minecraft:leather_leggings", 14);
		list.prices.put("minecraft:chainmail_leggings", 21);
		list.prices.put("minecraft:iron_leggings", 28);
		list.prices.put("minecraft:golden_leggings", 35);
		list.prices.put("minecraft:diamond_leggings", 1000);  // 10 Gold Coin
		list.prices.put("minecraft:netherite_leggings", 2000);  // 20 Gold Coin
		
		// === ARMOR - BOOTS ===
		list.prices.put("minecraft:leather_boots", 8);
		list.prices.put("minecraft:chainmail_boots", 12);
		list.prices.put("minecraft:iron_boots", 16);
		list.prices.put("minecraft:golden_boots", 20);
		list.prices.put("minecraft:diamond_boots", 600);  // 6 Gold Coin
		list.prices.put("minecraft:netherite_boots", 1200);  // 12 Gold Coin
		
		// === SHIELDS ===
		list.prices.put("minecraft:shield", 30);  // Wood + iron
		
		// === BOWS & CROSSBOWS ===
		list.prices.put("minecraft:bow", 15);
		list.prices.put("minecraft:crossbow", 25);
		list.prices.put("minecraft:arrow", 1);  // Per arrow
		list.prices.put("minecraft:spectral_arrow", 5);  // Glowstone dust
		list.prices.put("minecraft:tipped_arrow", 3);  // With potion
		
		// === UTILITY ITEMS ===
		list.prices.put("minecraft:bucket", 12);  // 3 iron
		list.prices.put("minecraft:water_bucket", 15);
		list.prices.put("minecraft:lava_bucket", 20);
		list.prices.put("minecraft:powder_snow_bucket", 18);
		list.prices.put("minecraft:milk_bucket", 15);
		list.prices.put("minecraft:cod_bucket", 18);
		list.prices.put("minecraft:salmon_bucket", 18);
		list.prices.put("minecraft:pufferfish_bucket", 20);
		list.prices.put("minecraft:tropical_fish_bucket", 20);
		list.prices.put("minecraft:axolotl_bucket", 25);
		list.prices.put("minecraft:tadpole_bucket", 18);
		
		list.prices.put("minecraft:compass", 15);  // 4 iron + 1 redstone
		list.prices.put("minecraft:clock", 15);  // 4 gold + 1 redstone
		list.prices.put("minecraft:spyglass", 40);  // 2 copper + 1 amethyst shard
		list.prices.put("minecraft:recovery_compass", 80);  // 8 echo shards (Deep Dark, rare)
		
		list.prices.put("minecraft:fishing_rod", 8);
		list.prices.put("minecraft:carrot_on_a_stick", 12);
		list.prices.put("minecraft:warped_fungus_on_a_stick", 15);
		
		list.prices.put("minecraft:flint_and_steel", 10);
		list.prices.put("minecraft:fire_charge", 8);
		
		list.prices.put("minecraft:shears", 10);  // 2 iron
		list.prices.put("minecraft:brush", 12);  // Archaeology tool
		
		list.prices.put("minecraft:lead", 15);  // String + slime
		list.prices.put("minecraft:name_tag", 25);  // Rare (not craftable, dungeon/fishing)
		list.prices.put("minecraft:saddle", 30);  // Rare (not craftable)
		
		// === HORSE ARMOR ===
		list.prices.put("minecraft:leather_horse_armor", 25);
		list.prices.put("minecraft:iron_horse_armor", 40);  // Silver Coin
		list.prices.put("minecraft:golden_horse_armor", 60);  // Silver Coin
		list.prices.put("minecraft:diamond_horse_armor", 100);  // 1 Gold Coin (phế, ít ai dùng)
		
		// === ENDGAME ITEMS ===
		list.prices.put("minecraft:elytra", 5500);  // 55 Gold Coin
		list.prices.put("minecraft:totem_of_undying", 200);  // 2 Gold Coin (dễ farm)
		
		// === TRIDENTS ===
		list.prices.put("minecraft:trident", 150);  // 1.5 Gold Coin (dễ kiếm)
		
		// === MUSIC DISCS (collectibles) - 30-50 Silver Coin ===
		// NOTE: These are vanilla discs, NOT related to custom Music Player feature
		list.prices.put("minecraft:music_disc_13", 30);
		list.prices.put("minecraft:music_disc_cat", 30);
		list.prices.put("minecraft:music_disc_blocks", 35);
		list.prices.put("minecraft:music_disc_chirp", 35);
		list.prices.put("minecraft:music_disc_far", 35);
		list.prices.put("minecraft:music_disc_mall", 35);
		list.prices.put("minecraft:music_disc_mellohi", 35);
		list.prices.put("minecraft:music_disc_stal", 35);
		list.prices.put("minecraft:music_disc_strad", 35);
		list.prices.put("minecraft:music_disc_ward", 35);
		list.prices.put("minecraft:music_disc_11", 40);
		list.prices.put("minecraft:music_disc_wait", 35);
		list.prices.put("minecraft:music_disc_otherside", 50);  // Rare (1.18+)
		list.prices.put("minecraft:music_disc_5", 50);  // Deep dark
		list.prices.put("minecraft:music_disc_pigstep", 45);  // Bastion
		list.prices.put("minecraft:music_disc_relic", 50);  // 1.20+
		list.prices.put("minecraft:music_disc_creator", 50);  // 1.21+
		list.prices.put("minecraft:music_disc_creator_music_box", 50);  // 1.21+
		list.prices.put("minecraft:music_disc_precipice", 50);  // 1.21+
		
		// === BOATS & MINECARTS ===
		String[] boatTypes = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo"};
		for (String boat : boatTypes) {
			list.prices.put("minecraft:" + boat + "_boat", 5);
			list.prices.put("minecraft:" + boat + "_chest_boat", 25);
		}
		
		list.prices.put("minecraft:minecart", 20);  // 5 iron
		list.prices.put("minecraft:chest_minecart", 40);
		list.prices.put("minecraft:furnace_minecart", 45);
		list.prices.put("minecraft:hopper_minecart", 65);
		list.prices.put("minecraft:tnt_minecart", 40);
		
		// === GOAT HORN (1.19+ collectible) ===
		list.prices.put("minecraft:goat_horn", 20);  // Various types from goats
		
		return list;
	}
	
	public static PriceList createFoodDrinksDefaults() {
		PriceList list = new PriceList();
		list.category = "food_drinks";
		
		// ===== FOOD & DRINKS PRICING =====
		// Logic: Giá dựa trên độ khó farm + hunger/saturation restore
		// Low tier (1-3s): Easy crops (wheat, carrot, potato)
		// Mid tier (5-10s): Cooked meat, crafted food
		// High tier (15-30s): Golden apples, suspicious stew, complex recipes
		
		// === RAW CROPS (1-2s = farm rất dễ) ===
		list.prices.put("minecraft:wheat", 1);
		list.prices.put("minecraft:carrot", 1);
		list.prices.put("minecraft:potato", 1);
		list.prices.put("minecraft:beetroot", 1);
		list.prices.put("minecraft:sweet_berries", 2);
		list.prices.put("minecraft:glow_berries", 3);
		list.prices.put("minecraft:melon_slice", 1);
		list.prices.put("minecraft:apple", 2);
		list.prices.put("minecraft:chorus_fruit", 5);  // End only
		
		// === COOKED CROPS (2-3s = cần smelt) ===
		list.prices.put("minecraft:baked_potato", 2);
		list.prices.put("minecraft:bread", 3);  // 3 wheat
		
		// === RAW MEAT (3-5s = cần giết mob) ===
		list.prices.put("minecraft:beef", 3);
		list.prices.put("minecraft:porkchop", 3);
		list.prices.put("minecraft:chicken", 3);
		list.prices.put("minecraft:mutton", 3);
		list.prices.put("minecraft:rabbit", 3);
		list.prices.put("minecraft:cod", 3);
		list.prices.put("minecraft:salmon", 3);
		list.prices.put("minecraft:tropical_fish", 4);
		list.prices.put("minecraft:pufferfish", 5);  // Độc
		
		// === COOKED MEAT (5-8s = raw + smelt) ===
		list.prices.put("minecraft:cooked_beef", 6);
		list.prices.put("minecraft:cooked_porkchop", 6);
		list.prices.put("minecraft:cooked_chicken", 6);
		list.prices.put("minecraft:cooked_mutton", 6);
		list.prices.put("minecraft:cooked_rabbit", 6);
		list.prices.put("minecraft:cooked_cod", 6);
		list.prices.put("minecraft:cooked_salmon", 6);
		
		// === CRAFTED FOOD (5-10s = cần recipe) ===
		list.prices.put("minecraft:cookie", 3);  // 2 wheat + cocoa
		list.prices.put("minecraft:pumpkin_pie", 8);  // Pumpkin + egg + sugar
		list.prices.put("minecraft:cake", 12);  // Milk + sugar + egg + wheat
		
		// === STEWS & SOUPS (8-12s = bowl + ingredients) ===
		list.prices.put("minecraft:mushroom_stew", 8);
		list.prices.put("minecraft:rabbit_stew", 10);
		list.prices.put("minecraft:beetroot_soup", 8);
		list.prices.put("minecraft:suspicious_stew", 15);  // Có effects
		
		// === GOLDEN FOOD (20-100s = rare) ===
		list.prices.put("minecraft:golden_apple", 500);  // 5 Gold Coin (8 gold ingots)
		list.prices.put("minecraft:enchanted_golden_apple", 5000);  // 50 Gold Coin (ultra rare, not craftable)
		list.prices.put("minecraft:golden_carrot", 50);  // 8 gold nuggets (best food for saturation)
		
		// === MISC FOOD ===
		list.prices.put("minecraft:spider_eye", 5);
		list.prices.put("minecraft:poisonous_potato", 2);
		list.prices.put("minecraft:rotten_flesh", 2);
		
		// === DRINKS - POTIONS (10-50s tùy effect) ===
		list.prices.put("minecraft:potion", 10);  // Water bottle (base)
		list.prices.put("minecraft:milk_bucket", 15);  // Cure effects
		
		// Awkward Potion (base for brewing)
		list.prices.put("minecraft:awkward_potion", 20);
		
		// Common potions (Healing, Speed, Strength, etc.)
		// Note: Minecraft có hàng trăm loại potions với NBT data khác nhau
		// Để đơn giản, tôi chỉ list giá cơ bản, actual shop sẽ cần handle NBT
		// Hoặc có thể không bán potions (player tự brew)
		
		// === HONEY ===
		list.prices.put("minecraft:honey_bottle", 10);
		list.prices.put("minecraft:honeycomb", 8);
		
		return list;
	}
	
	public static PriceList createIngredientsDefaults() {
		PriceList list = new PriceList();
		list.category = "ingredients";
		
		// ===== INGREDIENTS PRICING =====
		// NOTE: Bán nguyên liệu có thể làm game quá dễ (pay-to-win)
		// Nên đặt giá CAO để balance
		
		// === COMMON MINERALS (5-20s) ===
		list.prices.put("minecraft:coal", 5);
		list.prices.put("minecraft:charcoal", 3);
		list.prices.put("minecraft:raw_iron", 15);
		list.prices.put("minecraft:iron_ingot", 20);
		list.prices.put("minecraft:iron_nugget", 2);
		list.prices.put("minecraft:raw_copper", 10);
		list.prices.put("minecraft:copper_ingot", 12);
		list.prices.put("minecraft:raw_gold", 30);
		list.prices.put("minecraft:gold_ingot", 40);
		list.prices.put("minecraft:gold_nugget", 4);
		
		// === RARE GEMS (50-200s) ===
		list.prices.put("minecraft:lapis_lazuli", 20);
		list.prices.put("minecraft:redstone", 15);
		list.prices.put("minecraft:diamond", 500);  // 5 Gold Coin
		list.prices.put("minecraft:emerald", 400);  // 4 Gold Coin
		list.prices.put("minecraft:netherite_scrap", 1000);  // 10 Gold Coin
		list.prices.put("minecraft:netherite_ingot", 2000);  // 20 Gold Coin
		
		// === AMETHYST (15-30s) ===
		list.prices.put("minecraft:amethyst_shard", 25);
		
		// === QUARTZ (10-15s) ===
		list.prices.put("minecraft:quartz", 12);
		
		// === PRISMARINE (15-25s) ===
		list.prices.put("minecraft:prismarine_shard", 18);
		list.prices.put("minecraft:prismarine_crystals", 20);
		
		// === ORGANIC MATERIALS (3-15s) ===
		list.prices.put("minecraft:string", 3);
		list.prices.put("minecraft:leather", 8);
		list.prices.put("minecraft:rabbit_hide", 5);
		list.prices.put("minecraft:feather", 3);
		list.prices.put("minecraft:bone", 5);
		list.prices.put("minecraft:bone_meal", 2);
		list.prices.put("minecraft:stick", 1);
		list.prices.put("minecraft:paper", 3);
		list.prices.put("minecraft:book", 10);
		list.prices.put("minecraft:ink_sac", 5);
		list.prices.put("minecraft:glow_ink_sac", 8);
		
		// === SLIME & MOBILITY (10-20s) ===
		list.prices.put("minecraft:slime_ball", 15);  // Hard to farm
		list.prices.put("minecraft:phantom_membrane", 20);  // Rare drop
		
		// === NETHER MATERIALS (15-50s) ===
		list.prices.put("minecraft:blaze_rod", 30);
		list.prices.put("minecraft:blaze_powder", 15);
		list.prices.put("minecraft:magma_cream", 20);
		list.prices.put("minecraft:ghast_tear", 40);
		list.prices.put("minecraft:nether_star", 5000);  // 50 Gold Coin (boss drop)
		
		// === ENDER MATERIALS (20-80s) ===
		list.prices.put("minecraft:ender_pearl", 25);
		list.prices.put("minecraft:ender_eye", 50);  // Pearl + blaze powder
		list.prices.put("minecraft:shulker_shell", 80);  // Very rare
		list.prices.put("minecraft:dragon_breath", 100);  // Unique boss
		
		// === GUNPOWDER & EXPLOSIVES (8-15s) ===
		list.prices.put("minecraft:gunpowder", 10);
		list.prices.put("minecraft:fire_charge", 8);
		
		// === BREWING INGREDIENTS (10-30s) ===
		list.prices.put("minecraft:glowstone_dust", 8);
		list.prices.put("minecraft:redstone_dust", 5);  // Duplicate but for clarity
		list.prices.put("minecraft:sugar", 2);
		list.prices.put("minecraft:spider_eye", 5);
		list.prices.put("minecraft:fermented_spider_eye", 12);
		list.prices.put("minecraft:glistering_melon_slice", 40);  // 8 gold nuggets
		list.prices.put("minecraft:golden_carrot", 50);  // Duplicate from food
		list.prices.put("minecraft:rabbit_foot", 15);
		list.prices.put("minecraft:turtle_scute", 30);  // Rare from baby turtle
		list.prices.put("minecraft:nautilus_shell", 50);  // Rare ocean drop
		list.prices.put("minecraft:heart_of_the_sea", 100);  // Buried treasure
		
		// === ECHO SHARDS (Deep Dark - 100s) ===
		list.prices.put("minecraft:echo_shard", 100);  // Very rare
		
		// === POTTERY SHERDS (Archaeology - 30-50s each) ===
		// NOTE: Có rất nhiều loại sherds, tôi list một số common
		String[] sherds = {
			"angler", "archer", "arms_up", "blade", "brewer", "burn", "danger",
			"explorer", "friend", "heart", "heartbreak", "howl", "miner", "mourner",
			"plenty", "prize", "sheaf", "shelter", "skull", "snort"
		};
		for (String sherd : sherds) {
			list.prices.put("minecraft:" + sherd + "_pottery_sherd", 40);
		}
		
		// === ARMOR TRIMS (Smithing templates - 50-150s) ===
		// NOTE: Cũng có nhiều loại templates, list một số
		String[] trims = {
			"coast", "dune", "eye", "host", "raiser", "rib", "sentry", "shaper",
			"silence", "snout", "spire", "tide", "vex", "ward", "wayfinder", "wild"
		};
		for (String trim : trims) {
			list.prices.put("minecraft:" + trim + "_armor_trim_smithing_template", 80);
		}
		
		// Netherite Upgrade Template (rarest)
		list.prices.put("minecraft:netherite_upgrade_smithing_template", 150);
		
		// === MISC ===
		list.prices.put("minecraft:flint", 5);
		list.prices.put("minecraft:clay_ball", 2);
		list.prices.put("minecraft:brick", 3);
		list.prices.put("minecraft:nether_brick", 8);
		list.prices.put("minecraft:wheat_seeds", 1);
		list.prices.put("minecraft:beetroot_seeds", 1);
		list.prices.put("minecraft:melon_seeds", 3);
		list.prices.put("minecraft:pumpkin_seeds", 3);
		list.prices.put("minecraft:torchflower_seeds", 10);
		list.prices.put("minecraft:pitcher_pod", 10);
		
		// === ENCHANTED BOOKS - TOÀN BỘ (All enchantments with all levels) ===
		// ENCHANTED BOOKS
		// Format: "enchanted_book:enchantment_name:level" 
		// ShopManager will parse this and create proper enchanted books with stored_enchantments component
		
		// WEAPON ENCHANTMENTS
		list.prices.put("enchanted_book:sharpness:1", 60);
		list.prices.put("enchanted_book:sharpness:2", 70);
		list.prices.put("enchanted_book:sharpness:3", 80);
		list.prices.put("enchanted_book:sharpness:4", 90);
		list.prices.put("enchanted_book:sharpness:5", 100);
		
		list.prices.put("enchanted_book:smite:1", 60);
		list.prices.put("enchanted_book:smite:2", 70);
		list.prices.put("enchanted_book:smite:3", 80);
		list.prices.put("enchanted_book:smite:4", 90);
		list.prices.put("enchanted_book:smite:5", 100);
		
		list.prices.put("enchanted_book:bane_of_arthropods:1", 48);
		list.prices.put("enchanted_book:bane_of_arthropods:2", 56);
		list.prices.put("enchanted_book:bane_of_arthropods:3", 64);
		list.prices.put("enchanted_book:bane_of_arthropods:4", 72);
		list.prices.put("enchanted_book:bane_of_arthropods:5", 80);
		
		list.prices.put("enchanted_book:knockback:1", 60);
		list.prices.put("enchanted_book:knockback:2", 70);
		
		list.prices.put("enchanted_book:fire_aspect:1", 85);
		list.prices.put("enchanted_book:fire_aspect:2", 100);
		
		list.prices.put("enchanted_book:looting:1", 100);
		list.prices.put("enchanted_book:looting:2", 120);
		list.prices.put("enchanted_book:looting:3", 140);
		
		list.prices.put("enchanted_book:sweeping_edge:1", 75);
		list.prices.put("enchanted_book:sweeping_edge:2", 90);
		list.prices.put("enchanted_book:sweeping_edge:3", 105);
		
		// TOOL ENCHANTMENTS
		list.prices.put("enchanted_book:efficiency:1", 60);
		list.prices.put("enchanted_book:efficiency:2", 70);
		list.prices.put("enchanted_book:efficiency:3", 80);
		list.prices.put("enchanted_book:efficiency:4", 90);
		list.prices.put("enchanted_book:efficiency:5", 100);
		
		list.prices.put("enchanted_book:fortune:1", 100);
		list.prices.put("enchanted_book:fortune:2", 120);
		list.prices.put("enchanted_book:fortune:3", 140);
		
		list.prices.put("enchanted_book:silk_touch:1", 150);
		
		// UNIVERSAL
		list.prices.put("enchanted_book:unbreaking:1", 85);
		list.prices.put("enchanted_book:unbreaking:2", 100);
		list.prices.put("enchanted_book:unbreaking:3", 115);
		
		list.prices.put("enchanted_book:mending:1", 500);  // 5 Gold - Most valuable
		
		// ARMOR ENCHANTMENTS
		list.prices.put("enchanted_book:protection:1", 60);
		list.prices.put("enchanted_book:protection:2", 70);
		list.prices.put("enchanted_book:protection:3", 80);
		list.prices.put("enchanted_book:protection:4", 90);
		
		list.prices.put("enchanted_book:fire_protection:1", 60);
		list.prices.put("enchanted_book:fire_protection:2", 70);
		list.prices.put("enchanted_book:fire_protection:3", 80);
		list.prices.put("enchanted_book:fire_protection:4", 90);
		
		list.prices.put("enchanted_book:blast_protection:1", 60);
		list.prices.put("enchanted_book:blast_protection:2", 70);
		list.prices.put("enchanted_book:blast_protection:3", 80);
		list.prices.put("enchanted_book:blast_protection:4", 90);
		
		list.prices.put("enchanted_book:projectile_protection:1", 60);
		list.prices.put("enchanted_book:projectile_protection:2", 70);
		list.prices.put("enchanted_book:projectile_protection:3", 80);
		list.prices.put("enchanted_book:projectile_protection:4", 90);
		
		list.prices.put("enchanted_book:feather_falling:1", 72);
		list.prices.put("enchanted_book:feather_falling:2", 84);
		list.prices.put("enchanted_book:feather_falling:3", 96);
		list.prices.put("enchanted_book:feather_falling:4", 108);
		
		list.prices.put("enchanted_book:thorns:1", 100);
		list.prices.put("enchanted_book:thorns:2", 120);
		list.prices.put("enchanted_book:thorns:3", 140);
		
		list.prices.put("enchanted_book:respiration:1", 75);
		list.prices.put("enchanted_book:respiration:2", 90);
		list.prices.put("enchanted_book:respiration:3", 105);
		
		list.prices.put("enchanted_book:aqua_affinity:1", 80);
		
		list.prices.put("enchanted_book:depth_strider:1", 90);
		list.prices.put("enchanted_book:depth_strider:2", 110);
		list.prices.put("enchanted_book:depth_strider:3", 130);
		
		list.prices.put("enchanted_book:frost_walker:1", 120);
		list.prices.put("enchanted_book:frost_walker:2", 140);
		
		list.prices.put("enchanted_book:soul_speed:1", 175);
		list.prices.put("enchanted_book:soul_speed:2", 200);
		list.prices.put("enchanted_book:soul_speed:3", 225);
		
		list.prices.put("enchanted_book:swift_sneak:1", 175);
		list.prices.put("enchanted_book:swift_sneak:2", 200);
		list.prices.put("enchanted_book:swift_sneak:3", 225);
		
		// BOW ENCHANTMENTS
		list.prices.put("enchanted_book:power:1", 60);
		list.prices.put("enchanted_book:power:2", 70);
		list.prices.put("enchanted_book:power:3", 80);
		list.prices.put("enchanted_book:power:4", 90);
		list.prices.put("enchanted_book:power:5", 100);
		
		list.prices.put("enchanted_book:punch:1", 60);
		list.prices.put("enchanted_book:punch:2", 70);
		
		list.prices.put("enchanted_book:flame:1", 70);
		list.prices.put("enchanted_book:infinity:1", 120);
		
		// CROSSBOW ENCHANTMENTS
		list.prices.put("enchanted_book:quick_charge:1", 75);
		list.prices.put("enchanted_book:quick_charge:2", 90);
		list.prices.put("enchanted_book:quick_charge:3", 105);
		
		list.prices.put("enchanted_book:multishot:1", 100);
		
		list.prices.put("enchanted_book:piercing:1", 60);
		list.prices.put("enchanted_book:piercing:2", 70);
		list.prices.put("enchanted_book:piercing:3", 80);
		list.prices.put("enchanted_book:piercing:4", 90);
		
		// TRIDENT ENCHANTMENTS
		list.prices.put("enchanted_book:loyalty:1", 90);
		list.prices.put("enchanted_book:loyalty:2", 110);
		list.prices.put("enchanted_book:loyalty:3", 130);
		
		list.prices.put("enchanted_book:impaling:1", 60);
		list.prices.put("enchanted_book:impaling:2", 70);
		list.prices.put("enchanted_book:impaling:3", 80);
		list.prices.put("enchanted_book:impaling:4", 90);
		list.prices.put("enchanted_book:impaling:5", 100);
		
		list.prices.put("enchanted_book:riptide:1", 100);
		list.prices.put("enchanted_book:riptide:2", 120);
		list.prices.put("enchanted_book:riptide:3", 140);
		
		list.prices.put("enchanted_book:channeling:1", 120);
		
		
		// FISHING ROD ENCHANTMENTS
		list.prices.put("enchanted_book:luck_of_the_sea:1", 75);
		list.prices.put("enchanted_book:luck_of_the_sea:2", 90);
		list.prices.put("enchanted_book:luck_of_the_sea:3", 105);
		
		list.prices.put("enchanted_book:lure:1", 75);
		list.prices.put("enchanted_book:lure:2", 90);
		list.prices.put("enchanted_book:lure:3", 105);
		
		// CURSES
		list.prices.put("enchanted_book:vanishing_curse:1", 10);
		list.prices.put("enchanted_book:binding_curse:1", 10);
		
		// === BOOKS (base items) ===
		list.prices.put("minecraft:book", 10);
		list.prices.put("minecraft:writable_book", 15);
		list.prices.put("minecraft:written_book", 20);
		
		return list;
	}
	
	// Getters
	public String getVersion() { return version; }
	public String getCategory() { return category; }
	public Map<String, Integer> getPrices() { return prices; }
	
	public Integer getPrice(String itemId) {
		return prices.get(itemId);
	}
	
	// Setters
	public void setVersion(String version) { this.version = version; }
	public void setCategory(String category) { this.category = category; }
	public void setPrices(Map<String, Integer> prices) { this.prices = prices; }
}
