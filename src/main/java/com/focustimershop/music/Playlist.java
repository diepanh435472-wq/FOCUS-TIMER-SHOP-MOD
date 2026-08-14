package com.focustimershop.music;

import java.util.ArrayList;
import java.util.List;

/**
 * Playlist containing music tracks
 * Serialized to/from JSON
 */
public class Playlist {
	private String version = "1.0.0";
	private List<MusicTrack> tracks = new ArrayList<>();
	private int currentTrackIndex = 0;
	private boolean shuffle = false;
	private boolean repeat = false;  // false = no repeat, true = repeat all
	
	public Playlist() {
	}
	
	// Getters & Setters
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
	
	public List<MusicTrack> getTracks() { return tracks; }
	public void setTracks(List<MusicTrack> tracks) { this.tracks = tracks; }
	
	public int getCurrentTrackIndex() { return currentTrackIndex; }
	public void setCurrentTrackIndex(int currentTrackIndex) { 
		if (currentTrackIndex >= 0 && currentTrackIndex < tracks.size()) {
			this.currentTrackIndex = currentTrackIndex; 
		}
	}
	
	public boolean isShuffle() { return shuffle; }
	public void setShuffle(boolean shuffle) { this.shuffle = shuffle; }
	
	public boolean isRepeat() { return repeat; }
	public void setRepeat(boolean repeat) { this.repeat = repeat; }
	
	// Utility methods
	public MusicTrack getCurrentTrack() {
		if (tracks.isEmpty() || currentTrackIndex < 0 || currentTrackIndex >= tracks.size()) {
			return null;
		}
		return tracks.get(currentTrackIndex);
	}
	
	public void addTrack(MusicTrack track) {
		tracks.add(track);
	}
	
	public void removeTrack(int index) {
		if (index >= 0 && index < tracks.size()) {
			tracks.remove(index);
			// Adjust current index if needed
			if (currentTrackIndex >= tracks.size()) {
				currentTrackIndex = Math.max(0, tracks.size() - 1);
			}
		}
	}
	
	public void nextTrack() {
		if (tracks.isEmpty()) return;
		
		if (shuffle) {
			// Random next (excluding current)
			int next = (int)(Math.random() * tracks.size());
			currentTrackIndex = next;
		} else {
			currentTrackIndex++;
			if (currentTrackIndex >= tracks.size()) {
				if (repeat) {
					currentTrackIndex = 0;  // Loop back
				} else {
					currentTrackIndex = tracks.size() - 1;  // Stay at end
				}
			}
		}
	}
	
	public void previousTrack() {
		if (tracks.isEmpty()) return;
		
		currentTrackIndex--;
		if (currentTrackIndex < 0) {
			if (repeat) {
				currentTrackIndex = tracks.size() - 1;  // Loop to end
			} else {
				currentTrackIndex = 0;  // Stay at start
			}
		}
	}
	
	public boolean isEmpty() {
		return tracks.isEmpty();
	}
	
	public int size() {
		return tracks.size();
	}
}
