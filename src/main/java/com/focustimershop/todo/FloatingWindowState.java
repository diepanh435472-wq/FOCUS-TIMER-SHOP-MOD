package com.focustimershop.todo;

import com.google.gson.JsonObject;

/**
 * v1.0.7-beta - Floating Window State Model
 * Tracks position and state (normal/minimized/closed) of floating windows
 */
public class FloatingWindowState {
	public enum State {
		NORMAL,
		MINIMIZED,
		CLOSED
	}
	
	private final String windowId;
	private State state;
	private int posX;
	private int posY;
	
	public FloatingWindowState(String windowId, State state, int posX, int posY) {
		this.windowId = windowId;
		this.state = state;
		this.posX = posX;
		this.posY = posY;
	}
	
	public FloatingWindowState(String windowId) {
		this(windowId, State.NORMAL, 50, 50); // Default position
	}
	
	// Getters
	public String getWindowId() {
		return windowId;
	}
	
	public State getState() {
		return state;
	}
	
	public int getPosX() {
		return posX;
	}
	
	public int getPosY() {
		return posY;
	}
	
	// Setters
	public void setState(State state) {
		this.state = state;
	}
	
	public void setPosition(int x, int y) {
		this.posX = x;
		this.posY = y;
	}
	
	// Serialization
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("windowId", windowId);
		json.addProperty("state", state.name());
		json.addProperty("posX", posX);
		json.addProperty("posY", posY);
		return json;
	}
	
	public static FloatingWindowState fromJson(JsonObject json) {
		String windowId = json.get("windowId").getAsString();
		State state = State.valueOf(json.get("state").getAsString());
		int posX = json.get("posX").getAsInt();
		int posY = json.get("posY").getAsInt();
		return new FloatingWindowState(windowId, state, posX, posY);
	}
	
	@Override
	public String toString() {
		return String.format("FloatingWindowState{windowId='%s', state=%s, pos=(%d,%d)}", 
			windowId, state, posX, posY);
	}
}
