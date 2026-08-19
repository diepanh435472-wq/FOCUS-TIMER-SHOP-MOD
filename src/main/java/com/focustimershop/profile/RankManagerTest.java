package com.focustimershop.profile;

/**
 * Test cases for RankManager - especially multi-rank jump scenarios
 * v1.0.6 Phase 0.1 bug fix verification
 */
public class RankManagerTest {
	
	public static void runAllTests() {
		System.out.println("========================================");
		System.out.println("RANK MANAGER TEST SUITE (v1.0.6 Phase 0.1)");
		System.out.println("========================================\n");
		
		testSingleRankProgress();
		testMultiRankJump();
		testExactBoundaries();
		testMaxRank();
		testRealWorldBugCase();
		
		System.out.println("\n========================================");
		System.out.println("ALL TESTS COMPLETED");
		System.out.println("========================================");
	}
	
	/**
	 * Test 1: Normal single-rank progression
	 */
	private static void testSingleRankProgress() {
		System.out.println("TEST 1: Single Rank Progress");
		System.out.println("-----------------------------");
		
		// Player has 20 XP - should be Chưa Hạng II (15-30 range)
		RankTier rank = RankManager.resolveRank(20);
		
		System.out.println("Total XP: 20");
		System.out.println("Expected: Chưa Hạng II");
		System.out.println("Actual: " + rank.getDisplayName());
		System.out.println("XP Into Level: " + rank.getXpIntoLevel() + " / " + rank.getXpNeededForLevel());
		
		// xpIntoLevel should be 20 - 15 = 5 (entered at 15 cumulative)
		// xpNeededForLevel should be 15 (requirement for level II)
		double percent = (rank.getXpIntoLevel() * 100.0) / rank.getXpNeededForLevel();
		System.out.println("Percentage: " + String.format("%.1f%%", percent));
		
		assert rank.getTier().equals("Chưa Hạng") : "Wrong tier";
		assert rank.getLevel() == 2 : "Wrong level";
		assert rank.getXpIntoLevel() == 5 : "Wrong xpIntoLevel";
		assert rank.getXpNeededForLevel() == 15 : "Wrong xpNeededForLevel";
		assert percent <= 100.0 : "FAIL: Percentage > 100%!";
		
		System.out.println("✓ PASS\n");
	}
	
	/**
	 * Test 2: Multi-rank jump (the main bug scenario)
	 */
	private static void testMultiRankJump() {
		System.out.println("TEST 2: Multi-Rank Jump (Main Bug Scenario)");
		System.out.println("--------------------------------------------");
		
		// Player earns 150 XP in one session, jumping from Unranked I (0 XP) to beyond Bronze
		// 150 XP should land in Đồng III (cumulative 130-155)
		RankTier rank = RankManager.resolveRank(150);
		
		System.out.println("Total XP: 150 (jumped multiple ranks at once)");
		System.out.println("Expected: Đồng III or nearby");
		System.out.println("Actual: " + rank.getDisplayName());
		System.out.println("XP Into Level: " + rank.getXpIntoLevel() + " / " + rank.getXpNeededForLevel());
		
		double percent = (rank.getXpIntoLevel() * 100.0) / rank.getXpNeededForLevel();
		System.out.println("Percentage: " + String.format("%.1f%%", percent));
		
		assert rank.getTier().equals("Đồng") : "Wrong tier - should be Đồng";
		assert rank.getLevel() == 3 : "Wrong level - should be III";
		assert percent <= 100.0 : "FAIL: Percentage > 100%!";
		assert percent >= 0.0 : "FAIL: Percentage < 0%!";
		
		System.out.println("✓ PASS\n");
	}
	
