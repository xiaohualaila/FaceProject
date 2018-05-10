package com.shuli.root.faceproject.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

import com.shuli.root.faceproject.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;


/**
 * Created by dhht on 17/6/13.
 */

public class KeyboardEditText extends android.support.v7.widget.AppCompatEditText implements KeyboardView.OnKeyboardActionListener {
    private KeyboardView mKeyboardView;
    private Keyboard mKeyboard;

    private Window mWindow;
    private View mDecorView;
    private View mContentView;

    private PopupWindow mKeyboardWindow;

    private boolean mNeedCustomKeyboard = true; // 是否启用自定义键盘

    private int mScrollDistance = 0; // 输入框在键盘被弹出时，要被推上去的距离

    public static int mScreenW = -1;// 未知宽高
    public static int mScreenH = -1;
    public static int mScreenHNonavBar = -1; // 不包含导航栏的高度
    public static int mRealContentH = -1; // 实际内容高度， 计算公式:屏幕高度-导航栏高度-电量栏高度

    public static float mDensity = 1.0f;
    public static int mDensityDpi = 160;

    private boolean mIsEditable = true;

    public KeyboardEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttribute(context);
        initKeyboard(context, attrs);
    }

    public KeyboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttribute(context);
        initKeyboard(context, attrs);
    }

    private void initAttribute(Context context) {
        initScreenParams(context);
        this.setLongClickable(false);
        this.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        removeCopyAbility();

        if (this.getText() != null) {
            this.setSelection(this.getText().length());
        }

        this.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard();
                }
            }
        });
    }

    /**
     * 初始化键盘
     *
     * @param context
     * @param attrs
     */
    private void initKeyboard(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.keyboard);
        if (a.hasValue(R.styleable.keyboard_xml)) {
            mNeedCustomKeyboard = true;
            int xmlId = a.getResourceId(R.styleable.keyboard_xml, 0);
            mKeyboard = new Keyboard(context, xmlId);
            mKeyboardView = (KeyboardView) LayoutInflater.from(context).inflate(R.layout.keyboard_view, null);
            mKeyboardView.setKeyboard(mKeyboard);
            mKeyboardView.setEnabled(true);
            mKeyboardView.setPreviewEnabled(false);
            mKeyboardView.setOnKeyboardActionListener(this);
            mKeyboardWindow = new PopupWindow(mKeyboardView, ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            mKeyboardWindow.setAnimationStyle(R.style.AnimationFade);
            mKeyboardWindow.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (mScrollDistance > 0) {
                        int temp = mScrollDistance;
                        mScrollDistance = 0;
                        if (null != mContentView) {
                            mContentView.scrollBy(0, -temp);
                        }
                    }
                }
            });
        } else {
            mNeedCustomKeyboard = false;
        }
        a.recycle();

    }

    /**
     * 显示键盘
     */
    private void showKeyboard() {
        if (mIsEditable) {
            if (mKeyboardWindow != null) {
                if (!mKeyboardWindow.isShowing()) {
                    mKeyboardView.setKeyboard(mKeyboard);
                    if (mKeyboardWindow != null && !mKeyboardWindow.isShowing()) {
                        mKeyboardWindow.showAtLocation( this.mDecorView, Gravity.BOTTOM, 0, 0);
                    }
                    mKeyboardWindow.update();

                    if (null != mDecorView && null != mContentView) {
                        int[] pos = new int[2];
                        // 计算弹出的键盘的尺寸
                        getLocationOnScreen(pos);
                        float height = dpToPx(getContext(), 240);
                        Rect outRect = new Rect();
                        // 然后该View有个getWindowVisibleDisplayFrame()方法可以获取到程序显示的区域，
                        // 包括标题栏，但不包括状态栏。
                        mDecorView.getWindowVisibleDisplayFrame(outRect);// 获得view空间，也就是除掉标题栏
                        // outRect.top表示状态栏（通知栏)
                        int screen = mRealContentH;
                        mScrollDistance = (int) ((pos[1] + getMeasuredHeight() - outRect.top) - (screen - height));
                        if (mScrollDistance > 0) {
                            mContentView.scrollBy(0, mScrollDistance);
                        }
                    }
                }
            }
        }
    }

    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        if (null != mKeyboardWindow) {
            if(mKeyboardWindow!=null&& mKeyboardWindow.isShowing()){
                mKeyboardWindow.dismiss();
            }
        }
    }

    /**
     * 隐藏系统键盘
     */
    private void hideSysInput() {
        if (this.getWindowToken() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 键盘是否可用
     */
    public void setEditable(boolean editable) {
        this.mIsEditable = editable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        requestFocus();
        requestFocusFromTouch();

        if (mNeedCustomKeyboard) {
            hideSysInput();
            showKeyboard();
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (null != mKeyboardWindow) {
                if (mKeyboardWindow != null && mKeyboardWindow.isShowing()) {
                    mKeyboardWindow.dismiss();
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        this.mWindow = ((Activity) getContext()).getWindow();
        this.mDecorView = this.mWindow.getDecorView();
        this.mContentView = this.mWindow.findViewById(Window.ID_ANDROID_CONTENT);

        hideSysInput();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hideKeyboard();
        mDecorView = null;
        mContentView = null;
        mWindow = null;
    }

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        Editable editable = this.getText();
        int start = this.getSelectionStart();
        if (primaryCode == Keyboard.KEYCODE_DONE) {// 隐藏键盘
            hideKeyboard();
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {// 回退
            if (editable != null && editable.length() > 0) {
                if (start > 0) {
                    editable.delete(start - 1, start);
                }
            }
        } else if (0x0 <= primaryCode && primaryCode <= 0x7f) {
            // 可以直接输入的字符(如0-9,.)，他们在键盘映射xml中的keycode值必须配置为该字符的ASCII码
            editable.insert(start, Character.toString((char) primaryCode));
        } else if (primaryCode > 0x7f) {
            Key mkey = getKeyByKeyCode(primaryCode);
            // 可以直接输入的字符(如0-9,.)，他们在键盘映射xml中的keycode值必须配置为该字符的ASCII码
            editable.insert(start, mkey.label);
        }
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    private Key getKeyByKeyCode(int keyCode) {
        if (null != mKeyboard) {
            List<Key> mKeys = mKeyboard.getKeys();
            for (int i = 0, size = mKeys.size(); i < size; i++) {
                Key mKey = mKeys.get(i);

                int codes[] = mKey.codes;

                if (codes[0] == keyCode) {
                    return mKey;
                }
            }
        }

        return null;
    }

    @TargetApi(11)
    private void removeCopyAbility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setCustomSelectionActionModeCallback(new ActionMode.Callback() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {

                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }
            });
        }
    }

    /**
     * 密度转换为像素值
     *
     * @param dp
     * @return
     */
    public static int dpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void initScreenParams(Context context) {
        DisplayMetrics dMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(dMetrics);

        mScreenW = dMetrics.widthPixels;
        mScreenH = dMetrics.heightPixels;
        mDensity = dMetrics.density;
        mDensityDpi = dMetrics.densityDpi;

        mScreenHNonavBar = mScreenH;

        int ver = Build.VERSION.SDK_INT;

        // 新版本的android 系统有导航栏，造成无法正确获取高度
        if (ver == 13) {
            try {
                Method mt = display.getClass().getMethod("getRealHeight");
                mScreenHNonavBar = (Integer) mt.invoke(display);
            } catch (Exception e) {
            }
        } else if (ver > 13) {
            try {
                Method mt = display.getClass().getMethod("getRawHeight");
                mScreenHNonavBar = (Integer) mt.invoke(display);
            } catch (Exception e) {
            }
        }

        mRealContentH = mScreenHNonavBar - getStatusBarHeight(context);

    }

    /**
     * 电量栏高度
     *
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return sbar;
    }

}
