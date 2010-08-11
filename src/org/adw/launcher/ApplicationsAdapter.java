/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.adw.launcher;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.adw.launcher.catalogue.AppGrpUtils;

/**
 * GridView adapter to show the list of applications and shortcuts
 */
public class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
	private final LayoutInflater mInflater;
	private Drawable mBackground;
	private int mTextColor = 0;
	private boolean useThemeTextColor = false;
    private Typeface themeFont=null;
	public static ArrayList<ApplicationInfo> allItems = new ArrayList<ApplicationInfo>();
	public static ArrayList<ApplicationInfo> filtered = new ArrayList<ApplicationInfo>();
	private CatalogueFilter filter;
    private static final Collator sCollator = Collator.getInstance();

	public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
		super(context, 0, apps);

		mInflater = LayoutInflater.from(context);
		// ADW: Load textcolor and bubble color from theme
		String themePackage = AlmostNexusSettingsHelper.getThemePackageName(
				getContext(), Launcher.THEME_DEFAULT);
		if (!themePackage.equals(Launcher.THEME_DEFAULT)) {
			Resources themeResources = null;
			try {
				themeResources = getContext().getPackageManager()
						.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// e.printStackTrace();
			}
			if (themeResources != null) {
				int textColorId = themeResources.getIdentifier(
						"drawer_text_color", "color", themePackage);
				if (textColorId != 0) {
					mTextColor = themeResources.getColor(textColorId);
					useThemeTextColor = true;
				}
				mBackground = IconHighlights.getDrawable(getContext(),
						IconHighlights.TYPE_DRAWER);
    			try{
    				themeFont=Typeface.createFromAsset(themeResources.getAssets(), "themefont.ttf");
    			}catch (RuntimeException e) {
					// TODO: handle exception
				}
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ApplicationInfo info = getItem(position);

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.application_boxed, parent,
					false);
		}

		if (!info.filtered) {
			info.icon = Utilities.createIconThumbnail(info.icon, getContext());
			info.filtered = true;
		}

		final TextView textView = (TextView) convertView;
		textView.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null,
				null);
		textView.setText(info.title);
		if (useThemeTextColor) {
			textView.setTextColor(mTextColor);
		}
		//ADW: Custom font
		if(themeFont!=null) textView.setTypeface(themeFont);
		// so i'd better not use it, sorry themers
		if (mBackground != null)
			convertView.setBackgroundDrawable(mBackground);
		return convertView;
	}


	@Override
	public void add(ApplicationInfo info) {
		boolean changed = false;
		//check allItems before added. It is a fix for all of the multi-icon issue, but will 
		//lose performance. Anyway, we do not expected to have many applications.
		synchronized (allItems) {
			if (!allItems.contains(info)) {
				changed = true;
				allItems.add(info);
				Collections.sort(allItems,new ApplicationInfoComparator());
			}
		}
//		Log.v("added",info.intent.getComponent().flattenToString()+" "+allItems.size());
		if (changed) updateDataSet();
	}

	//2 super functions, to make sure related add/clear do not affect allItems.
	//in current Froyo/Eclair, it is not necessary.
	void superAdd(ApplicationInfo info) {
		String s = info.intent.getComponent().flattenToString();
		if (appInGroup(s)) {
			super.add(info);
		}
	}
	
	void superClear() {
		super.clear();
	}
	
	@Override
	public void remove(ApplicationInfo info) {
		synchronized (allItems) {
			allItems.remove(info);
		}
		updateDataSet();
	}

	private boolean appInGroup(String s) {
		return AppGrpUtils.checkAppInGroup(s);
	}

	private void filterApps(ArrayList<ApplicationInfo> theFiltered,
			ArrayList<ApplicationInfo> theItems) {
		theFiltered.clear();
		//AppGrpUtils.checkAndInitGrp();
		if (theItems != null) {
			int length = theItems.size();

			for (int i = 0; i < length; i++) {
				ApplicationInfo info = theItems.get(i);
				String s = info.intent.getComponent().flattenToString();
				
				if (appInGroup(s)) {
					theFiltered.add(info);
				}
			}
		}
	}
	//filter,sort,update
    public void updateDataSet()
	{
        getFilter().filter(null);
	}
    
	@Override
	public Filter getFilter() {
		if (filter == null)
			filter = new CatalogueFilter();
		return filter;
	}

	private class CatalogueFilter extends Filter {
		ArrayList<ApplicationInfo> filt = new ArrayList<ApplicationInfo>();
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {

			FilterResults result = new FilterResults();
			filt.clear();
			
			synchronized (allItems) {
				filterApps(filt, allItems);
			}
			
			result.values = filt;
			result.count = filt.size();
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			// NOTE: this function is *always* called from the UI thread.
			filtered = (ArrayList<ApplicationInfo>) results.values;

			setNotifyOnChange(false);
			
			superClear();
			// there could be a serious sync issue.
			// very bad
			int l = filtered.size(); 
			for (int i = 0;i < l; i++) {
				superAdd(filtered.get(i));
			}
			
			notifyDataSetChanged();

		}
	}
    static class ApplicationInfoComparator implements Comparator<ApplicationInfo> {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            return sCollator.compare(a.title.toString(), b.title.toString());
        }
    }	
}
