package com.focustimershop.network;

import com.focustimershop.FocusTimerShop;
import net.minecraft.util.Identifier;

/**
 * Network packet identifiers (v1.0.6 Phase 5)
 */
public class NetworkHandler {
	
	public static final Identifier EQUIP_TITLE_C2S = new Identifier(FocusTimerShop.MOD_ID, "equip_title");
	public static final Identifier ACHIEVEMENT_UNLOCK_S2C = new Identifier(FocusTimerShop.MOD_ID, "achievement_unlock");
	public static final Identifier TITLE_UNLOCK_S2C = new Identifier(FocusTimerShop.MOD_ID, "title_unlock");
}
