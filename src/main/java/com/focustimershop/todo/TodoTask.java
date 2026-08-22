package com.focustimershop.todo;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * v1.0.7-beta - To-Do List Task Model
 * Represents a single task in the player's to-do list
 */
public class TodoTask {
	private final String id;
	private String text;
	private boolean completed;
	private int order; // Manual sort position among incomplete tasks
	
	public TodoTask(String id, String text, boolean completed, int order) {
		this.id = id;
		this.text = text;
		this.completed = completed;
		this.order = order;
	}
	
	public TodoTask(String text) {
		this(UUID.randomUUID().toString(), text, false, 0);
	}
	
	// Getters
	public String getId() {
		return id;
	}
	
	public String getText() {
		return text;
	}
	
	public boolean isCompleted() {
		return completed;
	}
	
	public int getOrder() {
		return order;
	}
	
	// Setters
	public void setText(String text) {
		this.text = text;
	}
	
	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	// Serialization
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("id", id);
		json.addProperty("text", text);
		json.addProperty("completed", completed);
		json.addProperty("order", order);
		return json;
	}
	
	public static TodoTask fromJson(JsonObject json) {
		String id = json.get("id").getAsString();
		String text = json.get("text").getAsString();
		boolean completed = json.get("completed").getAsBoolean();
		int order = json.has("order") ? json.get("order").getAsInt() : 0;
		return new TodoTask(id, text, completed, order);
	}
	
	@Override
	public String toString() {
		return String.format("TodoTask{id='%s', text='%s', completed=%s, order=%d}", 
			id, text, completed, order);
	}
}