	/**
	 * Test 3: Exact rank boundaries
	 */
	private static void testExactBoundaries() {
		System.out.println("TEST 3: Exact Rank Boundaries");
		System.out.println("------------------------------");
		
		// Test exactly at rank threshold - should be AT that rank, 100% progress
		// Đồng I starts at cumulative 85, ends at cumulative 105
		RankTier rankAt105 = RankManager.resolveRank(105);
		
		System.out.println("Total XP: 105 (exact boundary)");
		System.out.println("Rank: " + rankAt105.getDisplayName());
		System.out.println("XP Into Level: " + rankAt105.getXpIntoLevel() + " / " + rankAt105.getXpNeededForLevel());
		
		double percent = (rankAt105.getXpIntoLevel() * 100.0) / rankAt105.getXpNeededForLevel();
		System.out.println("Percentage: " + String.format("%.1f%%", percent));
		
		// At 105, should be exactly completing Đồng I (100% into it)
		assert rankAt105.getTier().equals("Đồng") : "Wrong tier";
		assert rankAt105.getLevel() == 1 : "Wrong level";
		assert percent == 100.0 : "Should be exactly 100% at boundary";
		
		System.out.println("✓ PASS\n");
	}
	
	/**
	 * Test 4: Max rank overflow
	 */
	private static void testMaxRank() {
		System.out.println("TEST 4: Max Rank (Legend III overflow)");
		System.out.println("---------------------------------------");
		
		// Legend III caps at cumulative 24010
		// Test with way more XP - should not crash or give weird %
		RankTier rank = RankManager.resolveRank(50000);
		
		System.out.println("Total XP: 50000 (way over max)");
		System.out.println("Rank: " + rank.getDisplayName());
		System.out.println("Is Max Rank: " + rank.isMaxRank());
		System.out.println("XP Into Level: " + rank.getXpIntoLevel() + " / " + rank.getXpNeededForLevel());
		
		assert rank.getTier().equals("Legend") : "Should be Legend";
		assert rank.getLevel() == 3 : "Should be level III";
		assert rank.isMaxRank() : "Should be marked as max rank";
		
		System.out.println("✓ PASS\n");
	}
	
	/**
	 * Test 5: The EXACT bug reported by user
	 */
	private static void testRealWorldBugCase() {
		System.out.println("TEST 5: Real World Bug Case (User Report)");
		System.out.println("------------------------------------------");
		
		// User reported: "Đồng II — 44 / 25 XP — 176%"
		// 44 total XP should NOT be Đồng II (that's 130 cumulative)
		// 44 XP should be Chưa Hạng III (30-45 range) or Chưa Hạng IV (45-65)
		RankTier rank = RankManager.resolveRank(44);
		
		System.out.println("Total XP: 44 (user's actual XP)");
		System.out.println("Old buggy display: Đồng II — 44 / 25 XP — 176%");
		System.out.println("----");
		System.out.println("NEW CORRECT:");
		System.out.println("Rank: " + rank.getDisplayName());
		System.out.println("XP Into Level: " + rank.getXpIntoLevel() + " / " + rank.getXpNeededForLevel());
		
		double percent = (rank.getXpIntoLevel() * 100.0) / rank.getXpNeededForLevel();
		System.out.println("Percentage: " + String.format("%.1f%%", percent));
		System.out.println("Next Rank: " + rank.getNextRankName() + " (need " + rank.getXpToNextRank() + " more XP)");
		
		// 44 XP = Chưa Hạng III (30-45 range)
		// xpIntoLevel = 44 - 30 = 14
		// xpNeededForLevel = 15 (requirement for level III)
		// percent = 14/15 = 93.3%
		
		assert rank.getTier().equals("Chưa Hạng") : "Should be Chưa Hạng, not Đồng!";
		assert rank.getLevel() == 3 : "Should be level III";
		assert rank.getXpIntoLevel() == 14 : "xpIntoLevel should be 14";
		assert rank.getXpNeededForLevel() == 15 : "xpNeededForLevel should be 15";
		assert percent <= 100.0 : "CRITICAL: Percentage still > 100%!";
		assert Math.abs(percent - 93.33) < 1.0 : "Percentage should be ~93%";
		
		System.out.println("✓ PASS - Bug FIXED!\n");
	}
}
