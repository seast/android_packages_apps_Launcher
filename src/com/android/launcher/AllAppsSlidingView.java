package com.android.launcher;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Debug;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
public class AllAppsSlidingView extends AdapterView<ApplicationsAdapter> implements OnItemClickListener, OnItemLongClickListener, DragSource{// implements DragScroller{
    private static final int DEFAULT_SCREEN = 0;
    private static final int INVALID_SCREEN = -1;
    private static final int SNAP_VELOCITY = 1000;
    
    private int mCurrentScreen;
    private int mTotalScreens;
    private int mCurrentHolder;
    private int mPageWidth;
    private int mDefaultScreen=DEFAULT_SCREEN;
    private int mNextScreen = INVALID_SCREEN;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private float mLastMotionX;
    private float mLastMotionY;

    static final int TOUCH_STATE_DOWN = 3;
    static final int TOUCH_STATE_TAP = 4;
    static final int TOUCH_STATE_DONE_WAITING = 5;

    
    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    private final static int TOUCH_STATE_TRYSCROLL = 2;
    private int mTouchState = TOUCH_STATE_REST;
    private int mTouchSlop;
    private int mMaximumVelocity;
    private Launcher mLauncher;
    private DragController mDragger;
    private boolean mFirstLayout = true;
	private boolean mAllowLongPress = false;
	private ApplicationsAdapter mAdapter;
    /**
     * Should be used by subclasses to listen to changes in the dataset
     */
    AdapterDataSetObserver mDataSetObserver;
	public boolean mDataChanged;
	public int mItemCount;
	public int mOldItemCount;
    
    
	private int mNumColumns=2;
	private int mNumRows=2;
	private int paginatorSpace=25;
    static final int LAYOUT_NORMAL = 0;
    static final int LAYOUT_SCROLLING = 1;
    int mLayoutMode = LAYOUT_NORMAL;

    /**
     * Should be used by subclasses to listen to changes in the dataset
     */
    /**
     * Indicates whether the list selector should be drawn on top of the children or behind
     */
    boolean mDrawSelectorOnTop = false;

    /**
     * The drawable used to draw the selector
     */
    Drawable mSelector;

    /**
     * Defines the selector's location and dimension at drawing time
     */
    Rect mSelectorRect = new Rect();
    /**
     * The selection's left padding
     */
    int mSelectionLeftPadding = 0;

    /**
     * The selection's top padding
     */
    int mSelectionTopPadding = 0;

    /**
     * The selection's right padding
     */
    int mSelectionRightPadding = 0;

    /**
     * The selection's bottom padding
     */
    int mSelectionBottomPadding = 0;
    /**
     * The last CheckForLongPress runnable we posted, if any
     */
    private CheckForLongPress mPendingCheckForLongPress;

    /**
     * The last CheckForTap runnable we posted, if any
     */
    private Runnable mPendingCheckForTap;

    /**
     * The last CheckForKeyLongPress runnable we posted, if any
     */
    private CheckForKeyLongPress mPendingCheckForKeyLongPress;
	private int mCheckTapPosition;
	private int mSelectedPosition= INVALID_POSITION;
    /**
     * Acts upon click
     */
    private AllAppsSlidingView.PerformClick mPerformClick;
    /**
     * The data set used to store unused views that should be reused during the next layout
     * to avoid creating new ones
     */
    final RecycleBin mRecycler = new RecycleBin();
    //ADW:Hack the texture thing to make scrolling faster
    private boolean forceOpaque=false;
    private Bitmap mTexture;
    private Paint mPaint;
    private int mTextureWidth;
    private int mTextureHeight;
	private int mCacheColorHint=0;
	private boolean scrollCacheCreated;
	private boolean mBlockLayouts;
    
