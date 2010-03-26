package com.android.launcher;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
public final class AlmostNexusSettingsHelper {
	private static final String ALMOSTNEXUS_PREFERENCES = "launcher.preferences.almostnexus";
	
	public static int getDesktopScreens(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, context.MODE_PRIVATE);
		int screens = sp.getInt("desktopScreens", 3)+2;
		return screens;
	}
	public static int getDefaultScreen(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, context.MODE_PRIVATE);
		int screens = sp.getInt("defaultScreen", 0);
		return screens;
	}
	public static int getColumnsPortrait(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, context.MODE_PRIVATE);
		int screens = sp.getInt("drawerColumnsPortrait", 3)+1;
		return screens;
	}
	public static int getColumnsLandscape(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, context.MODE_PRIVATE);
		int screens = sp.getInt("drawerColumnsLandscape", 4)+1;
		return screens;
	}
	public static boolean getDrawerAnimated(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, context.MODE_PRIVATE);
		boolean animated = sp.getBoolean("drawerAnimated", true);
		return animated;
	}
	public static boolean getDrawerFast(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, context.MODE_PRIVATE);
		boolean fast = sp.getBoolean("drawerFast", false);
		return fast;
	}
}
