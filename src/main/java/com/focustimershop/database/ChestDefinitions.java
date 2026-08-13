package com.focustimershop.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lucky chest definitions with loot tables and probabilities
 * Updated for v1.0.3 with openOne/openTenPlusOne structure
 */
public class ChestDefinitions {
	
	private String version = "1.0.3";
	private List<ChestDef> chests = new ArrayList<>();
	
	/**
	 * Payment option for chest opening
	 */
	public static class PaymentOptionData {
		public int silver;
		public int gold;
		
		public PaymentOptionData() {}
		
		public PaymentOptionData(int silver, int gold) {
			this.silver = silver;
			this.gold = gold;
		}
		
		public static PaymentOptionData silver(int amount) {
			return new PaymentOptionData(amount, 0);
		}
		
		public static PaymentOptionData gold(int amount) {
			return new PaymentOptionData(0, amount);
		}
		
		public static PaymentOptionData mixed(int gold, int silver) {
			return new PaymentOptionData(silver, gold);
		}
	}
	
	/**
	 * Opening package (x1 or x10+1)
	 */
	public static class OpenPackageData {
		public List<PaymentOptionData> paymentOptions = new ArrayList<>();
	}
	
	/**
	 * Chest definition
	 */
	public static class ChestDef {
		public String name;
		public String displayName;
		
		// New structure for v1.0.3
		public OpenPackageData openOne;
		public OpenPackageData openTenPlusOne;
		
		// Legacy fields (deprecated but kept for backward compatibility)
		@Deprecated
		public int cost;
		@Deprecated
		public String currencyType;  // "SILVER" or "GOLD"
		
		public Map<String, Double> rarityProbabilities;  // rarity -> percentage
	}
	