	public AllAppsSlidingView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		initWorkspace();
	}
	public AllAppsSlidingView(Context context, AttributeSet attrs) {
		//super(context, attrs);
		// TODO Auto-generated constructor stub
        //initWorkspace();
		this(context, attrs, com.android.internal.R.attr.absListViewStyle);
	}
	public AllAppsSlidingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		initWorkspace();
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.AbsListView, defStyle, 0);
        //TODO: ADW-Check if it's necessary
        boolean bootOpaque=AlmostNexusSettingsHelper.getDrawerFast(context);
        //ADW force the hack
        forceOpaque=!bootOpaque;
        setForceOpaque(bootOpaque);
        //final int textureId = a.getResourceId(R.styleable.AllAppsSlidingView_texture, 0);
    	//final int textureId = R.drawable.pattern_carbon_fiber_dark;
        //if (textureId != 0) {
            //mTexture = BitmapFactory.decodeResource(getResources(), textureId);
            //mTextureWidth = mTexture.getWidth();
            //mTextureHeight = mTexture.getHeight();

            //mPaint = new Paint();
            //mPaint.setDither(false);
        //}
        a.recycle();

        Drawable d = a.getDrawable(com.android.internal.R.styleable.AbsListView_listSelector);
        if (d != null) {
            setSelector(d);
        }

        mDrawSelectorOnTop = a.getBoolean(
                com.android.internal.R.styleable.AbsListView_drawSelectorOnTop, false);

        /*int transcriptMode = a.getInt(R.styleable.AbsListView_transcriptMode,
                TRANSCRIPT_MODE_DISABLED);
        setTranscriptMode(transcriptMode);
		*/
        int color = a.getColor(com.android.internal.R.styleable.AbsListView_cacheColorHint, 0x00000000);
        setCacheColorHint(color);

        /*boolean enableFastScroll = a.getBoolean(R.styleable.AbsListView_fastScrollEnabled, false);
        setFastScrollEnabled(enableFastScroll);

        boolean smoothScrollbar = a.getBoolean(R.styleable.AbsListView_smoothScrollbar, true);
        setSmoothScrollbarEnabled(smoothScrollbar);*/

        a.recycle();
		
	}
    @Override
    public boolean isOpaque() {
    	return true;
    	/*if(forceOpaque){
    		return true;
    	}else if(mTexture!=null){
    		return !mTexture.hasAlpha();
    	}else{
    		return false;
    	}*/
    }
	
    private void initWorkspace() {
        mDrawSelectorOnTop = false;
    	//setFocusable(true);
    	//setFocusableInTouchMode(true);  
        setWillNotDraw(false);
        setAlwaysDrawnWithCacheEnabled(false);
        setChildrenDrawnWithCacheEnabled(true);
        setChildrenDrawingCacheEnabled(true);        
        //setScrollingCacheEnabled(true);
        mScroller = new Scroller(getContext());
        mCurrentScreen = mDefaultScreen;
        mScroller.forceFinished(true);
        mPaint = new Paint();
        mPaint.setDither(false);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }
    @Override
    protected void onFinishInflate() {
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }
    
    void setLauncher(Launcher launcher) {
        mLauncher = launcher;        
    }    
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(),mScroller.getCurrY());
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
            if(mCurrentScreen!=Math.max(0, Math.min(mNextScreen, mTotalScreens - 1))){
	        	mCurrentScreen = Math.max(0, Math.min(mNextScreen, mTotalScreens - 1));
	            mNextScreen = INVALID_SCREEN;
	            //Log.d("MyApps","computeScroll ended?");
	            //mFirstPosition=mCurrentScreen*mNumColumns*mNumRows;
	            //Log.d("MyApps","my mFirstPosition="+mFirstPosition);
	            //RecycleOuterViews(mCurrentScreen);
	        	mLayoutMode=LAYOUT_NORMAL;
	        	//clearScrollingCache();
	            clearChildrenCache();
	            mBlockLayouts=false;
	            requestLayout();
            }
        }
    }
    private void drawChildren(Canvas canvas, int screen,long drawingTime){
    	//final int realScreen=((screen)%3); 
    	final int startPos=screen*mNumColumns*mNumRows;
    	int lastPos=startPos+(mNumColumns*mNumRows)-1;
    	if(lastPos>getChildCount()-1){
    		lastPos=getChildCount()-1;
    	}
    	Log.d("MyApps","We need to draw children from "+startPos+" to "+lastPos);
    	for(int i=startPos;i<=lastPos;i++){
    		drawChild(canvas, getChildAt(i), drawingTime);
    		//drawChild(canvas,getViewAtPosition(i),drawingTime);
    	}
    	/*for(int i=0;i<getChildCount();i++){
    		drawChild(canvas, getChildAt(i), drawingTime);
    	}*/
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int saveCount = 0;
    	//if(!forceOpaque){
        if(mTexture!=null && !forceOpaque){
	    	final Bitmap texture = mTexture;
	        final Paint paint = mPaint;
	
	        final int width = getWidth();
	        final int height = getHeight();
	
	        final int textureWidth = mTextureWidth;
	        final int textureHeight = mTextureHeight;
	
	        int x = getScrollX();//0;
	        int y;
	
	        while ((x-getScrollX()) < width) {
	            y = 0;
	            while (y < height) {
	                canvas.drawBitmap(texture, x, y, paint);
	                y += textureHeight;
	            }
	            x += textureWidth;
	        }
    	}

        final boolean clipToPadding = (mGroupFlags & CLIP_TO_PADDING_MASK) == CLIP_TO_PADDING_MASK;
        if (clipToPadding) {
            saveCount = canvas.save();
            final int scrollX = mScrollX;
            final int scrollY = mScrollY;
            canvas.clipRect(scrollX + mPaddingLeft, scrollY + mPaddingTop,
                    scrollX + mRight - mLeft - mPaddingRight,
                    scrollY + mBottom - mTop - mPaddingBottom);
            mGroupFlags &= ~CLIP_TO_PADDING_MASK;
        }

        final boolean drawSelectorOnTop = mDrawSelectorOnTop;
        if (!drawSelectorOnTop) {
            drawSelector(canvas);
        }

        super.dispatchDraw(canvas);

        if (drawSelectorOnTop) {
            drawSelector(canvas);
        }

        if (clipToPadding) {
            canvas.restoreToCount(saveCount);
            mGroupFlags |= CLIP_TO_PADDING_MASK;
        }
    }
    
    /*@Override
    protected void dispatchDraw(Canvas canvas) {
        //TODO: ADW-Check if this is necessary
    	Log.d("NOOOOOOOOOOOO","DispatchDraw!");
    	if(!forceOpaque){
	    	final Bitmap texture = mTexture;
	        final Paint paint = mPaint;
	
	        final int width = getWidth();
	        final int height = getHeight();
	
	        final int textureWidth = mTextureWidth;
	        final int textureHeight = mTextureHeight;
	
	        int x = getScrollX();//0;
	        int y;
	
	        while ((x-getScrollX()) < width) {
	            y = 0;
	            while (y < height) {
	                canvas.drawBitmap(texture, x, y, paint);
	                y += textureHeight;
	            }
	            x += textureWidth;
	        }
    	}
    	
        boolean restore = false;

        // ViewGroup.dispatchDraw() supports many features we don't need:
        // clip to padding, layout animation, animation listener, disappearing
        // children, etc. The following implementation attempts to fast-track
        // the drawing dispatch by drawing only what we know needs to be drawn.
        if(getChildCount()>0){        
	        boolean fastDraw = mTouchState != TOUCH_STATE_SCROLLING && mNextScreen == INVALID_SCREEN;
	        // If we are not scrolling or flinging, draw only the current screen
            //TODO:ADW-Find icons for screens to draw
	        if (!mDrawSelectorOnTop) {
	            drawSelector(canvas);
	        }
	        int realScreen=0;
	        if(mCurrentScreen>0){
	        	realScreen=1;
	        }
	        if (fastDraw) {
	        	drawChild(canvas, getChildAt(realScreen), getDrawingTime());
	        } else {
	            final long drawingTime = getDrawingTime();
                final int count = getChildCount();
                for (int i = 0; i < count; i++) {
                    drawChild(canvas, getChildAt(i), drawingTime);
                }
	        }
	        
        }else{
        	Log.d("MyAPPS","Not redrawing cause of no children");        	
        }

        if (restore) {
            canvas.restore();
        }
    }*/
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
        mPageWidth=widthSize;
    }
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	//Log.d("AAAAAAAAAAAAAAAARRRRRRRRRGGGGGGGG","onLayout!!");
    	/*Log.d("AAAAAAAAAAAAAAAARRRRRRRRRGGGGGGGG","--------->Changed?->"+changed);
    	Log.d("AAAAAAAAAAAAAAAARRRRRRRRRGGGGGGGG","--------->left?->"+left);
    	Log.d("AAAAAAAAAAAAAAAARRRRRRRRRGGGGGGGG","--------->top?->"+top);
    	Log.d("AAAAAAAAAAAAAAAARRRRRRRRRGGGGGGGG","--------->right?->"+right);
    	Log.d("AAAAAAAAAAAAAAAARRRRRRRRRGGGGGGGG","--------->bottom?->"+bottom);*/
    	if(!mBlockLayouts){
    		layoutChildren();
    		enableChildrenCache();
    	}
    	//if(!scrollCacheCreated)enableChildrenCache();
    }
    private void layoutChildren(){
    	//clearScrollingCache();
        // Pull all children into the RecycleBin.
        // These views will be reused if possible
        //final int firstPosition = mFirstPosition;
        final RecycleBin recycleBin = mRecycler;

        //recycleBin.scrapActiveViews();
        //Log.d("MyApps","Tenemos que reciclar "+getChildCount()+" active views");
        //Log.d("MyApps","EL ID de la primera view a recyclar seria:"+getPositionForView(getChildAt(0)));
        /*if (mDataChanged) {
            for (int i = 0; i < getChildCount(); i++) {
                recycleBin.addScrapView(getChildAt(i));
            }
        } else {
            Log.d("MyApps","Recycling active views with mFirstPosition="+mFirstPosition);
        	recycleBin.fillActiveViews(getChildCount(), mFirstPosition);
        }*/
        for (int i = 0; i < getChildCount(); i++) {
            final ViewGroup h=(ViewGroup) getChildAt(i);
        	for(int j=0;j<h.getChildCount();j++){
        		recycleBin.addScrapView(h.getChildAt(j));
        	}
        }
        detachAllViewsFromParent();
        //TODO: ADW We should only add views from current screen except when scrolling
        if(mLayoutMode==LAYOUT_NORMAL){
        	makePage(mCurrentScreen);
        }else{
        	makePage(mCurrentScreen-1);
        	makePage(mCurrentScreen);
        	makePage(mCurrentScreen+1);
        }
        /*for(int i=0;i<count;i++){
        	makePage(i);
        }*/
        //recycleBin.scrapActiveViews();
        //requestFocus();
        //setFocusable(true);
        /*if(mAdapter!=null){
	        int realScreen=mCurrentScreen;
	        if(realScreen>0) realScreen=1;
	        getChildAt(realScreen).setFocusable(true);
	        getChildAt(realScreen).requestFocus();
        }*/
        //getChildAt(realScreen).setPressed(true);
        mDataChanged = false;
        //if(mCacheColorHint!=0){
        	//createScrollingCache();
        //}
        //invalidate();
        mBlockLayouts=true;
    }
    public void makePage(int pageNum) {
    	if(pageNum<0 || pageNum>mTotalScreens-1){
    		return;
    	}    	
    	//TODO:ADW Make it use paddings and fill the current screen space
    	final int pageSpacing = pageNum*mPageWidth;
        final int startPos=pageNum*mNumColumns*mNumRows;
        
        final int marginTop=getPaddingTop()+paginatorSpace;
        final int marginBottom=getPaddingBottom();
        final int marginLeft=getPaddingLeft();
        final int marginRight=getPaddingRight();
        final int actualWidth=getMeasuredWidth()-marginLeft-marginRight;
        final int actualHeight=getMeasuredHeight()-marginTop-marginBottom;
        final int columnWidth=actualWidth/mNumColumns;
        final int rowHeight=actualHeight/mNumRows;
        //Log.d("MyApps","MakePage with width="+getMeasuredWidth());
    	
        AllAppsSlidingView.LayoutParams p;
        p = new AllAppsSlidingView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
        		ViewGroup.LayoutParams.FILL_PARENT);
        int pos=startPos;
        int x=marginLeft;
        int y=marginTop;
        HolderLayout holder=new HolderLayout(getContext());
        for(int i=0;i<mNumRows;i++){
        	for(int j=0;j<mNumColumns;j++){
        		if(pos<mAdapter.getCount()){
		            //View child = obtainView(pos);
		            View child;
		            boolean recycled=false;
		            if (!mDataChanged) {
		                // Try to use an exsiting view for this position
		                child = mRecycler.getActiveView(pos);
		                if (child != null) {
		                    // Found it -- we're using an existing child
		                    // This just needs to be positioned
		                    recycled=true;
		                }else{
				            // Make a new view for this position, or convert an unused view if
				            // possible
		                	child = obtainView(pos);
		                }
		            }else{
		            	child = obtainView(pos);
		            }
		            child.setLayoutParams(p);
		            boolean isSelected = pos==mSelectedPosition;
		            final boolean updateChildSelected = isSelected != child.isSelected();
		            child.setSelected(false);
		            child.setPressed(false);
		            /*if (updateChildSelected) {
		                child.setSelected(isSelected);
		                if (isSelected) {
				            //Log.d("MyApps", "POSICIONANDO ITEM:"+pos+" SELECTED");
		                    requestFocus();
		                }
		            }*/
		            int childHeightSpec = getChildMeasureSpec(
		                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0, p.height);
		            int childWidthSpec = getChildMeasureSpec(
		                    MeasureSpec.makeMeasureSpec(columnWidth, MeasureSpec.EXACTLY), 0, p.width);
		            child.measure(childWidthSpec, childHeightSpec);
        			int left=x;//+pageSpacing;//+ ((columnWidth - child.getMeasuredWidth()) / 2);
        			int top=y;//+((rowHeight - child.getMeasuredHeight()) / 2);
        			int w=columnWidth;
        			int h=rowHeight;
        			/*if(columnWidth>child.getWidth()){
        				w=child.getWidth();
        			}
        			if(rowHeight>child.getMeasuredHeight()){
        				w=child.getMeasuredHeight();
        			}*/
		            
        			child.layout(left, top, left+w, top+h);
		            //addViewInLayout(child, pos, p, true);
		            if (recycled) {
		            	holder.attachViewToParent(child, holder.getChildCount(), p);
		            } else {
		            	//Log.d("MyApps","Adding view in layout on position="+(pos-mFirstPosition));
		                holder.addViewInLayout(child, holder.getChildCount(), p, true);
		            }
		            pos++;
		        	x+=columnWidth;
        		}
        	}
        	x=marginLeft;
    		y+=rowHeight;
        }
        AllAppsSlidingView.LayoutParams holderParams=new AllAppsSlidingView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,ViewGroup.LayoutParams.FILL_PARENT);
        holder.layout(pageSpacing, 0, pageSpacing+mPageWidth, getMeasuredHeight());
	    //if (mCachingStarted) {
        	holder.setDrawingCacheBackgroundColor(mCacheColorHint);
	    	holder.setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);
	        holder.setDrawingCacheEnabled(true);
	    	//holder.setFocusable(true);
	    	//holder.setFocusableInTouchMode(true);
	    //}
        addViewInLayout(holder, getChildCount(), holderParams, true);

        //this.invalidate();
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	//Log.d("MyApps","Intercepted!");
    	/*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
        	//Log.d("MyApps","Intercepted!->Returned");
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX is set to the y value
                 * of the down event.
                 */
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);

                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;
                
                if (xMoved || yMoved) {
                    
                    if (xMoved) {
                        // Scroll if the user moved far enough along the X axis
                        mTouchState = TOUCH_STATE_SCROLLING;
                        //createScrollingCache();
                        //enableChildrenCache();
                    }
                    // Either way, cancel any pending longpress
                    //if (mAllowLongPress) {
                        //mAllowLongPress = false;
                        // Try canceling the long press. It could also have been scheduled
                        // by a distant descendant, so use the mAllowLongPress flag to block
                        // everything
                        //final View currentScreen = getChildAt(mCurrentScreen);
                        //currentScreen.cancelLongPress();
                    //}
                }
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mLastMotionX = x;
                mLastMotionY = y;
                mAllowLongPress = true;

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't.  mScroller.isFinished should be false when
                 * being flinged.
                 */
                //mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
            	mTouchState=mScroller.isFinished() ? TOUCH_STATE_DOWN:TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // Release the drag
                //clearScrollingCache();
            	//Log.d("MyApps","Intercepted!->UP->");
                mTouchState = TOUCH_STATE_REST;
                mAllowLongPress = false;
                break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mTouchState != TOUCH_STATE_REST;
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();
        final View child;
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            /*
             * If being flinged and user touches, stop the fling. isFinished
             * will be false if being flinged.
             */
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            mTouchState = TOUCH_STATE_DOWN;
            child = pointToView((int) x, (int) y);            
            if (child!=null) {
	            // FIXME Debounce
	            if (mPendingCheckForTap == null) {
	                mPendingCheckForTap = new CheckForTap();
	            }
	            postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
	            // Remember where the motion event started
	            mCheckTapPosition = getPositionForView(child);
            }
            // Remember where the motion event started
            mLastMotionX = x;
            break;
        case MotionEvent.ACTION_MOVE:
        	if (mTouchState == TOUCH_STATE_SCROLLING || mTouchState == TOUCH_STATE_DOWN) {
            	// Scroll to follow the motion event
                final int deltaX = (int) (mLastMotionX - x);
                if(Math.abs(deltaX)>mTouchSlop || mTouchState == TOUCH_STATE_SCROLLING){
                	mTouchState = TOUCH_STATE_SCROLLING;                	
	                mLastMotionX = x;
	                if(mLayoutMode==LAYOUT_NORMAL){
	                	mLayoutMode=LAYOUT_SCROLLING;
	                	mBlockLayouts=false;
	                	requestLayout();
	                }
	                //if(!scrollCacheCreated)enableChildrenCache();
                	//createScrollingCache();
                	
	                if (deltaX < 0) {
	                    if (getScrollX() > 0) {
	                        scrollBy(Math.max(-getScrollX(), deltaX), 0);
	                    }
	                } else if (deltaX > 0) {
	                	final int availableToScroll = ((mTotalScreens)*mPageWidth)-getScrollX()-mPageWidth;
	                	if (availableToScroll > 0) {
	                        scrollBy(Math.min(availableToScroll, deltaX), 0);
	                    }
	                }
                }
                final int deltaY = (int) (mLastMotionY - y);
                if(Math.abs(deltaY)>mTouchSlop || mTouchState == TOUCH_STATE_SCROLLING){
                	mTouchState = TOUCH_STATE_SCROLLING;
                }
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                int velocityX = (int) velocityTracker.getXVelocity();

                if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
                    // Fling hard enough to move left
                	//Log.d("SCROLLLLLLLLLL","staptoScreen:"+(mCurrentScreen - 1));
                    snapToScreen(mCurrentScreen - 1);
                } else if (velocityX < -SNAP_VELOCITY && mCurrentScreen < (mTotalScreens - 1)) {
                    // Fling hard enough to move right
                	//Log.d("SCROLLLLLLLLLL","staptoScreen:"+(mCurrentScreen + 1));
                    snapToScreen(mCurrentScreen + 1);
                } else {
                	//Log.d("SCROLLLLLLLLLL","staptoDestination");
                    snapToDestination();
                }

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
            }else{
            	//child = pointToView((int) x, (int) y);
            	child=getViewAtPosition(mCheckTapPosition);
            	if(child!=null && child.equals(pointToView((int)x, (int)y))){
            		TextView t=(TextView)child.findViewById(R.id.name);
            		//Log.d("AAAAAAAAAAAAAAAAAAAAA","Pinchassso en:"+t.getText());
	            	if (mPerformClick == null) {
	                    mPerformClick = new PerformClick();
	                }
	
	                final AllAppsSlidingView.PerformClick performClick = mPerformClick;
	                performClick.mChild = child;
	                performClick.mClickMotionPosition = mCheckTapPosition;
	                performClick.rememberWindowAttachCount();
	                if (mTouchState == TOUCH_STATE_DOWN || mTouchState == TOUCH_STATE_TAP) {
		                final Handler handler = getHandler();
		                if (handler != null) {
		                    handler.removeCallbacks(mTouchState == TOUCH_STATE_DOWN ?
		                            mPendingCheckForTap : mPendingCheckForLongPress);
		                }
		                mLayoutMode = LAYOUT_NORMAL;
		                mTouchState = TOUCH_STATE_TAP;
		                if (!mDataChanged) {
		                    //setSelectedPositionInt(mCheckTapPosition);
		                	//setSelection(mCheckTapPosition);
		                    //layoutChildren();
		                    //child.setPressed(true);
		                    //positionSelector(child);
		                    //setPressed(true);
		                    if (mSelector != null) {
		                        Drawable d = mSelector.getCurrent();
		                        if (d != null && d instanceof TransitionDrawable) {
		                            ((TransitionDrawable)d).resetTransition();
		                        }
		                    }
		                    postDelayed(new Runnable() {
		                        public void run() {
		                            child.setPressed(false);
		                            //setPressed(false);
		                            if (!mDataChanged) {
		                                post(performClick);
		                            }
		                            mTouchState = TOUCH_STATE_REST;
		                        }
		                    }, ViewConfiguration.getPressedStateDuration());
		                }
		                return true;
	                }else{
	                	
	                }
                }
            }
            mTouchState = TOUCH_STATE_REST;
            mCheckTapPosition=INVALID_POSITION;
            //setPressed(false);
            hideSelector();
            invalidate();

            final Handler handler = getHandler();
            if (handler != null) {
                handler.removeCallbacks(mPendingCheckForLongPress);
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            mTouchState = TOUCH_STATE_REST;
        }

        return true;
    }
    public void onTouchModeChanged(boolean isInTouchMode) {
        if (isInTouchMode) {
            // Get rid of the selection when we enter touch mode
            hideSelector();
            // Layout, but only if we already have done so previously.
            // (Otherwise may clobber a LAYOUT_SYNC layout that was requested to restore
            // state.)
            /*if (getHeight() > 0 && getChildCount() > 0) {
                // We do not lose focus initiating a touch (since AbsListView is focusable in
                // touch mode). Force an initial layout to get rid of the selection.
                mLayoutMode = LAYOUT_NORMAL;
                layoutChildren();
            }*/
        }
    }
    public View getViewAtPosition(int pos){
    	View v = null;
    	int position=pos;
        int realScreen=0;
    	if(mCurrentScreen>0){
    		//realScreen=1;
    		int leftScreens=mCurrentScreen;
    		position-=leftScreens*(mNumColumns*mNumRows);
    	}
    	final ViewGroup h=(ViewGroup)getChildAt(realScreen);
    	v=h.getChildAt(position);
    	return v;
    }
    @Override
    public int getPositionForView(View view) {
        View listItem = view;
        int realScreen=0;
    	int pos=0;
        if(mCurrentScreen>0){
    		//realScreen=1;
    		int leftScreens=mCurrentScreen;
    		pos+=leftScreens*(mNumColumns*mNumRows);
    	}
    	final ViewGroup h=(ViewGroup)getChildAt(realScreen);
    	for(int i=0;i<h.getChildCount();i++){
            if (h.getChildAt(i).equals(listItem)) {
                return (i+pos);
            }
    	}
        // Child not found!
        return INVALID_POSITION;
    }    
    public View pointToView(int x, int y) {
    	if(getChildCount()>0){
	    	Rect frame = new Rect();
	    	int realScreen=0;
	    	if(mCurrentScreen>0){
	    		//realScreen=1;
	    	}
	    	final ViewGroup h=(ViewGroup)getChildAt(realScreen);
	    	for(int i=0;i<h.getChildCount();i++){
	        	final View child = h.getChildAt(i);
	            if (child.getVisibility() == View.VISIBLE) {
	                child.getHitRect(frame);
	                if (frame.contains(x, y)) {
	                    return child;
	                }
	            }
	        }
    	}
        return null;
    }
    private void snapToDestination() {
        final int screenWidth = mPageWidth;
        final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
        snapToScreen(whichScreen);
    }

    void snapToScreen(int whichScreen) {
        if (!mScroller.isFinished()) return;

        //enableChildrenCache();

        whichScreen = Math.max(0, Math.min(whichScreen, mTotalScreens - 1));
        boolean changingScreens = whichScreen != mCurrentScreen;
        
        mNextScreen = whichScreen;
        
        View focusedChild = getFocusedChild();
        if (focusedChild != null && changingScreens && focusedChild == getChildAt((mCurrentScreen==0)?0:1)) {
            focusedChild.clearFocus();
        }
        
        final int newX = whichScreen * mPageWidth;
        final int delta = newX - getScrollX();
        mScroller.startScroll(getScrollX(), 0, delta, 0, Math.abs(delta) * 2);
        //invalidate();
    }
	@Override
	public ApplicationsAdapter getAdapter() {
		// TODO Auto-generated method stub
		return mAdapter;
	}
	@Override
	public void setAdapter(ApplicationsAdapter adapter) {
		// TODO Auto-generated method stub
        if (null != mAdapter) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }

        //resetList();
        mRecycler.clear();        
        mAdapter = adapter;

        //mOldSelectedPosition = INVALID_POSITION;
        //mOldSelectedRowId = INVALID_ROW_ID;
        
        if (mAdapter != null) {
            mOldItemCount = mItemCount;
            mItemCount = mAdapter.getCount();
    		mTotalScreens=getPageCount();
            mDataChanged = true;
            //checkFocus();

            mDataSetObserver = new AdapterDataSetObserver();
            mAdapter.registerDataSetObserver(mDataSetObserver);

            mRecycler.setViewTypeCount(mAdapter.getViewTypeCount());

/*            int position;
            if (mStackFromBottom) {
                position = lookForSelectablePosition(mItemCount - 1, false);
            } else {
                position = lookForSelectablePosition(0, true);
            }
            setSelectedPositionInt(position);
            setNextSelectedPositionInt(position);
            checkSelectionChanged();
*/        } else {
            //checkFocus();            
            // Nothing selected
            //checkSelectionChanged();
        }
        //mBlockLayouts=false;
        mBlockLayouts=false;
        requestLayout();
	}
    void hideSelector() {
        if (mSelectedPosition != INVALID_POSITION) {
            /*mResurrectToPosition = mSelectedPosition;
            if (mNextSelectedPosition >= 0 && mNextSelectedPosition != mSelectedPosition) {
                mResurrectToPosition = mNextSelectedPosition;
            }*/
            //setSelectedPositionInt(INVALID_POSITION);
        	setSelection(INVALID_POSITION);
            /*setNextSelectedPositionInt(INVALID_POSITION);
            mSelectedTop = 0;*/
            mSelectorRect.setEmpty();
        }
    }	
	@Override
	public View getSelectedView() {
    	final ViewGroup h=(ViewGroup)getChildAt(mCurrentScreen);
        if (mItemCount > 0 && mSelectedPosition >= 0) {
            return h.getChildAt(mSelectedPosition);
        } else {
            return null;
        }

	}
	@Override
	public void setSelection(int position) {
		// TODO Auto-generated method stub
		mSelectedPosition=position;
	}    
    View obtainView(int position) {
        View scrapView;

        scrapView = mRecycler.getScrapView(position);

        View child;
        if (scrapView != null) {
            child = mAdapter.getView(position, scrapView, this);

            if (child != scrapView) {
                mRecycler.addScrapView(scrapView);
            }
        } else {
            child = mAdapter.getView(position, null, this);
        }
        return child;
    }
    public int getPageCount(){
    	//int pages=(int) Math.floor(mAdapter.getCount()/(mNumColumns*mNumRows))+1;
    	int pages=(int) mAdapter.getCount()/(mNumColumns*mNumRows);
    	if(mAdapter.getCount()%(mNumColumns*mNumRows)>0){
    		pages++;
    	}
    	return pages;
    }
    //TODO:ADW Focus things :)
    /**
     * @return True if the current touch mode requires that we draw the selector in the pressed
     *         state.
     */
    boolean touchModeDrawsInPressedState() {
        // FIXME use isPressed for this
        switch (mTouchState) {
        case TOUCH_STATE_TAP:
        case TOUCH_STATE_DONE_WAITING:
            return true;
        default:
            return false;
        }
    }
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        //Log.d("MyApps","DrawableStateChanged!!!");
        if (mSelector != null) {
            mSelector.setState(getDrawableState());
        }
    }
    
    void positionSelector(View sel) {
        final Rect selectorRect = mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
        //Log.d("MyApps","Trying to draw selector over "+sel);
        //Log.d("MyApps","on x="+sel.getLeft()+" and y="+sel.getTop());
        positionSelector(selectorRect.left, selectorRect.top, selectorRect.right,
                selectorRect.bottom);

        //final boolean isChildViewEnabled = mIsChildViewEnabled;
        //if (sel.isEnabled() != isChildViewEnabled) {
            //mIsChildViewEnabled = !isChildViewEnabled;
            refreshDrawableState();
        //}
    }

    private void positionSelector(int l, int t, int r, int b) {
        mSelectorRect.set(l - mSelectionLeftPadding+getScrollX(), t - mSelectionTopPadding+getScrollY(), r
                + mSelectionRightPadding+getScrollX(), b + mSelectionBottomPadding+getScrollY());
    }
    /**
     * Indicates whether this view is in a state where the selector should be drawn. This will
     * happen if we have focus but are not in touch mode, or we are in the middle of displaying
     * the pressed state for an item.
     *
     * @return True if the selector should be shown
     */
    boolean shouldShowSelector() {
		//final HolderLayout h=(HolderLayout)getChildAt(mCurrentScreen==0?0:1);
    	return (hasFocus() && !isInTouchMode()) || touchModeDrawsInPressedState();
    }

    private void drawSelector(Canvas canvas) {
        if (shouldShowSelector() && mSelectorRect != null && !mSelectorRect.isEmpty()) {
            final Drawable selector = mSelector;
            //Log.d("MyApps","FCKNG SELECTOR BOUNDS="+mSelector.toString());
            selector.setBounds(mSelectorRect);
            selector.draw(canvas);
        }
    }

    /**
     * Controls whether the selection highlight drawable should be drawn on top of the item or
     * behind it.
     *
     * @param onTop If true, the selector will be drawn on the item it is highlighting. The default
     *        is false.
     *
     * @attr ref android.R.styleable#AbsListView_drawSelectorOnTop
     */
    public void setDrawSelectorOnTop(boolean onTop) {
        mDrawSelectorOnTop = onTop;
    }

    /**
     * Set a Drawable that should be used to highlight the currently selected item.
     *
     * @param resID A Drawable resource to use as the selection highlight.
     *
     * @attr ref android.R.styleable#AbsListView_listSelector
     */
    public void setSelector(int resID) {
        setSelector(getResources().getDrawable(resID));
    }

    public void setSelector(Drawable sel) {
        if (mSelector != null) {
            mSelector.setCallback(null);
            unscheduleDrawable(mSelector);
        }
        mSelector = sel;
        Rect padding = new Rect();
        sel.getPadding(padding);
        mSelectionLeftPadding = padding.left;
        mSelectionTopPadding = padding.top;
        mSelectionRightPadding = padding.right;
        mSelectionBottomPadding = padding.bottom;
        sel.setCallback(this);
        sel.setState(getDrawableState());
    }
    /**
     * Utility to keep mSelectedPosition and mSelectedRowId in sync
     * @param position Our current position
     */
    
    /*void setSelectedPositionInt(int position) {
        mSelectedPosition = position;
        //mSelectedRowId = getItemIdAtPosition(position);
    }*/
    /**
     * Returns the selector {@link android.graphics.drawable.Drawable} that is used to draw the
     * selection in the list.
     *
     * @return the drawable used to display the selector
     */
    public Drawable getSelector() {
        return mSelector;
    }
    @Override
    public int getSolidColor() {
        return mCacheColorHint;
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this list is always drawn
     * on top of a solid, single-color, opaque background
     *
     * @param color The background color
     */
    public void setCacheColorHint(int color) {
        mCacheColorHint = color;
    }

    /**
     * When set to a non-zero value, the cache color hint indicates that this list is always drawn
     * on top of a solid, single-color, opaque background
     *
     * @return The cache color hint
     */
    public int getCacheColorHint() {
        return mCacheColorHint;
    }    
    //TODO: ADW Recycle Bin
    private void RecycleOuterViews(int screen){
        //Log.d("MyApps","Lets try to recycle some views out of screen "+screen);
    	//final int currentScreen=screen;
    	final int startPos=(screen*mNumColumns*mNumRows);//-mFirstPosition;
    	final int endPos=startPos+(mNumColumns*mNumRows)-1;
    	final int childCount=getChildCount();
    	int recycledCount=0;
    	Log.d("MyApps","We are on screen "+screen);
    	for(int i=childCount-1;i>=0;i--){
    		if(i<startPos || i>endPos){
    			View child=getChildAt(i);
    			mRecycler.addScrapView(child);
    			detachViewFromParent(child);
    			recycledCount++;
    		}
    	}
        mLayoutMode=LAYOUT_NORMAL;
        //Log.d("MyApps","Recycled "+recycledCount+" views! of "+childCount);
    	/*for(int x=0;x<mTotalScreens;x++){
	        if(x!=currentScreen){
	    		final int startPos=x*mNumColumns*mNumRows;
		        int pos=startPos;
		        
		        for(int i=0;i<mNumRows;i++){
		        	for(int j=0;j<mNumColumns;j++){
		        		if(pos<mAdapter.getCount()){
				        	//int left=pageSpacing+(j*mColumnWidth);
				        	//int top=i*mRowHeight;
		        	        //Log.d("MyApps","Lets try to recycle child #"+pos);
				            View child = getChildAt(pos);
				            mRecycler.addScrapView(child);
				            pos++;
		        		}
		        	}
		        }
	        }
    	}*/    	
    }
    /**
     * Sets the recycler listener to be notified whenever a View is set aside in
     * the recycler for later reuse. This listener can be used to free resources
     * associated to the View.
     *
     * @param listener The recycler listener to be notified of views set aside
     *        in the recycler.
     *
     * @see android.widget.AbsListView.RecycleBin
     * @see android.widget.AbsListView.RecyclerListener
     */
    public void setRecyclerListener(RecyclerListener listener) {
        mRecycler.mRecyclerListener = listener;
    }
    /**
     * A RecyclerListener is used to receive a notification whenever a View is placed
     * inside the RecycleBin's scrap heap. This listener is used to free resources
     * associated to Views placed in the RecycleBin.
     *
     * @see android.widget.AbsListView.RecycleBin
     * @see android.widget.AbsListView#setRecyclerListener(android.widget.AbsListView.RecyclerListener)
     */
    public static interface RecyclerListener {
        /**
         * Indicates that the specified View was moved into the recycler's scrap heap.
         * The view is not displayed on screen any more and any expensive resource
         * associated with the view should be discarded.
         *
         * @param view
         */
        void onMovedToScrapHeap(View view);
    }
    
    /**
     * The RecycleBin facilitates reuse of views across layouts. The RecycleBin has two levels of
     * storage: ActiveViews and ScrapViews. ActiveViews are those views which were onscreen at the
     * start of a layout. By construction, they are displaying current information. At the end of
     * layout, all views in ActiveViews are demoted to ScrapViews. ScrapViews are old views that
     * could potentially be used by the adapter to avoid allocating views unnecessarily.
     *
     * @see android.widget.AbsListView#setRecyclerListener(android.widget.AbsListView.RecyclerListener)
     * @see android.widget.AbsListView.RecyclerListener
     */
    class RecycleBin {
        private RecyclerListener mRecyclerListener;

        /**
         * The position of the first view stored in mActiveViews.
         */
        private int mFirstActivePosition;

        /**
         * Views that were on screen at the start of layout. This array is populated at the start of
         * layout, and at the end of layout all view in mActiveViews are moved to mScrapViews.
         * Views in mActiveViews represent a contiguous range of Views, with position of the first
         * view store in mFirstActivePosition.
         */
        private View[] mActiveViews = new View[0];

        /**
         * Unsorted views that can be used by the adapter as a convert view.
         */
        private ArrayList<View>[] mScrapViews;

        private int mViewTypeCount;

        private ArrayList<View> mCurrentScrap;

        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
            }
            //noinspection unchecked
            ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mCurrentScrap = scrapViews[0];
            mScrapViews = scrapViews;
        }

        public boolean shouldRecycleViewType(int viewType) {
            return viewType >= 0;
        }

        /**
         * Clears the scrap heap.
         */
        void clear() {
            if (mViewTypeCount == 1) {
                final ArrayList<View> scrap = mCurrentScrap;
                final int scrapCount = scrap.size();
                for (int i = 0; i < scrapCount; i++) {
                    removeDetachedView(scrap.remove(scrapCount - 1 - i), false);
                }
            } else {
                final int typeCount = mViewTypeCount;
                for (int i = 0; i < typeCount; i++) {
                    final ArrayList<View> scrap = mScrapViews[i];
                    final int scrapCount = scrap.size();
                    for (int j = 0; j < scrapCount; j++) {
                        removeDetachedView(scrap.remove(scrapCount - 1 - j), false);
                    }
                }
            }
        }

        /**
         * Fill ActiveViews with all of the children of the AbsListView.
         *
         * @param childCount The minimum number of views mActiveViews should hold
         * @param firstActivePosition The position of the first view that will be stored in
         *        mActiveViews
         */
        void fillActiveViews(int childCount, int firstActivePosition) {
            if (mActiveViews.length < childCount) {
                mActiveViews = new View[childCount];
            }
            mFirstActivePosition = firstActivePosition;

            final View[] activeViews = mActiveViews;
            for (int i = 0; i < childCount; i++) {
            	View child = getChildAt(i);
                AllAppsSlidingView.LayoutParams lp = (AllAppsSlidingView.LayoutParams)child.getLayoutParams();
                // Don't put header or footer views into the scrap heap
                if (lp != null && lp.viewType != AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER) {
                    // Note:  We do place AdapterView.ITEM_VIEW_TYPE_IGNORE in active views.
                    //        However, we will NOT place them into scrap views.
                    activeViews[i] = child;
                }
            }
            for(int i=0;i<activeViews.length;i++){
            	//Log.d("MyRecycler","We have recycled activeview "+i);
            	//Log.d("MyRecycler","So whe we call it will be "+(i-mFirstActivePosition));
            }
        }

        /**
         * Get the view corresponding to the specified position. The view will be removed from
         * mActiveViews if it is found.
         *
         * @param position The position to look up in mActiveViews
         * @return The view if it is found, null otherwise
         */
        View getActiveView(int position) {
            int index = position - mFirstActivePosition;
            final View[] activeViews = mActiveViews;
            //Log.d("MyRecycler","We're recovering view "+index+" of a list of "+activeViews.length);
            if (index >=0 && index < activeViews.length) {
                final View match = activeViews[index];
                activeViews[index] = null;
                return match;
            }
            return null;
        }

        /**
         * @return A view from the ScrapViews collection. These are unordered.
         */
        View getScrapView(int position) {
            ArrayList<View> scrapViews;
            if (mViewTypeCount == 1) {
                scrapViews = mCurrentScrap;
                int size = scrapViews.size();
                if (size > 0) {
                    return scrapViews.remove(size - 1);
                } else {
                    return null;
                }
            } else {
                int whichScrap = mAdapter.getItemViewType(position);
                if (whichScrap >= 0 && whichScrap < mScrapViews.length) {
                    scrapViews = mScrapViews[whichScrap];
                    int size = scrapViews.size();
                    if (size > 0) {
                        return scrapViews.remove(size - 1);
                    }
                }
            }
            return null;
        }

        /**
         * Put a view into the ScapViews list. These views are unordered.
         *
         * @param scrap The view to add
         */
        void addScrapView(View scrap) {
            AllAppsSlidingView.LayoutParams lp = (AllAppsSlidingView.LayoutParams) scrap.getLayoutParams();
            if (lp == null) {
                return;
            }

            // Don't put header or footer views or views that should be ignored
            // into the scrap heap
            int viewType = lp.viewType;
            if (!shouldRecycleViewType(viewType)) {
                return;
            }

            if (mViewTypeCount == 1) {
                mCurrentScrap.add(scrap);
            } else {
                mScrapViews[viewType].add(scrap);
            }

            if (mRecyclerListener != null) {
                mRecyclerListener.onMovedToScrapHeap(scrap);
            }
        }

        /**
         * Move all views remaining in mActiveViews to mScrapViews.
         */
        void scrapActiveViews() {
            final View[] activeViews = mActiveViews;
            final boolean hasListener = mRecyclerListener != null;
            final boolean multipleScraps = mViewTypeCount > 1;

            ArrayList<View> scrapViews = mCurrentScrap;
            final int count = activeViews.length;
            for (int i = 0; i < count; ++i) {
                final View victim = activeViews[i];
                if (victim != null) {
                    int whichScrap = ((AllAppsSlidingView.LayoutParams)
                            victim.getLayoutParams()).viewType;

                    activeViews[i] = null;

                    if (whichScrap == AdapterView.ITEM_VIEW_TYPE_IGNORE) {
                        // Do not move views that should be ignored
                        continue;
                    }

                    if (multipleScraps) {
                        scrapViews = mScrapViews[whichScrap];
                    }
                    scrapViews.add(victim);

                    if (hasListener) {
                        mRecyclerListener.onMovedToScrapHeap(victim);
                    }

                }
            }

            pruneScrapViews();
        }

        /**
         * Makes sure that the size of mScrapViews does not exceed the size of mActiveViews.
         * (This can happen if an adapter does not recycle its views).
         */
        private void pruneScrapViews() {
            final int maxViews = mActiveViews.length;
            final int viewTypeCount = mViewTypeCount;
            final ArrayList<View>[] scrapViews = mScrapViews;
            for (int i = 0; i < viewTypeCount; ++i) {
                final ArrayList<View> scrapPile = scrapViews[i];
                int size = scrapPile.size();
                final int extras = size - maxViews;
                size--;
                for (int j = 0; j < extras; j++) {
                    removeDetachedView(scrapPile.remove(size--), false);
                }
            }
        }

        /**
         * Puts all views in the scrap heap into the supplied list.
         */
        void reclaimScrapViews(List<View> views) {
            if (mViewTypeCount == 1) {
                views.addAll(mCurrentScrap);
            } else {
                final int viewTypeCount = mViewTypeCount;
                final ArrayList<View>[] scrapViews = mScrapViews;
                for (int i = 0; i < viewTypeCount; ++i) {
                    final ArrayList<View> scrapPile = scrapViews[i];
                    views.addAll(scrapPile);
                }
            }
        }
    }    
    
    //TODO:ADW Helper classes
    final class CheckForTap implements Runnable {
        public void run() {
            if (mTouchState == TOUCH_STATE_DOWN) {
                mTouchState = TOUCH_STATE_TAP;
                //final View child = getChildAt(mCheckTapPosition);
                final View child=getViewAtPosition(mCheckTapPosition);
                if (child != null && !child.hasFocusable()) {
                    mLayoutMode = LAYOUT_NORMAL;

                    if (!mDataChanged) {
                        //layoutChildren();
                        child.setPressed(true);
                        //setSelectedPositionInt(mCheckTapPosition);
                        setSelection(mCheckTapPosition);
                        positionSelector(child);
                        //setPressed(true);
                        //invalidate();
                        final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                        final boolean longClickable = isLongClickable();

                        if (mSelector != null) {
                            Drawable d = mSelector.getCurrent();
                            if (d != null && d instanceof TransitionDrawable) {
                                if (longClickable) {
                                    ((TransitionDrawable) d).startTransition(longPressTimeout);
                                } else {
                                    ((TransitionDrawable) d).resetTransition();
                                }
                            }
                        }

                        if (longClickable) {
                            if (mPendingCheckForLongPress == null) {
                                mPendingCheckForLongPress = new CheckForLongPress();
                            }
                            mPendingCheckForLongPress.rememberWindowAttachCount();
                            postDelayed(mPendingCheckForLongPress, longPressTimeout);
                        } else {
                            mTouchState = TOUCH_STATE_DONE_WAITING;
                        }
                    } else {
                        mTouchState = TOUCH_STATE_DONE_WAITING;
                    }
                }
            }
        }
    }
    /**
     * A base class for Runnables that will check that their view is still attached to
     * the original window as when the Runnable was created.
     *
     */
    private class WindowRunnnable {
        private int mOriginalAttachCount;

        public void rememberWindowAttachCount() {
            mOriginalAttachCount = getWindowAttachCount();
        }

        public boolean sameWindow() {
            return hasWindowFocus() && getWindowAttachCount() == mOriginalAttachCount;
        }
    }

    private class PerformClick extends WindowRunnnable implements Runnable {
        View mChild;
        int mClickMotionPosition;

        public void run() {
            // The data has changed since we posted this action in the event queue,
            // bail out before bad things happen
            if (mDataChanged) return;
            final int realPosition=mClickMotionPosition;
            if (realPosition==INVALID_POSITION) return;
            if (mAdapter != null &&  realPosition < mAdapter.getCount() && sameWindow()) {
                performItemClick(mChild, realPosition, mAdapter.getItemId(realPosition));
                setSelection(INVALID_POSITION);                
            }
        }
    }

    private class CheckForLongPress extends WindowRunnnable implements Runnable {
        public void run() {
            final int motionPosition = mCheckTapPosition;
            final View child = getViewAtPosition(motionPosition);
            //final int realPosition=translatePosition(motionPosition);
            if (child != null) {
            	//Log.d("MyApps","Trying to longpress "+child);
                final int longPressPosition = motionPosition;
                final long longPressId = mAdapter.getItemId(motionPosition);

                boolean handled = false;
                if (sameWindow() && !mDataChanged) {
                    handled = performLongPress(child, longPressPosition, longPressId);
                }
                if (handled) {
                    mTouchState = TOUCH_STATE_REST;
                    //setPressed(false);
                    child.setPressed(false);
                } else {
                    mTouchState = TOUCH_STATE_DONE_WAITING;
                }

            }
        }
    }

    private class CheckForKeyLongPress extends WindowRunnnable implements Runnable {
        public void run() {
            if (isPressed() && mCheckTapPosition >= 0) {
                int index = mCheckTapPosition;
                View v = getChildAt(index);

                if (!mDataChanged) {
                    boolean handled = false;
                    if (sameWindow()) {
                        handled = performLongPress(v, mCheckTapPosition, mCheckTapPosition);
                    }
                    if (handled) {
                        //setPressed(false);
                        v.setPressed(false);
                    }
                } else {
                    v.setPressed(false);
                    if (v != null) v.setPressed(false);
                }
            }
        }
    }

    private boolean performLongPress(final View child,
            final int longPressPosition, final long longPressId) {
        boolean handled = false;
		
        if (getOnItemLongClickListener() != null) {
            handled = getOnItemLongClickListener().onItemLongClick(AllAppsSlidingView.this, child,
                    longPressPosition, longPressId);
        }
        /*if (!handled) {
            mContextMenuInfo = createContextMenuInfo(child, longPressPosition, longPressId);
            handled = super.showContextMenuForChild(MyApps.this);
        }*/
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }
    /*@Override
    protected void dispatchSetPressed(boolean pressed) {
        // Don't dispatch setPressed to our children. We call setPressed on ourselves to
        // get the selector in the right state, but we don't want to press each child.
        int focusableScreen=(mCurrentScreen==0)?0:1;
        getChildAt(focusableScreen).setPressed(pressed);
    	
    }
    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        int focusableScreen=mCurrentScreen;
        if(focusableScreen>0)focusableScreen=1;
        getChildAt(focusableScreen).requestFocus(direction, previouslyFocusedRect);
        return false;
    }
    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        //final int realScreen=(mCurrentScreen==0?0:1);
    	//getChildAt(realScreen).addFocusables(views, direction);
        if (direction == View.FOCUS_LEFT) {
            if (mCurrentScreen > 0) {
                getChildAt(0).addFocusables(views, direction);
            }
        } else if (direction == View.FOCUS_RIGHT){
            if (mCurrentScreen < getChildCount() - 1) {
                getChildAt(1).addFocusables(views, direction);
            }
        }
    }*/
    
    /**
     * AbsListView extends LayoutParams to provide a place to hold the view type.
     */
    public class LayoutParams extends AdapterView.LayoutParams {
        /**
         * View type for this view, as returned by
         * {@link android.widget.Adapter#getItemViewType(int) }
         */
        int viewType;

        /**
         * When this boolean is set, the view has been added to the AbsListView
         * at least once. It is used to know whether headers/footers have already
         * been added to the list view and whether they should be treated as
         * recycled views or not.
         */
        boolean recycledHeaderFooter;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int w, int h) {
            super(w, h);
        }

        public LayoutParams(int w, int h, int viewType) {
            super(w, h);
            this.viewType = viewType;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new AllAppsSlidingView.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof AllAppsSlidingView.LayoutParams;
    }    
    //TODO:ADW DATA HANDLING
    class AdapterDataSetObserver extends DataSetObserver {

        private Parcelable mInstanceState = null;

        @Override
        public void onChanged() {
            mDataChanged = true;
            mOldItemCount = mItemCount;
            mItemCount = getAdapter().getCount();
            mTotalScreens=getPageCount();

            // Detect the case where a cursor that was previously invalidated has
            // been repopulated with new data.
            if (AllAppsSlidingView.this.getAdapter().hasStableIds() && mInstanceState != null
                    && mOldItemCount == 0 && mItemCount > 0) {
            	AllAppsSlidingView.this.onRestoreInstanceState(mInstanceState);
                mInstanceState = null;
            } else {
                //rememberSyncState();
            }
            //checkFocus();
            mBlockLayouts=false;
            requestLayout();
        }

        @Override
        public void onInvalidated() {
            mDataChanged = true;

            if (AllAppsSlidingView.this.getAdapter().hasStableIds()) {
                // Remember the current state for the case where our hosting activity is being
                // stopped and later restarted
                //mInstanceState = AllAppsSlidingView.this.onSaveInstanceState();
            }

            // Data is invalid so we should reset our state
            mOldItemCount = mItemCount;
            mItemCount = 0;
            mSelectedPosition = INVALID_POSITION;
            //mSelectedRowId = INVALID_ROW_ID;
            //mNextSelectedPosition = INVALID_POSITION;
            //mNextSelectedRowId = INVALID_ROW_ID;
            //mNeedSync = false;
            //checkSelectionChanged();

            //checkFocus();
            //requestLayout();
        }

        public void clearSavedState() {
            mInstanceState = null;
        }
    }
    void enableChildrenCache() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final HolderLayout h = (HolderLayout) getChildAt(i);
            h.setChildrenDrawnWithCacheEnabled(true);
            h.setChildrenDrawingCacheEnabled(true);
            /*h.setDrawingCacheEnabled(true);
        	h.setAlwaysDrawnWithCacheEnabled(true);
        	h.setAnimationCacheEnabled(true);
        	h.setChildrenDrawingCacheEnabled(true);
        	h.setChildrenDrawnWithCacheEnabled(true);
        	h.setDrawingCacheEnabled(true);
    		setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);*/
        }
    	/*setAlwaysDrawnWithCacheEnabled(true);
    	setAnimationCacheEnabled(true);
    	setChildrenDrawingCacheEnabled(true);
    	setChildrenDrawnWithCacheEnabled(true);
    	setDrawingCacheEnabled(true);
    	//setScrollingCacheEnabled(true);
		setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);*/        
        scrollCacheCreated=true;
    }

    void clearChildrenCache() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final HolderLayout h = (HolderLayout) getChildAt(i);
            h.setChildrenDrawnWithCacheEnabled(false);
            h.setDrawingCacheEnabled(false);
        }
        scrollCacheCreated=false;
    }
    
    //TODO: ADW Events

	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
		// TODO Auto-generated method stub
        ApplicationInfo app = (ApplicationInfo) getItemAtPosition(position);
        mLauncher.startActivitySafely(app.intent);		
	}

	public boolean onItemLongClick(AdapterView<?> parent, View v,
			int position, long id) {
		// TODO Auto-generated method stub
        if (!v.isInTouchMode()) {
            return false;
        }

        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        app = new ApplicationInfo(app);

        mDragger.startDrag(v, this, app, DragController.DRAG_ACTION_COPY);
        mLauncher.closeAllApplications();

        return true;
	}
	public void onDropCompleted(View target, boolean success) {
		// TODO Auto-generated method stub
		
	}
	public void setDragger(DragController dragger) {
		// TODO Auto-generated method stub
		mDragger=dragger;
		
	}
    public void setForceOpaque(boolean value){
    	if(value!=forceOpaque){
	    	forceOpaque=value;
	    	if(value){
	    		setBackgroundColor(0xFF000000);
	    		setCacheColorHint(0xFF000000);
	    	}else{
	    		setBackgroundDrawable(null);
	    		setCacheColorHint(Color.TRANSPARENT);
				setDrawingCacheBackgroundColor(Color.TRANSPARENT);
	    	}
    	}
    	mBlockLayouts=false;
    	requestLayout();
		/*setBackgroundDrawable(null);
		setCacheColorHint(0xFF000000);
    	setAlwaysDrawnWithCacheEnabled(true);
    	setAnimationCacheEnabled(true);
    	setChildrenDrawingCacheEnabled(true);
    	setChildrenDrawnWithCacheEnabled(true);
    	setDrawingCacheEnabled(true);
    	//setScrollingCacheEnabled(true);
		setDrawingCacheQuality(DRAWING_CACHE_QUALITY_LOW);
		requestLayout();*/
    }
	public int getNumColumns() {
		return mNumColumns;
	}
	public void setNumColumns(int mNumColumns) {
		this.mNumColumns = mNumColumns;
		if(mAdapter!=null){
			mTotalScreens=getPageCount();
			requestLayout();
		}
	}
	public int getNumRows() {
		return mNumRows;
	}
	public void setNumRows(int mNumRows) {
		this.mNumRows = mNumRows;
		if(mAdapter!=null){
			mTotalScreens=getPageCount();
			requestLayout();
		}
	}
	@Override
	protected void onAnimationEnd() {
		// TODO Auto-generated method stub
		super.onAnimationEnd();
	}
	@Override
	protected void onAnimationStart() {
		// TODO Auto-generated method stub
		super.onAnimationStart();
	}
	@Override
	public void setVisibility(int visibility) {
		// TODO Auto-generated method stub
        if(visibility==View.VISIBLE){
			mTexture=mLauncher.getBlurredBg();
	        mTextureWidth = mTexture.getWidth();
	        mTextureHeight = mTexture.getHeight();
	
	        mPaint = new Paint();
	        mPaint.setDither(false);
        }
		super.setVisibility(visibility);
	}
}
