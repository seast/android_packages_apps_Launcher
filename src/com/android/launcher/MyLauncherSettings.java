package com.android.launcher;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class MyLauncherSettings extends PreferenceActivity implements OnPreferenceChangeListener {
    private static final String ALMOSTNEXUS_PREFERENCES = "launcher.preferences.almostnexus";
    private boolean shouldRestart=false;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		//TODO: ADW should i read stored values after addPreferencesFromResource?
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(ALMOSTNEXUS_PREFERENCES);
        addPreferencesFromResource(R.xml.launcher_settings);
        dlgSeekBarPreference desktopScreens= (dlgSeekBarPreference) findPreference("desktopScreens");
        desktopScreens.setMin(2);
        desktopScreens.setOnPreferenceChangeListener(this);
        dlgSeekBarPreference defaultScreen= (dlgSeekBarPreference) findPreference("defaultScreen");
        defaultScreen.setMin(1);
        defaultScreen.setMax(AlmostNexusSettingsHelper.getDesktopScreens(this)-1);
        defaultScreen.setOnPreferenceChangeListener(this);
        Preference drawerFast = (Preference) findPreference("drawerFast");
        drawerFast.setOnPreferenceChangeListener(this);
        dlgSeekBarPreference columnsPortrait= (dlgSeekBarPreference) findPreference("drawerColumnsPortrait");
        columnsPortrait.setMin(1);
        dlgSeekBarPreference columnsLandscape= (dlgSeekBarPreference) findPreference("drawerColumnsLandscape");
        columnsLandscape.setMin(1);
    }
	@Override
	protected void onPause(){
		if(shouldRestart){
   			ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
	        am.restartPackage("com.android.launcher");	
		}
		super.onPause();
	}
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("desktopScreens")) {
			dlgSeekBarPreference pref = (dlgSeekBarPreference) findPreference("defaultScreen");
			pref.setMax((Integer) newValue+1);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("This setting will cause launcher to restart")
			       .setCancelable(false)
			       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
							shouldRestart=true;
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();
		}else if (preference.getKey().equals("defaultScreen")){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("This setting will cause launcher to restart")
			       .setCancelable(false)
			       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
							shouldRestart=true;
			           }
			       });
			AlertDialog alert = builder.create();
			alert.show();		
		}else if(preference.getKey().equals("drawerFast")){
			boolean val=Boolean.parseBoolean(newValue.toString());
			if(!val){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Setting this to off will cause launcher to restart")
				       .setCancelable(false)
				       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
								shouldRestart=true;
				           }
				       });
				AlertDialog alert = builder.create();
				alert.show();
			}else{
				shouldRestart=false;
			}			
		}
        return true;  
	}
    
}
