package com.focustimershop.profile;

import java.util.Arrays;
import java.util.List;

/**
 * Configurable streak milestones (v1.0.6 Phase 2)
 * Shows next milestone target on Profile screen
 */
public class StreakMilestones {
	
	// Configurable milestone list (days)
	private static final List<Integer> MILESTONES = Arrays.asList(
		3, 7, 14, 30, 60, 100, 200, 365
	);
	
	/**
	 * Get next milestone after current streak
	 * @param currentStreak Current streak in days
	 * @return Next milestone day count, or -1 if no more milestones
	 */
	public static int getNextMilestone(int currentStreak) {
		for (int milestone : MILESTONES) {
			if (currentStreak < milestone) {
				return milestone;
			}
		}
		return -1; // No more milestones (streak >= 365)
	}
	
	/**
	 * Get all milestones (for admin/debug)
	 */
	public static List<Integer> getAllMilestones() {
		return MILESTONES;
	}
	
	/**
	 * Format milestone text for display
	 */
	public static String formatMilestone(int milestone) {
		if (milestone < 0) {
			return "Đã đạt tất cả mốc!";
		}
		return "Mốc tiếp theo: " + milestone + " ngày";
	}
}
