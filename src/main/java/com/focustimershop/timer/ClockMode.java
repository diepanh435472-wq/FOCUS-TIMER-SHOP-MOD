package com.focustimershop.timer;

/**
 * v1.0.7-beta Timer UI Overhaul - Clock modes
 * Available in all session categories
 */
public enum ClockMode {
	COUNTDOWN,  // Đếm ngược - counts down from preset time
	STOPWATCH;  // Bấm giờ - counts up from zero
	
	public String getDisplayName() {
		switch (this) {
			case COUNTDOWN: return "Đếm Ngược";
			case STOPWATCH: return "Bấm Giờ";
			default: return name();
		}
	}
	
	public String getDisplayNameShort() {
		switch (this) {
			case COUNTDOWN: return "Countdown";
			case STOPWATCH: return "Stopwatch";
			default: return name();
		}
	}
}
