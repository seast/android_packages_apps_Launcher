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

package com.android.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class WallpaperChooser extends Activity implements AdapterView.OnItemSelectedListener,
        OnClickListener {

    private static final Integer[] THUMB_IDS = {
	    R.drawable.wallpaper_skate_small,
        R.drawable.wallpaper_prash_nexus_surf_small,
	    R.drawable.wallpaper_cyan_small,
	    R.drawable.wallpaper_prash_arrowd_blue_small,
	    R.drawable.wallpaper_prash_arrowd_green_small,
	    R.drawable.wallpaper_prash_arrowd_pink_small,
	    R.drawable.wallpaper_cm_nexus_08_small,
	    R.drawable.wallpaper_electric_small,
	    R.drawable.wallpaper_grass_small,
	    R.drawable.wallpaper_canyon_small,
	    R.drawable.wallpaper_monumentvalley_small,
	    R.drawable.wallpaper_tree_small,
	    R.drawable.wallpaper_zanzibar_small,
	    R.drawable.wallpaper_field_small,
	    R.drawable.wallpaper_cloud_small,
	    R.drawable.wallpaper_desert_small,
	    R.drawable.wallpaper_goldengate_small,
	    R.drawable.wallpaper_despair_small,
	    R.drawable.wallpaper_grass_night_small,
	    R.drawable.wallpaper_galaxy_small,
	    R.drawable.wallpaper_x67_small,
	    R.drawable.wallpaper_nexusrain_small,
	    R.drawable.wallpaper_nexuspattern_small,
	    R.drawable.wallpaper_nexuswallpaper1_small,
	    R.drawable.wallpaper_brown_small,
	    R.drawable.wallpaper_pcut_small,
	    R.drawable.wallpaper_bluedotgrid_small,
	    R.drawable.wallpaper_hazybluedots_small,
	    R.drawable.wallpaper_ropelights_small,
	    R.drawable.wallpaper_prash_cm_girls_small

    };

    private static final Integer[] IMAGE_IDS = {
	    R.drawable.wallpaper_skate,
        R.drawable.wallpaper_prash_nexus_surf,
	    R.drawable.wallpaper_cyan,
	    R.drawable.wallpaper_prash_arrowd_blue,
	    R.drawable.wallpaper_prash_arrowd_green,
	    R.drawable.wallpaper_prash_arrowd_pink,
	    R.drawable.wallpaper_cm_nexus_08,
	    R.drawable.wallpaper_electric,
	    R.drawable.wallpaper_grass,
	    R.drawable.wallpaper_canyon,
	    R.drawable.wallpaper_monumentvalley,
	    R.drawable.wallpaper_tree,
	    R.drawable.wallpaper_zanzibar,
	    R.drawable.wallpaper_field,
	    R.drawable.wallpaper_cloud,
	    R.drawable.wallpaper_desert,
	    R.drawable.wallpaper_goldengate,
	    R.drawable.wallpaper_despair,
	    R.drawable.wallpaper_grass_night,
	    R.drawable.wallpaper_galaxy,
	    R.drawable.wallpaper_x67,
	    R.drawable.wallpaper_nexusrain,
	    R.drawable.wallpaper_nexuspattern,
	    R.drawable.wallpaper_nexuswallpaper1,
	    R.drawable.wallpaper_brown,
	    R.drawable.wallpaper_pcut,
	    R.drawable.wallpaper_bluedotgrid,
	    R.drawable.wallpaper_hazybluedots,
	    R.drawable.wallpaper_ropelights,
	    R.drawable.wallpaper_prash_cm_girls
    };

    private Gallery mGallery;
    private ImageView mImageView;
    private boolean mIsWallpaperSet;

    private Bitmap mBitmap;

    private ArrayList<Integer> mThumbs;
    private ArrayList<Integer> mImages;
    private WallpaperLoader mLoader;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        findWallpapers();

        setContentView(R.layout.wallpaper_chooser);

        mGallery = (Gallery) findViewById(R.id.gallery);
        mGallery.setAdapter(new ImageAdapter(this));
        mGallery.setOnItemSelectedListener(this);
        mGallery.setCallbackDuringFling(false);

        findViewById(R.id.set).setOnClickListener(this);

        mImageView = (ImageView) findViewById(R.id.wallpaper);
    }

    private void findWallpapers() {
        mThumbs = new ArrayList<Integer>(THUMB_IDS.length + 4);
        Collections.addAll(mThumbs, THUMB_IDS);

        mImages = new ArrayList<Integer>(IMAGE_IDS.length + 4);
        Collections.addAll(mImages, IMAGE_IDS);

        final Resources resources = getResources();
        final String[] extras = resources.getStringArray(R.array.extra_wallpapers);
        final String packageName = getApplication().getPackageName();

        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0) {
                final int thumbRes = resources.getIdentifier(extra + "_small",
                        "drawable", packageName);

                if (thumbRes != 0) {
                    mThumbs.add(thumbRes);
                    mImages.add(res);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsWallpaperSet = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (mLoader != null && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel(true);
            mLoader = null;
        }
    }

    public void onItemSelected(AdapterView parent, View v, int position, long id) {
        if (mLoader != null && mLoader.getStatus() != WallpaperLoader.Status.FINISHED) {
            mLoader.cancel();
        }
        mLoader = (WallpaperLoader) new WallpaperLoader().execute(position);
    }

    /*
     * When using touch if you tap an image it triggers both the onItemClick and
     * the onTouchEvent causing the wallpaper to be set twice. Ensure we only
     * set the wallpaper once.
     */
    private void selectWallpaper(int position) {
        if (mIsWallpaperSet) {
            return;
        }

        mIsWallpaperSet = true;
        try {
            InputStream stream = getResources().openRawResource(mImages.get(position));
            setWallpaper(stream);
            setResult(RESULT_OK);
            finish();
        } catch (IOException e) {
            Log.e(Launcher.LOG_TAG, "Failed to set wallpaper: " + e);
        }
    }

    public void onNothingSelected(AdapterView parent) {
    }

    private class ImageAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(WallpaperChooser context) {
            mLayoutInflater = context.getLayoutInflater();
        }

        public int getCount() {
            return mThumbs.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView image;

            if (convertView == null) {
                image = (ImageView) mLayoutInflater.inflate(R.layout.wallpaper_item, parent, false);
            } else {
                image = (ImageView) convertView;
            }

            image.setImageResource(mThumbs.get(position));
            image.getDrawable().setDither(true);
            return image;
        }
    }

    public void onClick(View v) {
        selectWallpaper(mGallery.getSelectedItemPosition());
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        BitmapFactory.Options mOptions;

        WallpaperLoader() {
            mOptions = new BitmapFactory.Options();
            mOptions.inDither = false;
            mOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;            
        }
        
        protected Bitmap doInBackground(Integer... params) {
            if (isCancelled()) return null;
            try {
                return BitmapFactory.decodeResource(getResources(),
                        mImages.get(params[0]), mOptions);
            } catch (OutOfMemoryError e) {
                return null;
            }            
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b == null) return;

            if (!isCancelled() && !mOptions.mCancel) {
                // Help the GC
                if (mBitmap != null) {
                    mBitmap.recycle();
                }
    
                final ImageView view = mImageView;
                view.setImageBitmap(b);
    
                mBitmap = b;
    
                final Drawable drawable = view.getDrawable();
                drawable.setFilterBitmap(true);
                drawable.setDither(true);

                view.postInvalidate();

                mLoader = null;
            } else {
               b.recycle(); 
            }
        }

        void cancel() {
            mOptions.requestCancelDecode();
            super.cancel(true);
        }
    }
}
