package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Main menu screen with Essential-inspired modern dark theme
 * Features: rounded corners, clean layout, tab navigation
 */
public class MainMenuScreen extends Screen {
	
	private static final int BG_COLOR = 0xE0101010;  // Dark background with transparency
	private static final int PANEL_COLOR = 0xE01A1A1A;  // Slightly lighter panel
	private static final int ACCENT_COLOR = 0xFF4A9EFF;  // Blue accent
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int SECONDARY_TEXT_COLOR = 0xFFAAAAAA;

	private GuiTab currentTab = GuiTab.TIMER;
	
	private TimerTabScreenV2 timerTab;  // v1.0.7-beta - New UI
	private ShopTabScreen shopTab;
	private BulkOrderTabScreen bulkOrderTab;  // v1.0.6-beta
	private LuckyChestTabScreen chestTab;
	private RentalTabScreen rentalTab;
	private ProfileTabScreen profileTab;  // v1.0.6
	
	// Music icon position for click detection
	private int musicIconX;
	private int musicIconY;
	private int musicIconWidth;

	public MainMenuScreen() {
		super(Text.literal("Focus Timer Shop"));
	}

	public net.minecraft.client.font.TextRenderer getTextRenderer() {
		return this.textRenderer;
	}

	@Override
	protected void init() {
		super.init();
		
		// Initialize tab screens only if not already initialized
		if (timerTab == null) {
			timerTab = new TimerTabScreenV2(this);  // v1.0.7-beta - New UI
		}
		if (shopTab == null) {
			shopTab = new ShopTabScreen(this);
		}
		if (bulkOrderTab == null) {
			bulkOrderTab = new BulkOrderTabScreen(this);  // v1.0.6-beta
		}
		if (chestTab == null) {
			chestTab = new LuckyChestTabScreen(this);
		}
		if (rentalTab == null) {
			rentalTab = new RentalTabScreen(this);
		}
		if (profileTab == null) {
			profileTab = new ProfileTabScreen(this);  // v1.0.6
		}

		// Note: Sidebar buttons are now rendered manually in render() method
		// No ButtonWidget needed - we handle clicks in mouseClicked()
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Check if Timer tab has a modal open - if so, render ONLY the modal with dim overlay
		if (currentTab == GuiTab.TIMER && timerTab != null && timerTab.isShowingModal()) {
			// Dim overlay covering ENTIRE screen (including sidebar)
			context.fill(0, 0, this.width, this.height, 0xDD000000);
			
			// Let timer tab render its modal content
			int contentX = 150;
			int contentY = 50;
			int contentWidth = this.width - 170;
			int contentHeight = this.height - 70;
			timerTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, delta);
			
			super.render(context, mouseX, mouseY, delta);
			return; // Skip normal rendering
		}
		
		// Normal rendering (no modal open)
		// Dark background
		renderBackground(context);

		// Main content panel (rounded rectangle simulation)
		int panelX = 140;
		int panelY = 40;
		int panelWidth = this.width - 160;
		int panelHeight = this.height - 60;
		
