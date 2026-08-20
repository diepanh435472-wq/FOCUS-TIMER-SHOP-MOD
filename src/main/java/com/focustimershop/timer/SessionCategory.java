package com.focustimershop.timer;

/**
 * v1.0.7-beta Timer UI Overhaul - Session categories
 * Replaces the old 3-type system with 4 categories
 */
public enum SessionCategory {
	TAP_TRUNG,      // Focus (earns rewards)
	NGHI_NGAN,      // Short Break (earns rewards)
	NGHI_DAI,       // Long Break (earns rewards)
	TAP_LUYEN;      // Exercise (earns rewards)
	
	public String getDisplayName() {
		switch (this) {
			case TAP_TRUNG: return "Tập Trung";
			case NGHI_NGAN: return "Nghỉ Ngắn";
			case NGHI_DAI: return "Nghỉ Dài";
			case TAP_LUYEN: return "Tập Luyện";
			default: return name();
		}
	}
	
	public String getDisplayNameShort() {
		switch (this) {
			case TAP_TRUNG: return "Focus";
			case NGHI_NGAN: return "Short Break";
			case NGHI_DAI: return "Long Break";
			case TAP_LUYEN: return "Exercise";
			default: return name();
		}
	}
}
