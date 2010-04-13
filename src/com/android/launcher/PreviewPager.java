package com.android.launcher;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PreviewPager extends ViewGroup {
	private int mTotalItems;
	private int mCurrentItem;
	private Drawable mDotDrawable;
	private ImageView mDot;
	public PreviewPager(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public PreviewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public PreviewPager(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub

	}

}
