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