	public static ChestDefinitions createDefault() {
		ChestDefinitions defs = new ChestDefinitions();
		
		// Wooden Chest - Gỗ
		ChestDef wooden = new ChestDef();
		wooden.name = "WOODEN";
		wooden.displayName = "Wooden Chest";
		wooden.openOne = new OpenPackageData();
		wooden.openOne.paymentOptions.add(PaymentOptionData.silver(40));
		wooden.openTenPlusOne = new OpenPackageData();
		wooden.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(400));
		wooden.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(4));
		wooden.rarityProbabilities = new HashMap<>();
		wooden.rarityProbabilities.put("COMMON", 80.0);
		wooden.rarityProbabilities.put("UNCOMMON", 18.0);
		wooden.rarityProbabilities.put("RARE", 2.0);
		wooden.rarityProbabilities.put("EPIC", 0.0);
		wooden.rarityProbabilities.put("LEGENDARY", 0.0);
		defs.chests.add(wooden);
		
		// Stone Chest - Đá
		ChestDef stone = new ChestDef();
		stone.name = "STONE";
		stone.displayName = "Stone Chest";
		stone.openOne = new OpenPackageData();
		stone.openOne.paymentOptions.add(PaymentOptionData.silver(60));
		stone.openTenPlusOne = new OpenPackageData();
		stone.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(600));
		stone.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(6));
		stone.rarityProbabilities = new HashMap<>();
		stone.rarityProbabilities.put("COMMON", 70.0);
		stone.rarityProbabilities.put("UNCOMMON", 25.0);
		stone.rarityProbabilities.put("RARE", 5.0);
		stone.rarityProbabilities.put("EPIC", 0.0);
		stone.rarityProbabilities.put("LEGENDARY", 0.0);
		defs.chests.add(stone);
		
		// Coal Chest - Than
		ChestDef coal = new ChestDef();
		coal.name = "COAL";
		coal.displayName = "Coal Chest";
		coal.openOne = new OpenPackageData();
		coal.openOne.paymentOptions.add(PaymentOptionData.silver(80));
		coal.openTenPlusOne = new OpenPackageData();
		coal.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(800));
		coal.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(8));
		coal.rarityProbabilities = new HashMap<>();
		coal.rarityProbabilities.put("COMMON", 60.0);
		coal.rarityProbabilities.put("UNCOMMON", 30.0);
		coal.rarityProbabilities.put("RARE", 9.0);
		coal.rarityProbabilities.put("EPIC", 1.0);
		coal.rarityProbabilities.put("LEGENDARY", 0.0);
		defs.chests.add(coal);
		
		// Copper Chest - Đồng
		ChestDef copper = new ChestDef();
		copper.name = "COPPER";
		copper.displayName = "Copper Chest";
		copper.openOne = new OpenPackageData();
		copper.openOne.paymentOptions.add(PaymentOptionData.silver(110));
		copper.openOne.paymentOptions.add(PaymentOptionData.mixed(1, 10));
		copper.openTenPlusOne = new OpenPackageData();
		copper.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(1100));
		copper.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(11));
		copper.rarityProbabilities = new HashMap<>();
		copper.rarityProbabilities.put("COMMON", 50.0);
		copper.rarityProbabilities.put("UNCOMMON", 35.0);
		copper.rarityProbabilities.put("RARE", 12.0);
		copper.rarityProbabilities.put("EPIC", 3.0);
		copper.rarityProbabilities.put("LEGENDARY", 0.0);
		defs.chests.add(copper);
		
		// Iron Chest - Sắt
		ChestDef iron = new ChestDef();
		iron.name = "IRON";
		iron.displayName = "Iron Chest";
		iron.openOne = new OpenPackageData();
		iron.openOne.paymentOptions.add(PaymentOptionData.silver(150));
		iron.openOne.paymentOptions.add(PaymentOptionData.mixed(1, 50));
		iron.openTenPlusOne = new OpenPackageData();
		iron.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(1500));
		iron.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(15));
		iron.rarityProbabilities = new HashMap<>();
		iron.rarityProbabilities.put("COMMON", 40.0);
		iron.rarityProbabilities.put("UNCOMMON", 35.0);
		iron.rarityProbabilities.put("RARE", 18.0);
		iron.rarityProbabilities.put("EPIC", 7.0);
		iron.rarityProbabilities.put("LEGENDARY", 0.0);
		defs.chests.add(iron);
		
		// Gold Chest - Vàng
		ChestDef gold = new ChestDef();
		gold.name = "GOLD";
		gold.displayName = "Gold Chest";
		gold.openOne = new OpenPackageData();
		gold.openOne.paymentOptions.add(PaymentOptionData.silver(300));
		gold.openOne.paymentOptions.add(PaymentOptionData.gold(3));
		gold.openTenPlusOne = new OpenPackageData();
		gold.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(3000));
		gold.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(30));
		gold.rarityProbabilities = new HashMap<>();
		gold.rarityProbabilities.put("COMMON", 30.0);
		gold.rarityProbabilities.put("UNCOMMON", 35.0);
		gold.rarityProbabilities.put("RARE", 22.0);
		gold.rarityProbabilities.put("EPIC", 12.0);
		gold.rarityProbabilities.put("LEGENDARY", 1.0);
		defs.chests.add(gold);
		
		// Lapis Chest
		ChestDef lapis = new ChestDef();
		lapis.name = "LAPIS";
		lapis.displayName = "Lapis Chest";
		lapis.openOne = new OpenPackageData();
		lapis.openOne.paymentOptions.add(PaymentOptionData.silver(400));
		lapis.openOne.paymentOptions.add(PaymentOptionData.gold(4));
		lapis.openTenPlusOne = new OpenPackageData();
		lapis.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(4000));
		lapis.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(40));
		lapis.rarityProbabilities = new HashMap<>();
		lapis.rarityProbabilities.put("COMMON", 25.0);
		lapis.rarityProbabilities.put("UNCOMMON", 30.0);
		lapis.rarityProbabilities.put("RARE", 28.0);
		lapis.rarityProbabilities.put("EPIC", 15.0);
		lapis.rarityProbabilities.put("LEGENDARY", 2.0);
		defs.chests.add(lapis);
		
		// Diamond Chest - Kim cương
		ChestDef diamond = new ChestDef();
		diamond.name = "DIAMOND";
		diamond.displayName = "Diamond Chest";
		diamond.openOne = new OpenPackageData();
		diamond.openOne.paymentOptions.add(PaymentOptionData.silver(800));
		diamond.openOne.paymentOptions.add(PaymentOptionData.gold(8));
		diamond.openTenPlusOne = new OpenPackageData();
		diamond.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(8000));
		diamond.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(80));
		diamond.rarityProbabilities = new HashMap<>();
		diamond.rarityProbabilities.put("COMMON", 20.0);
		diamond.rarityProbabilities.put("UNCOMMON", 25.0);
		diamond.rarityProbabilities.put("RARE", 30.0);
		diamond.rarityProbabilities.put("EPIC", 20.0);
		diamond.rarityProbabilities.put("LEGENDARY", 5.0);
		defs.chests.add(diamond);
		
		// Quartz Chest
		ChestDef quartz = new ChestDef();
		quartz.name = "QUARTZ";
		quartz.displayName = "Quartz Chest";
		quartz.openOne = new OpenPackageData();
		quartz.openOne.paymentOptions.add(PaymentOptionData.silver(1000));
		quartz.openOne.paymentOptions.add(PaymentOptionData.gold(10));
		quartz.openTenPlusOne = new OpenPackageData();
		quartz.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(10000));
		quartz.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(100));
		quartz.rarityProbabilities = new HashMap<>();
		quartz.rarityProbabilities.put("COMMON", 15.0);
		quartz.rarityProbabilities.put("UNCOMMON", 23.0);
		quartz.rarityProbabilities.put("RARE", 30.0);
		quartz.rarityProbabilities.put("EPIC", 25.0);
		quartz.rarityProbabilities.put("LEGENDARY", 7.0);
		defs.chests.add(quartz);
		
		// Netherite Chest
		ChestDef netherite = new ChestDef();
		netherite.name = "NETHERITE";
		netherite.displayName = "Netherite Chest";
		netherite.openOne = new OpenPackageData();
		netherite.openOne.paymentOptions.add(PaymentOptionData.silver(1500));
		netherite.openOne.paymentOptions.add(PaymentOptionData.gold(15));
		netherite.openTenPlusOne = new OpenPackageData();
		netherite.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(15000));
		netherite.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(150));
		netherite.rarityProbabilities = new HashMap<>();
		netherite.rarityProbabilities.put("COMMON", 12.0);
		netherite.rarityProbabilities.put("UNCOMMON", 20.0);
		netherite.rarityProbabilities.put("RARE", 30.0);
		netherite.rarityProbabilities.put("EPIC", 28.0);
		netherite.rarityProbabilities.put("LEGENDARY", 10.0);
		defs.chests.add(netherite);
		
		// Obsidian Chest
		ChestDef obsidian = new ChestDef();
		obsidian.name = "OBSIDIAN";
		obsidian.displayName = "Obsidian Chest";
		obsidian.openOne = new OpenPackageData();
		obsidian.openOne.paymentOptions.add(PaymentOptionData.silver(2000));
		obsidian.openOne.paymentOptions.add(PaymentOptionData.gold(20));
		obsidian.openTenPlusOne = new OpenPackageData();
		obsidian.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(20000));
		obsidian.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(200));
		obsidian.rarityProbabilities = new HashMap<>();
		obsidian.rarityProbabilities.put("COMMON", 10.0);
		obsidian.rarityProbabilities.put("UNCOMMON", 18.0);
		obsidian.rarityProbabilities.put("RARE", 28.0);
		obsidian.rarityProbabilities.put("EPIC", 30.0);
		obsidian.rarityProbabilities.put("LEGENDARY", 14.0);
		defs.chests.add(obsidian);
		
		// Bedrock Chest
		ChestDef bedrock = new ChestDef();
		bedrock.name = "BEDROCK";
		bedrock.displayName = "Bedrock Chest";
		bedrock.openOne = new OpenPackageData();
		bedrock.openOne.paymentOptions.add(PaymentOptionData.silver(3000));
		bedrock.openOne.paymentOptions.add(PaymentOptionData.gold(30));
		bedrock.openTenPlusOne = new OpenPackageData();
		bedrock.openTenPlusOne.paymentOptions.add(PaymentOptionData.silver(30000));
		bedrock.openTenPlusOne.paymentOptions.add(PaymentOptionData.gold(300));
		bedrock.rarityProbabilities = new HashMap<>();
		bedrock.rarityProbabilities.put("COMMON", 10.0);
		bedrock.rarityProbabilities.put("UNCOMMON", 25.0);
		bedrock.rarityProbabilities.put("RARE", 30.0);
		bedrock.rarityProbabilities.put("EPIC", 25.0);
		bedrock.rarityProbabilities.put("LEGENDARY", 10.0);
		defs.chests.add(bedrock);
		
		return defs;
	}
	
	// Getters
	public String getVersion() { return version; }
	public List<ChestDef> getChests() { return chests; }
	
	public ChestDef getChest(String name) {
		for (ChestDef chest : chests) {
			if (chest.name.equals(name)) {
				return chest;
			}
		}
		return null;
	}
	
	// Setters
	public void setVersion(String version) { this.version = version; }
	public void setChests(List<ChestDef> chests) { this.chests = chests; }
}