		context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);

		// Title bar
		context.drawText(this.textRenderer, "Focus Timer Shop", 20, 20, TEXT_COLOR, true);

		// Economy display (top right) - horizontal layout with proper icons and colors
		renderEconomyDisplay(context);

		// Render sidebar buttons
		renderSidebarButtons(context, mouseX, mouseY);

		// Render current tab content
		renderCurrentTab(context, mouseX, mouseY, delta);

		super.render(context, mouseX, mouseY, delta);
	}

	private void renderSidebarButtons(DrawContext context, int mouseX, int mouseY) {
		int sidebarX = 10;
		int tabY = 60;
		int tabWidth = 120;
		int tabHeight = 40;
		int spacing = 5;

		GuiTab[] tabs = {GuiTab.TIMER, GuiTab.SHOP, GuiTab.BULK_ORDER, GuiTab.LUCKY_CHEST, GuiTab.RENTAL, GuiTab.PROFILE};
		String[] icons = {"⏱", "🛒", "📦", "🎁", "🔧", "👤"};
		String[] labels = {"Timer", "Shop", "Bulk Order", "Lucky Chest", "Thuê", "Profile"};

		for (int i = 0; i < tabs.length; i++) {
			GuiTab tab = tabs[i];
			int btnY = tabY + i * (tabHeight + spacing);
			boolean active = (currentTab == tab);
			boolean hovered = mouseX >= sidebarX && mouseX <= sidebarX + tabWidth &&
			                  mouseY >= btnY && mouseY <= btnY + tabHeight;

			// Background color
			int bgColor = active ? ACCENT_COLOR : (hovered ? 0xFF2A2A2A : 0xFF1A1A1A);
			context.fill(sidebarX, btnY, sidebarX + tabWidth, btnY + tabHeight, bgColor);

			// White border when active (matching Timer style from prompt)
			if (active) {
				context.fill(sidebarX, btnY, sidebarX + tabWidth, btnY + 2, 0xFFFFFFFF); // top
				context.fill(sidebarX, btnY + tabHeight - 2, sidebarX + tabWidth, btnY + tabHeight, 0xFFFFFFFF); // bottom
				context.fill(sidebarX, btnY, sidebarX + 2, btnY + tabHeight, 0xFFFFFFFF); // left
				context.fill(sidebarX + tabWidth - 2, btnY, sidebarX + tabWidth, btnY + tabHeight, 0xFFFFFFFF); // right
			}

			// Icon + text centered
			String buttonText = icons[i] + " " + labels[i];
			int textWidth = this.textRenderer.getWidth(buttonText);
			int textX = sidebarX + (tabWidth - textWidth) / 2;
			int textY = btnY + (tabHeight - this.textRenderer.fontHeight) / 2;
			
			context.drawText(this.textRenderer, buttonText, textX, textY, TEXT_COLOR, true);
		}
	}

	private void renderEconomyDisplay(DrawContext context) {
		// Icons: ◉ for coins, ✦ for XP (Unicode symbols that render well in Minecraft font)
		String silverIcon = "◉";  // Silver coin
		String goldIcon = "◉";    // Gold coin  
		String xpIcon = "✦";      // Star for XP
		String musicIcon = "♪";   // Music note
		
		// Colors from spec
		int silverColor = 0xFFC0C0C0;  // #C0C0C0
		int goldColor = 0xFFFFD700;    // #FFD700
		int xpColor = 0xFFFF8C00;      // #FF8C00
		int separatorColor = 0xFF666666;
		
		// Music icon color - check if music is playing
		boolean isMusicPlaying = com.focustimershop.music.MusicPlayerManager.isPlaying();
		int musicColor = isMusicPlaying ? 0xFFFFD700 : 0xFF666666;  // Gold when playing, gray when muted/stopped
		
		// Get values
		long silver = ClientDataCache.getSilverCoins();
		long gold = ClientDataCache.getGoldCoins();
		long xp = ClientDataCache.getFocusXp();
		
		// Build strings
		String silverText = silverIcon + " " + silver;
		String goldText = goldIcon + " " + gold;
		String xpText = xpIcon + " " + xp;
		String separator = " ✦ ";  // Use star as separator too
		
		// Scale factor: 1.5× larger
		float scale = 1.5f;
		
		// Calculate widths at normal size first (for positioning)
		int silverWidth = (int)(this.textRenderer.getWidth(silverText) * scale);
		int separatorWidth = (int)(this.textRenderer.getWidth(separator) * scale);
		int goldWidth = (int)(this.textRenderer.getWidth(goldText) * scale);
		int xpWidth = (int)(this.textRenderer.getWidth(xpText) * scale);
		int musicWidth = (int)(this.textRenderer.getWidth(musicIcon) * scale);
		
		int totalWidth = silverWidth + separatorWidth + goldWidth + separatorWidth + xpWidth + separatorWidth + musicWidth;
		
		// Position: top right with padding
		int economyX = this.width - totalWidth - 20;
		int economyY = 20;
		
		// Render with scaling using matrix stack
		var matrices = context.getMatrices();
		int currentX = economyX;
		
		// Silver
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, silverText, (int)(currentX / scale), (int)(economyY / scale), silverColor, true);
		matrices.pop();
		currentX += silverWidth;
		
		// Separator 1
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, separator, (int)(currentX / scale), (int)(economyY / scale), separatorColor, false);
		matrices.pop();
		currentX += separatorWidth;
		
		// Gold
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, goldText, (int)(currentX / scale), (int)(economyY / scale), goldColor, true);
		matrices.pop();
		currentX += goldWidth;
		
		// Separator 2
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, separator, (int)(currentX / scale), (int)(economyY / scale), separatorColor, false);
		matrices.pop();
		currentX += separatorWidth;
		
		// XP
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, xpText, (int)(currentX / scale), (int)(economyY / scale), xpColor, true);
		matrices.pop();
		currentX += xpWidth;
		
		// Separator 3
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, separator, (int)(currentX / scale), (int)(economyY / scale), separatorColor, false);
		matrices.pop();
		currentX += separatorWidth;
		
		// Music icon (last in row)
		matrices.push();
		matrices.scale(scale, scale, 1.0f);
		context.drawText(this.textRenderer, musicIcon, (int)(currentX / scale), (int)(economyY / scale), musicColor, true);
		matrices.pop();
		
		// Store music icon position for click detection (in field variable)
		this.musicIconX = currentX;
		this.musicIconY = economyY;
		this.musicIconWidth = musicWidth;
	}
	
	private void renderCurrentTab(DrawContext context, int mouseX, int mouseY, float delta) {
		int contentX = 150;
		int contentY = 50;
		int contentWidth = this.width - 170;
		int contentHeight = this.height - 70;

		switch (currentTab) {
			case TIMER:
				timerTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, delta);
				break;
			case SHOP:
				shopTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, delta);
				break;
			case BULK_ORDER:
				bulkOrderTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY);
				break;
			case LUCKY_CHEST:
				chestTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, delta);
				break;
			case RENTAL:
				rentalTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, delta);
				break;
			case PROFILE:  // v1.0.6
				profileTab.render(context, contentX, contentY, contentWidth, contentHeight, mouseX, mouseY, delta);
				break;
		}
	}

	@Override
	public boolean shouldPause() {
		// Don't pause game when this screen is open (unless timer is running)
		return ClientDataCache.isGameFrozen();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		// If timer is running, don't allow ESC to close
		return !ClientDataCache.isGameFrozen();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Check music icon click (now in top bar with currency display)
		float scale = 1.5f;
		int iconHeight = (int)(this.textRenderer.fontHeight * scale);
		
		if (mouseX >= musicIconX && mouseX <= musicIconX + musicIconWidth &&
		    mouseY >= musicIconY && mouseY <= musicIconY + iconHeight) {
			// Open music player
			if (this.client != null) {
				this.client.setScreen(new MusicPlayerScreen(this));
			}
			return true;
		}
		
		// Check sidebar button clicks
		int sidebarX = 10;
		int tabY = 60;
		int tabWidth = 120;
		int tabHeight = 40;
		int spacing = 5;

		GuiTab[] tabs = {GuiTab.TIMER, GuiTab.SHOP, GuiTab.BULK_ORDER, GuiTab.LUCKY_CHEST, GuiTab.RENTAL, GuiTab.PROFILE};
		for (int i = 0; i < tabs.length; i++) {
			int btnY = tabY + i * (tabHeight + spacing);
			if (mouseX >= sidebarX && mouseX <= sidebarX + tabWidth &&
			    mouseY >= btnY && mouseY <= btnY + tabHeight) {
				currentTab = tabs[i];
				return true;
			}
		}

		// Forward clicks to current tab
		int contentX = 150;
		int contentY = 50;
		int contentWidth = this.width - 170;
		int contentHeight = this.height - 70;

		boolean handled = false;
		switch (currentTab) {
			case TIMER:
				handled = timerTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentWidth, contentHeight);
				break;
			case SHOP:
				handled = shopTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentWidth, contentHeight);
				break;
			case BULK_ORDER:
				handled = bulkOrderTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentWidth, contentHeight);
				break;
			case LUCKY_CHEST:
				handled = chestTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentWidth, contentHeight);
				break;
			case RENTAL:
				handled = rentalTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentWidth, contentHeight);
				break;
			case PROFILE:  // v1.0.6
				handled = profileTab.mouseClicked(mouseX, mouseY, button, contentX, contentY, contentWidth, contentHeight);
				break;
		}

		if (handled) {
			return true;
		}

		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (currentTab == GuiTab.SHOP) {
			return shopTab.mouseScrolled(mouseX, mouseY, 0, amount);
		} else if (currentTab == GuiTab.BULK_ORDER) {
			return bulkOrderTab.mouseScrolled(mouseX, mouseY, 0, amount);
		} else if (currentTab == GuiTab.LUCKY_CHEST) {
			return chestTab.mouseScrolled(mouseX, mouseY, amount);
		} else if (currentTab == GuiTab.PROFILE) {
			return profileTab.mouseScrolled(mouseX, mouseY, amount);
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (currentTab == GuiTab.SHOP) {
			return shopTab.mouseReleased(mouseX, mouseY, button);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (currentTab == GuiTab.SHOP) {
			return shopTab.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Forward keyboard input to current tab
		if (currentTab == GuiTab.SHOP) {
			if (shopTab.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		} else if (currentTab == GuiTab.BULK_ORDER) {
			if (bulkOrderTab.keyPressed(keyCode, scanCode, modifiers)) {
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean charTyped(char chr, int modifiers) {
		// Forward character typing to current tab
		if (currentTab == GuiTab.SHOP) {
			if (shopTab.charTyped(chr, modifiers)) {
				return true;
			}
		} else if (currentTab == GuiTab.BULK_ORDER) {
			if (bulkOrderTab.charTyped(chr, modifiers)) {
				return true;
			}
		}
		return super.charTyped(chr, modifiers);
	}

	enum GuiTab {
		TIMER,
		SHOP,
		BULK_ORDER,  // v1.0.6-beta
		LUCKY_CHEST,
		RENTAL,
		PROFILE  // v1.0.6
	}
}
