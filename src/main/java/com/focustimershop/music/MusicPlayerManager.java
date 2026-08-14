package com.focustimershop.music;

import com.focustimershop.FocusTimerShop;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Music Player Manager - Handles audio playback and playlist management
 * Client-side only
 */
public class MusicPlayerManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path MUSIC_DIR;
	private static Path PLAYLIST_FILE;
	
	private static Playlist playlist = new Playlist();
	private static Clip currentClip = null;
	private static boolean isPlaying = false;
	private static boolean isPaused = false;
	
	/**
	 * Initialize music player (create directories, load playlist)
	 */
	public static void initialize() {
		try {
			// Use Minecraft's game directory instead of hardcoded .minecraft
			Path gameDir = FabricLoader.getInstance().getGameDir();
			Path fctmsDir = gameDir.resolve("FCTMS");
			MUSIC_DIR = fctmsDir.resolve("music");
			PLAYLIST_FILE = fctmsDir.resolve("playlist.json");
			
			// Create music directory if not exists
			Files.createDirectories(MUSIC_DIR);
			FocusTimerShop.LOGGER.info("========================================");
			FocusTimerShop.LOGGER.info("Music Player Initialized!");
			FocusTimerShop.LOGGER.info("Game directory: {}", gameDir.toAbsolutePath());
			FocusTimerShop.LOGGER.info("Music directory: {}", MUSIC_DIR.toAbsolutePath());
			FocusTimerShop.LOGGER.info("========================================");
			
			// Load playlist from JSON
			if (Files.exists(PLAYLIST_FILE)) {
				try (FileReader reader = new FileReader(PLAYLIST_FILE.toFile())) {
					playlist = GSON.fromJson(reader, Playlist.class);
					if (playlist == null) {
						playlist = new Playlist();
					}
					FocusTimerShop.LOGGER.info("Loaded playlist with {} tracks", playlist.size());
				} catch (Exception e) {
					FocusTimerShop.LOGGER.error("Failed to load playlist", e);
					playlist = new Playlist();
				}
			} else {
				FocusTimerShop.LOGGER.info("No playlist found, creating new");
				playlist = new Playlist();
				savePlaylist();
			}
			
			// Auto-scan music directory for new files
			scanMusicDirectory();
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to initialize music player", e);
		}
	}
	
	/**
	 * Scan music directory and add new tracks not in playlist
	 */
	private static void scanMusicDirectory() {
		try {
			File musicDir = MUSIC_DIR.toFile();
			if (!musicDir.exists() || !musicDir.isDirectory()) {
				return;
			}
			
			File[] files = musicDir.listFiles((dir, name) -> {
				String lower = name.toLowerCase();
				return lower.endsWith(".ogg") || lower.endsWith(".wav");
			});
			
			if (files == null || files.length == 0) {
				FocusTimerShop.LOGGER.info("No music files found in directory");
				return;
			}
			
			// Check which files are already in playlist
			java.util.Set<String> existingFiles = new java.util.HashSet<>();
			for (MusicTrack track : playlist.getTracks()) {
				existingFiles.add(track.getFilename());
			}
			
			// Add new files
			int addedCount = 0;
			for (File file : files) {
				String filename = file.getName();
				if (!existingFiles.contains(filename)) {
					String title = filename.substring(0, filename.lastIndexOf("."));
					MusicTrack track = new MusicTrack(filename, title);
					
					// Try to get duration
					try {
						AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
						AudioFormat format = audioStream.getFormat();
						long frames = audioStream.getFrameLength();
						double durationInSeconds = frames / format.getFrameRate();
						track.setDurationSeconds((int) durationInSeconds);
						audioStream.close();
					} catch (Exception e) {
						FocusTimerShop.LOGGER.debug("Could not read duration for {}", filename);
					}
					
					playlist.addTrack(track);
					addedCount++;
				}
			}
			
			if (addedCount > 0) {
				FocusTimerShop.LOGGER.info("Auto-added {} new tracks from music directory", addedCount);
				savePlaylist();
			}
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Error scanning music directory", e);
		}
	}
	
	/**
	 * Rescan music directory (public method for manual refresh)
	 */
	public static void rescanMusicDirectory() {
		FocusTimerShop.LOGGER.info("Manually rescanning music directory...");
		scanMusicDirectory();
	}
	
	/**
	 * Save playlist to JSON
	 */
	public static void savePlaylist() {
		try {
			Files.createDirectories(PLAYLIST_FILE.getParent());
			try (FileWriter writer = new FileWriter(PLAYLIST_FILE.toFile())) {
				GSON.toJson(playlist, writer);
			}
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to save playlist", e);
		}
	}
	
	/**
	 * Add music file to playlist
	 */
	public static boolean addTrack(File file) {
		FocusTimerShop.LOGGER.info("Attempting to add track: {}", file.getAbsolutePath());
		
		if (!file.exists()) {
			FocusTimerShop.LOGGER.error("File does not exist: {}", file);
			return false;
		}
		
		if (!file.canRead()) {
			FocusTimerShop.LOGGER.error("Cannot read file: {}", file);
			return false;
		}
		
		String filename = file.getName();
		int dotIndex = filename.lastIndexOf(".");
		if (dotIndex == -1) {
			FocusTimerShop.LOGGER.error("File has no extension: {}", filename);
			return false;
		}
		
		String extension = filename.substring(dotIndex + 1).toLowerCase();
		
		// Check supported formats
		if (!extension.equals("wav") && !extension.equals("ogg")) {
			FocusTimerShop.LOGGER.error("Unsupported format: {} (file: {})", extension, filename);
			return false;
		}
		
		try {
			// Ensure music directory exists
			Files.createDirectories(MUSIC_DIR);
			FocusTimerShop.LOGGER.info("Music directory: {}", MUSIC_DIR.toAbsolutePath());
			
			// Copy file to music directory
			Path dest = MUSIC_DIR.resolve(filename);
			FocusTimerShop.LOGGER.info("Copying to: {}", dest.toAbsolutePath());
			
			Files.copy(file.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			FocusTimerShop.LOGGER.info("File copied successfully");
			
			// Create track metadata
			String title = filename.substring(0, dotIndex);
			MusicTrack track = new MusicTrack(filename, title);
			
			// Try to get duration (for WAV files)
			try {
				AudioInputStream audioStream = AudioSystem.getAudioInputStream(dest.toFile());
				AudioFormat format = audioStream.getFormat();
				long frames = audioStream.getFrameLength();
				double durationInSeconds = frames / format.getFrameRate();
				track.setDurationSeconds((int) durationInSeconds);
				audioStream.close();
				FocusTimerShop.LOGGER.info("Track duration: {} seconds", (int)durationInSeconds);
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Could not read duration for {}: {}", filename, e.getMessage());
			}
			
			playlist.addTrack(track);
			savePlaylist();
			
			FocusTimerShop.LOGGER.info("Successfully added track '{}' to playlist (total: {})", title, playlist.size());
			return true;
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to add track: {}", e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Remove track from playlist
	 */
	public static void removeTrack(int index) {
		if (index == playlist.getCurrentTrackIndex() && isPlaying) {
			stop();
		}
		playlist.removeTrack(index);
		savePlaylist();
	}
	
	/**
	 * Play current track
	 */
	public static void play() {
		if (isPaused && currentClip != null) {
			// Resume from pause
			currentClip.start();
			isPlaying = true;
			isPaused = false;
			return;
		}
		
		MusicTrack track = playlist.getCurrentTrack();
		if (track == null) {
			FocusTimerShop.LOGGER.warn("No track to play");
			return;
		}
		
		stop();  // Stop current track if any
		
		try {
			Path audioFile = MUSIC_DIR.resolve(track.getFilename());
			if (!Files.exists(audioFile)) {
				FocusTimerShop.LOGGER.error("Audio file not found: {}", audioFile);
				return;
			}
			
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile.toFile());
			currentClip = AudioSystem.getClip();
			currentClip.open(audioStream);
			
			// Add listener for track end
			currentClip.addLineListener(event -> {
				if (event.getType() == LineEvent.Type.STOP && !isPaused) {
					// Track ended, play next
					playlist.nextTrack();
					play();
				}
			});
			
			currentClip.start();
			isPlaying = true;
			isPaused = false;
			
			FocusTimerShop.LOGGER.info("Playing: {}", track.getTitle());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to play track", e);
		}
	}
	
	/**
	 * Pause playback
	 */
	public static void pause() {
		if (currentClip != null && isPlaying) {
			currentClip.stop();
			isPaused = true;
			isPlaying = false;
		}
	}
	
	/**
	 * Stop playback
	 */
	public static void stop() {
		if (currentClip != null) {
			currentClip.stop();
			currentClip.close();
			currentClip = null;
		}
		isPlaying = false;
		isPaused = false;
	}
	
	/**
	 * Skip to next track
	 */
	public static void next() {
		playlist.nextTrack();
		if (isPlaying || isPaused) {
			play();
		}
		savePlaylist();
	}
	
	/**
	 * Go to previous track
	 */
	public static void previous() {
		playlist.previousTrack();
		if (isPlaying || isPaused) {
			play();
		}
		savePlaylist();
	}
	
	/**
	 * Select and play specific track
	 */
	public static void playTrack(int index) {
		playlist.setCurrentTrackIndex(index);
		play();
		savePlaylist();
	}
	
	// Getters
	public static Playlist getPlaylist() { return playlist; }
	public static boolean isPlaying() { return isPlaying; }
	public static boolean isPaused() { return isPaused; }
	public static MusicTrack getCurrentTrack() { return playlist.getCurrentTrack(); }
	
	/**
	 * Get current playback position in seconds
	 */
	public static int getCurrentPosition() {
		if (currentClip == null) return 0;
		long microseconds = currentClip.getMicrosecondPosition();
		return (int)(microseconds / 1_000_000);
	}
	
	/**
	 * Cleanup on shutdown
	 */
	public static void shutdown() {
		stop();
		savePlaylist();
	}
	
	/**
	 * Get music directory path (for debug)
	 */
	public static String getMusicDirectory() {
		try {
			return MUSIC_DIR != null ? MUSIC_DIR.toAbsolutePath().toString() : "Not initialized";
		} catch (Exception e) {
			return MUSIC_DIR != null ? MUSIC_DIR.toString() : "Error";
		}
	}
	
	/**
	 * Open music directory in file manager
	 */
	public static void openMusicDirectory() {
		try {
			if (MUSIC_DIR == null) {
				FocusTimerShop.LOGGER.error("Music directory not initialized");
				return;
			}
			
			Files.createDirectories(MUSIC_DIR);
			File dir = MUSIC_DIR.toFile();
			
			// Try to open in system file manager
			if (java.awt.Desktop.isDesktopSupported()) {
				java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
				if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
					desktop.open(dir);
					FocusTimerShop.LOGGER.info("Opened music directory in file manager");
					return;
				}
			}
			
			// Fallback: try xdg-open on Linux
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("linux")) {
				Runtime.getRuntime().exec(new String[]{"xdg-open", dir.getAbsolutePath()});
				FocusTimerShop.LOGGER.info("Opened music directory with xdg-open");
			} else if (os.contains("mac")) {
				Runtime.getRuntime().exec(new String[]{"open", dir.getAbsolutePath()});
				FocusTimerShop.LOGGER.info("Opened music directory with open command");
			} else if (os.contains("win")) {
				Runtime.getRuntime().exec(new String[]{"explorer", dir.getAbsolutePath()});
				FocusTimerShop.LOGGER.info("Opened music directory with explorer");
			}
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to open music directory", e);
		}
	}
}
