package com.focustimershop.music;

/**
 * Represents a single music track in the playlist
 */
public class MusicTrack {
	private String filename;      // e.g. "song1.ogg"
	private String title;          // Display name (user can edit)
	private String artist;         // Optional
	private int durationSeconds;   // Track length
	
	public MusicTrack(String filename, String title) {
		this.filename = filename;
		this.title = title;
		this.artist = "Unknown";
		this.durationSeconds = 0;
	}
	
	// Getters & Setters
	public String getFilename() { return filename; }
	public void setFilename(String filename) { this.filename = filename; }
	
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	
	public String getArtist() { return artist; }
	public void setArtist(String artist) { this.artist = artist; }
	
	public int getDurationSeconds() { return durationSeconds; }
	public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
	
	/**
	 * Get formatted duration (MM:SS)
	 */
	public String getFormattedDuration() {
		if (durationSeconds == 0) return "--:--";
		int minutes = durationSeconds / 60;
		int seconds = durationSeconds % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}
}
