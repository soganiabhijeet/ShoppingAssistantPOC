package soganiabhijeet.com.myapplication;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import im.delight.android.webview.AdvancedWebView;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by abhijeetsogani on 7/2/16.
 */

public class ShoppingWizardService extends Service {
    private WindowManager windowManager;
    private RelativeLayout chatheadView;
    private View blinkContainer, webViewContainer, backgroundLayout;
    private ImageView chatheadImg;
    private int chatHeadXInitCoor, chatHeadYInitCoor, xInitMargin, yInitMargin;
    private Point szWindow = new Point();
    private boolean isLongclick = false;
    private WindowManager.LayoutParams paramOverlay;
    private WindowManager.LayoutParams paramChatContainer;
    private WebViewDialog dialog;

    private AdvancedWebView mWebView;
    private View shoppingContainerAvatar;
    //private boolean isDialogAdded = false;
    private boolean isWebviewOpen = false;

    private boolean onCustomize = false;
    private boolean onRemoveView = false;
    private int topOverlayHeightPx;
    private int topOverlayTouchHeightPx;
    private int chatDefaultLeftPixel;
    private int chatDefaultRightPixel;
    private int chatDefaultTopPixel;
    private int chatIdleLeftPixel;
    private int chatIdleRightPixel;

    private static String TAG = ShoppingWizardService.class.getSimpleName();
    private View webviewChathead;
    private CustomizeShoppingWizardDialog customizeShoppingWizard;
    //private ShoppingAssistantData itemModel;
    private final long ONE_DAY_IN_MILLIS = 60000 * 60 * 24;
    private CompositeSubscription rxSubscriber;
    private Subscription foregroundAppSubscription;
    private Subscription chatheadTransparentSubscription;

    private ShoppingWizardFTUE shoppingWizardFTUE;
    private String IS_FIRST_TIME = "is_shopping_assistant_never_opened";
    public static final String STOP_SHOPPING_ASSISTANT_FOR_TIME = "stop_shopping_assistant_time";
    private HashSet<String> blacklistedApps = new HashSet<>();
    private Vibrator vibrator;
    private boolean isWebpageLoaded = false;
    private boolean isCustomizeOpen = false;
    private RemoveView removeAndCustomize;
    private boolean isFTUEOpen = false;
    public static final String BLACKLISTED_APPS = "blacklisted_apps";
    public static final String DATA = "data";
    private View chatheadContainer;
    private PublishSubject<Integer> retryForegroundSubject;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.d(TAG, "ShoppingWizardService.onCreate()");

    }


    private void setDpToPixels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }
        topOverlayHeightPx = (int) getResources().getDimension(R.dimen.top_overlay_height);
        topOverlayTouchHeightPx = (int) getResources().getDimension(R.dimen.top_overlay_touch_height);
        chatDefaultLeftPixel = (-(int) getResources().getDimension(R.dimen.blink_container_height)) + (int) getResources().getDimension(R.dimen.chathead_height) / 2 + (int) getResources().getDimension(R.dimen.chathead_padding_boundary);
        chatDefaultRightPixel = szWindow.x - (int) getResources().getDimension(R.dimen.chathead_padding_boundary) - (int) getResources().getDimension(R.dimen.blink_container_height) - (int) getResources().getDimension(R.dimen.chathead_height) / 2;
        chatIdleLeftPixel = chatDefaultLeftPixel - (int) getResources().getDimension(R.dimen.idle_state_movement);
        chatIdleRightPixel = chatDefaultRightPixel + (int) getResources().getDimension(R.dimen.idle_state_movement);
        chatDefaultTopPixel = -szWindow.y / 2 + (int) getResources().getDimension(R.dimen.chathead_height) / 2 + (int) getResources().getDimension(R.dimen.chathead_margin_top) + getStatusBarHeight();
    }

    private void handleStart() {
        Log.d(TAG, "in handle start");
        setDpToPixels();
        rxSubscriber = new CompositeSubscription();
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        backgroundLayout = inflater.inflate(R.layout.shopping_wizard_black_shadow, null);
        initializeDialogView(dialog);
        paramOverlay = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                topOverlayHeightPx,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramOverlay.gravity = Gravity.TOP;
        chatheadView = (RelativeLayout) inflater.inflate(R.layout.shopping_wizard_chathead, null);
        chatheadImg = (ImageView) chatheadView.findViewById(R.id.chathead_img);
        chatheadContainer = chatheadView.findViewById(R.id.chathead_container);
        blinkContainer = chatheadView.findViewById(R.id.blink_container);
        addClickListenerOnCHatHead();
        shoppingContainerAvatar = chatheadView.findViewById(R.id.shopping_avatar_container);
        paramChatContainer = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        paramChatContainer.x = chatDefaultLeftPixel;
        paramChatContainer.y = chatDefaultTopPixel;
        windowManager.addView(chatheadView, paramChatContainer);
        chatheadView.setVisibility(View.GONE);
        setDialogViewValues();
        customizeShoppingWizard = new CustomizeShoppingWizardDialog(getBaseContext(), windowManager);
        createTimer();
        blinkDot();
        shoppingWizardFTUE = new ShoppingWizardFTUE(getBaseContext(), windowManager);
        removeAndCustomize = new RemoveView(getBaseContext(), windowManager);
        removeAndCustomize.addView();
        addTransparentSubscription(chatDefaultLeftPixel);
    }


    private void addClickListenerOnCHatHead() {
        chatheadView.setOnTouchListener(new View.OnTouchListener() {
            boolean inBounded = false;
            boolean inCustomize = false;
            long timeStart = 0, timeEnd = 0;
            Handler handlerLongClick = new Handler();
            Runnable runnableLongClick = new Runnable() {

                @Override
                public void run() {
                    Log.d(TAG, "Into runnableLongClick");
                    isLongclick = true;
                    removeAndCustomize.setVisibility(View.VISIBLE);
                }
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

                int initXCoordinate = (int) event.getRawX();
                int initYCoordinate = (int) event.getRawY();
                int xCordDestination, yCordDestination;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        timeStart = System.currentTimeMillis();
                        WindowManager.LayoutParams chatheadViewLayoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();
                        layoutParams.height = (int) getResources().getDimension(R.dimen.chathead_height);
                        layoutParams.width = (int) getResources().getDimension(R.dimen.chathead_height);
                        windowManager.updateViewLayout(chatheadView, chatheadViewLayoutParams);
                        chatheadContainer.setAlpha(1);
                        handlerLongClick.postDelayed(runnableLongClick, 600);
                        chatHeadXInitCoor = initXCoordinate;
                        chatHeadYInitCoor = initYCoordinate;
                        xInitMargin = layoutParams.x;
                        yInitMargin = layoutParams.y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int xDiffMove = initXCoordinate - chatHeadXInitCoor;
                        int yDiffMove = initYCoordinate - chatHeadYInitCoor;

                        xCordDestination = xInitMargin + xDiffMove;
                        yCordDestination = yInitMargin + yDiffMove;

                        if (isLongclick) {
                            if (initYCoordinate <= topOverlayTouchHeightPx) {
                                if (isLeftSide(initXCoordinate)) {
                                    if (!onRemoveView) {
                                        vibrator.vibrate(100);
                                        removeAndCustomize.updateRemoveLayoutColor();
                                        removeAndCustomize.updateCustomizeColorToOriginal();
                                    }
                                    inBounded = true;
                                    inCustomize = false;
                                    break;
                                } else {
                                    if (!onCustomize) {
                                        vibrator.vibrate(100);
                                        removeAndCustomize.updateCustomizeLayoutColor();
                                        removeAndCustomize.updateRemoveColorToOriginal();
                                    }
                                    inBounded = false;
                                    inCustomize = true;
                                }
                            } else {
                                if (onCustomize || onRemoveView) {
                                    removeAndCustomize.updateCustomizeColorToOriginal();
                                    removeAndCustomize.updateRemoveColorToOriginal();
                                }
                                inBounded = false;
                                inCustomize = false;
                            }

                        }
                        layoutParams.x = xCordDestination;
                        layoutParams.y = yCordDestination;
                        windowManager.updateViewLayout(chatheadView, layoutParams);
                        break;
                    case MotionEvent.ACTION_UP:
                        isLongclick = false;
                        removeAndCustomize.setVisibility(View.GONE);
                        handlerLongClick.removeCallbacks(runnableLongClick);
                        if (inBounded) {
                            //trackAssistantRemoved(itemModel.packageName);

                            stopShoppingAssistantService();
                            break;
                        } else if (inCustomize) {
                            //trackAssistantCustomize(itemModel.packageName);
                            customizeShoppingWizard.addCustomize();
                            Log.d(TAG, "In customize");
                            break;
                        }
                        int x_diff = initXCoordinate - chatHeadXInitCoor;
                        int y_diff = initYCoordinate - chatHeadYInitCoor;
                        if (Math.abs(x_diff) < 10 && Math.abs(y_diff) < 10) {
                            timeEnd = System.currentTimeMillis();
                            if ((timeEnd - timeStart) < 500) {
                                chatheadClick();
                                break;
                            }
                        }

                        yCordDestination = yInitMargin + y_diff;
                        layoutParams.y = yCordDestination;
                        //Log.e("UP", "layoutParams.x " + layoutParams.x + "layoutParams.y" + layoutParams.y);
                        inBounded = false;

                        resetPosition(initXCoordinate, false);
                        addTransparentSubscription(initXCoordinate);

                        break;
                    default:
                        //Log.d(Utils.LogTag, "chatheadView.setOnTouchListener  -> event.getAction() : default");
                        break;
                }
                return true;
            }
        });


    }

    private boolean isLeftSide(int currentX) {
        return currentX <= szWindow.x / 2;
    }

    private void addTransparentSubscription(final int xCoordinate) {
        if (chatheadTransparentSubscription != null) {
            chatheadTransparentSubscription.unsubscribe();
        }
        chatheadTransparentSubscription = Observable.just(1).interval(5, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        onErrorMain(e);
                    }

                    @Override
                    public void onNext(Long aLong) {
                        Log.d(TAG, "5 second ho gaya !!!!");
                        chatheadContainer.setAlpha(0.5f);
                        resetPosition(xCoordinate, true);
                    }
                });
        rxSubscriber.add(chatheadTransparentSubscription);
    }

    private void initializeDialogView(final View dialog) {
        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
        dialog.dispatchKeyEvent(keyEvent);
        ImageView headChatImage = (ImageView) dialog.findViewById(R.id.chathead_img);
        TextView webviewTitleText = (TextView) dialog.findViewById(R.id.webview_title_text);
        mWebView = (AdvancedWebView) dialog.findViewById(R.id.web_view);
        webViewContainer = dialog.findViewById(R.id.web_view_container);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setHorizontalScrollBarEnabled(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl("http://172.16.1.145:8000/deals/shopping_assistant_poc/");
        //mWebView.addJavascriptInterface(new WebAppInterface(getBaseContext()), "Android");
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        webviewChathead = dialog.findViewById(R.id.webview_chathead);
        /*ViewUtils.setText(webviewTitleText, itemModel.title, View.GONE);
        ViewUtils.setImage(getBaseContext(), headChatImage, itemModel.iconImgUrl, R.drawable.helpchat_logo);*/
        webviewChathead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isWebviewOpen) {
                    windowManager.removeViewImmediate(dialog);
                    windowManager.removeViewImmediate(backgroundLayout);
                    paramChatContainer.height = (int) getResources().getDimension(R.dimen.chathead_height);
                    paramChatContainer.width = (int) getResources().getDimension(R.dimen.chathead_height);
                    paramChatContainer.y = chatDefaultTopPixel;
                    paramChatContainer.x = chatDefaultLeftPixel;
                    windowManager.addView(chatheadView, paramChatContainer);
                    isWebviewOpen = false;
                }
            }
        });
    }


    private void showDialog() {
        dialog.setVisibility(View.VISIBLE);
        isWebviewOpen = true;
        WindowManager.LayoutParams paramDialog = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT);
        paramDialog.gravity = Gravity.BOTTOM;
        final Animation in = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shopping_wizard_bottom_up);
        in.setDuration(700);
        webViewContainer.setAnimation(in);
        windowManager.removeViewImmediate(chatheadView);
        windowManager.addView(dialog, paramDialog);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowManager.getDefaultDisplay().getSize(szWindow);
        } else {
            int w = windowManager.getDefaultDisplay().getWidth();
            int h = windowManager.getDefaultDisplay().getHeight();
            szWindow.set(w, h);
        }

        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

            if (layoutParams.y + (chatheadView.getHeight() + getStatusBarHeight()) > szWindow.y) {
                layoutParams.y = szWindow.y - (chatheadView.getHeight() + getStatusBarHeight());
                windowManager.updateViewLayout(chatheadView, layoutParams);
            }

            if (layoutParams.x != 0 && layoutParams.x < szWindow.x) {
                resetPosition(szWindow.x, false);
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "ShoppingWizardService.onConfigurationChanged -> portrait");

            if (layoutParams.x > szWindow.x) {
                resetPosition(szWindow.x, false);
            }

        }

    }

    private void resetPosition(int xCoordinateNow, boolean isIdleState) {
        if (chatheadView != null) {
            if (xCoordinateNow <= szWindow.x / 2) {
                final WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();
                if (isIdleState) {
                    overlayAnimation(chatheadView, mParams.x, chatIdleLeftPixel);
                } else {
                    overlayAnimation(chatheadView, mParams.x, chatDefaultLeftPixel);
                }


            } else {
                final WindowManager.LayoutParams mParams = (WindowManager.LayoutParams) chatheadView.getLayoutParams();
                if (isIdleState) {
                    overlayAnimation(chatheadView, mParams.x, chatIdleRightPixel);
                } else {
                    overlayAnimation(chatheadView, mParams.x, chatDefaultRightPixel);
                }
            }
        }
    }

    private void updateViewLayout(View view, Integer x, Integer y, Integer w, Integer h) {
        if (view != null) {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();

            if (x != null) lp.x = x;
            if (y != null) lp.y = y;
            if (w != null && w > 0) lp.width = w;
            if (h != null && h > 0) lp.height = h;
            if (!isWebviewOpen) {
                try {
                    windowManager.updateViewLayout(view, lp);
                } catch (IllegalArgumentException e) {
                    //Log.d(TAG, "Error occured while moving chathead");
                    //onErrorMain(e);
                }
            }
        }
    }

    private void overlayAnimation(final View view2animate, int viewX, int endX) {
        ValueAnimator translateLeft = ValueAnimator.ofInt(viewX, endX);
        translateLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int val = (Integer) valueAnimator.getAnimatedValue();
                updateViewLayout(view2animate, val, null, null, null);

            }
        });

        translateLeft.setDuration(500);
        translateLeft.start();

    }


    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void chatheadClick() {
        // trackAssistantOpened(itemModel.packageName);
        if (false) {
            shoppingWizardFTUE.addView();
        } else {
            isWebviewOpen = true;
            addBlackOverlay();
            showDialog();
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() -> startId=" + startId);
//        if (startId == Service.START_STICKY) {
        //itemModel = Parcels.unwrap(intent.getBundleExtra(DATA).getParcelable(DATA));
            /*ShoppingAssistantData itemModel = new ShoppingAssistantData();
            itemModel.iconImgUrl = "someUrl";
            itemModel.packageName = "com.olacabs.customer";
            itemModel.websiteUrl = "someUrl";*/
        //blacklistedApps = (HashSet<String>) intent.getBundleExtra(DATA).getSerializable(BLACKLISTED_APPS);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        dialog = new WebViewDialog(getBaseContext());
        initializeDialogView(dialog);
        isWebpageLoaded = true;
        handleStart();
//            return super.onStartCommand(intent, flags, startId);
//        } else {
        return Service.START_NOT_STICKY;
//        }
    }

    private void setDialogViewValues() {
        //ViewUtils.setImage(getBaseContext(), chatheadImg, itemModel.iconImgUrl, R.drawable.helpchat_logo);
    }

    private void createTimer() {
        Log.d(TAG, "Inside create timer");
        retryForegroundSubject = PublishSubject.create();
        foregroundAppSubscription = retryForegroundSubject
                .delay(1, TimeUnit.SECONDS)
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        Log.d(TAG, "TIMER METHOD IS UNSUBSCRIBED !!!!!");
                    }
                })
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "Inside create timer on complete");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "Error occurred while fetch foreground app for shopping assistant!", e);
                    }


                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "Timer method onNext !!!!!");
                        String currentApp = ProcessManager.getForegroundApp();
                        if (!TextUtils.isEmpty(currentApp)) {
                            currentApp = currentApp.trim();
                            if (!currentApp.equalsIgnoreCase("soganiabhijeet.com.myapplication") && !isCustomizeOpen && !isWebviewOpen && !isFTUEOpen && !blacklistedApps.contains(currentApp) && !isLongclick) {
                                //trackAssistantAutoRemoved(itemModel.packageName, currentApp);
                                Log.d(TAG, "App soganiabhijeet.com.myapplication" + " no longer in foreground " + " current foreground app is " + currentApp);
                                stopShoppingAssistantService();
                            }
                        } else {
                            //trackAssistantAutoRemoved(itemModel.packageName, "null");
                            Log.d(TAG, "App com.application.zomato" + " no longer in foreground " + " current foreground app is null");
                            stopShoppingAssistantService();
                        }
                        retryForegroundSubject.onNext(1);
                    }
                });

        Subscription subscribe = Observable.just(1)
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        Log.d(TAG, "Inside create timer retryForegroundSubject.onNext");
                        retryForegroundSubject.onNext(1);
                    }
                });
        rxSubscriber.add(foregroundAppSubscription);
        rxSubscriber.add(subscribe);

    }

    private void onErrorMain(Throwable throwable) {
        //Logger.logException(TAG, throwable.getMessage(), throwable);
    }


    private void stopShoppingAssistantService() {
        rxSubscriber.unsubscribe();
        Log.d(TAG, "Stopping shopping wizard");
        stopService(new Intent(ShoppingWizardService.this, ShoppingWizardService.class));
        //isCustomizeOpen = false;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (chatheadView != null && !isWebviewOpen) {
            windowManager.removeView(chatheadView);
        }

        if (dialog != null && isWebviewOpen) {
            windowManager.removeView(dialog);
        }
        if (backgroundLayout != null && isWebviewOpen) {
            windowManager.removeView(backgroundLayout);
        }

        if (removeAndCustomize != null) {
            windowManager.removeView(removeAndCustomize);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "ShoppingWizardService.onBind()");
        return null;
    }

    private void addBlackOverlay() {

        WindowManager.LayoutParams paramOverlay = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSPARENT);
        windowManager.addView(backgroundLayout, paramOverlay);
    }

    private void blinkDot() {
        chatheadView.setVisibility(View.VISIBLE);
        shoppingContainerAvatar.setVisibility(View.VISIBLE);

        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.3f, 1.0f, 1.3f, Animation.RELATIVE_TO_SELF, 0.5f, Animation
                .RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(500);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimation.setRepeatCount(Animation.INFINITE);


        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setDuration(500);
        if (false) {
            alphaAnimation.setRepeatCount(Animation.INFINITE);
        } else {
            alphaAnimation.setRepeatCount(2);
        }

        AnimationSet animationSet = new AnimationSet(false);
        animationSet.addAnimation(scaleAnimation);
        animationSet.addAnimation(alphaAnimation);
        shoppingContainerAvatar.startAnimation(animationSet);
        animationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                shoppingContainerAvatar.setVisibility(View.GONE);
                ViewGroup.LayoutParams params = blinkContainer.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                blinkContainer.setLayoutParams(params);
                chatheadView.bringToFront();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public class WebViewDialog extends LinearLayout {

        public WebViewDialog(Context context) {
            super(context);
            LayoutInflater.from(context).inflate(R.layout.shopping_wizard_webview_dialog, this);
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (down) {
                        if (isWebviewOpen) {
                            windowManager.removeViewImmediate(backgroundLayout);
                            windowManager.removeViewImmediate(dialog);
                            paramChatContainer.y = chatDefaultTopPixel;
                            paramChatContainer.x = chatDefaultLeftPixel;
                            windowManager.addView(chatheadView, paramChatContainer);
                            isWebviewOpen = false;
                        }
                        return true;
                    }
                    return true;
            }
            return super.dispatchKeyEvent(event);
        }
    }

    private class CustomizeShoppingWizardDialog extends LinearLayout {
        private View rootView;
        private RadioButton oneDayButton;
        private RadioButton oneWeekButton;
        private Switch aSwitch;
        private TextView cancelButton;
        private TextView ctaDone;
        private WindowManager windowManager;
        private WindowManager.LayoutParams customizeParams;

        private CustomizeShoppingWizardDialog(Context context, WindowManager windowManager) {
            super(context);
            this.windowManager = windowManager;
            this.init(context);
            customizeParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSPARENT);
            customizeParams.gravity = Gravity.CENTER;
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (down) {
                        removeCustomize();
                        return true;
                    }
                    return true;
            }
            return super.dispatchKeyEvent(event);
        }

        private void addCustomize() {
            isCustomizeOpen = true;
            windowManager.addView(rootView, customizeParams);
        }

        private void removeCustomize() {
            isCustomizeOpen = false;
            this.windowManager.removeView(rootView);
        }

        private void init(Context context) {
            rootView = LayoutInflater.from(context).inflate(R.layout.customize_shopping_wizard_dialog, this);
            oneDayButton = (RadioButton) rootView.findViewById(R.id.one_day_button);
            oneWeekButton = (RadioButton) rootView.findViewById(R.id.one_week_button);
            cancelButton = (TextView) rootView.findViewById(R.id.cancel_button);
            ctaDone = (TextView) rootView.findViewById(R.id.cta_done);
            aSwitch = (Switch) rootView.findViewById(R.id.pref_category_switch);
            cancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeCustomize();
                }
            });
            oneWeekButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (oneDayButton.isChecked()) {
                        oneDayButton.setChecked(false);
                    }
                }
            });
            oneDayButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (oneWeekButton.isChecked()) {
                        oneWeekButton.setChecked(false);
                    }
                }
            });
            ctaDone.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //closeCustomizeDialog(getReturnEnum(), aSwitch.isChecked());
                    removeCustomize();
                }
            });
        }

       /* private int getReturnEnum() {
            if (oneWeekButton.isChecked()) {
                return Constants.ShoppingWizardCustomizeState.ONE_WEEK;
            }
            if (oneDayButton.isChecked()) {
                return Constants.ShoppingWizardCustomizeState.ONE_DAY;
            }
            return Constants.ShoppingWizardCustomizeState.CANCEL;
        }*/
    }


    private void closeCustomizeDialog(int userPreference, Boolean showNotifications) {
        /*Set<String> bgMutedString = ApplicationPreference.getInstance().getStringSet(SyncService.MUTED_PACKAGES_LIST, new HashSet<String>());
        String eventString = "none";
        if (!showNotifications) {
            bgMutedString.add(itemModel.packageName);
        } else {
            for (String packageName : bgMutedString) {
                if (packageName.equals(itemModel.packageName)) {
                    bgMutedString.remove(packageName);
                }
            }
        }
        //ApplicationPreference.getInstance().set(SyncService.MUTED_PACKAGES_LIST, bgMutedString);
        switch (userPreference) {
            case Constants.ShoppingWizardCustomizeState.ONE_DAY:
                eventString = "today";
                ShoppingAssistantAppDbAdapter.stopNotificationForTime(itemModel.packageName, AndroidUtilities.getUnixTimeStamp(), ONE_DAY_IN_MILLIS + AndroidUtilities.getUnixTimeStamp(), itemModel.websiteUrl, itemModel.iconImgUrl, itemModel.title);
                //ShoppingAssistantAppDbAdapter.printCurrentItems();
                break;
            case Constants.ShoppingWizardCustomizeState.ONE_WEEK:
                eventString = "oneweek";
                /hoppingAssistantAppDbAdapter.stopNotificationForTime(itemModel.packageName, AndroidUtilities.getUnixTimeStamp(), ONE_DAY_IN_MILLIS * 7 + AndroidUtilities.getUnixTimeStamp(), itemModel.websiteUrl, itemModel.iconImgUrl, itemModel.title);
                //ShoppingAssistantAppDbAdapter.printCurrentItems();
                break;
            case Constants.ShoppingWizardCustomizeState.CANCEL:
                break;
        }*/
        //trackAssistantMuted(itemModel.packageName, eventString, showNotifications);
    }


    private class WebAppInterface {
        Context mContext;

        /**
         * Instantiate the interface and set the context
         */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Show a toast from the web page
         */
        @JavascriptInterface
        public void copyToClipboard(String content) {
            //stopShoppingAssistantService();
            //CommonUtils.copyToClipBoard(mContext, content);
            Log.d(TAG, "at copyToClipboard");
        }

        @JavascriptInterface
        public void openDeeplink(String deeplink) {
            Log.d(TAG, "at openDeeplink");
            try {
                Uri uri = Uri.parse(deeplink);
                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(i);
            } catch (Exception e) {
                Log.d(TAG, "Error opening deeplink !");
                return;
            }

        }

        @JavascriptInterface
        public void webpageLoaded() {
            Log.d(TAG, "at webpage loaded");
            if (!isWebpageLoaded) {
                Log.d(TAG, "at webpage loaded observable");
                Observable.just(1).subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "onCompleted");
                    }

                    @Override
                    public void onError(Throwable e) {
                        onErrorMain(e);
                    }

                    @Override
                    public void onNext(Integer integer) {
                    }

                });
            }


        }

        @JavascriptInterface
        public void showDialog(String couponCode, String cta, String iconUrl) {
            Toast.makeText(getBaseContext(), "You coupon code has been copied", Toast.LENGTH_LONG).show();

        }

        @JavascriptInterface
        public void stopShoppingAssistant() {
            Log.d(TAG, "at stopShoppingAssistant");
            stopShoppingAssistantService();
        }
    }

   /* private void trackOnBoardingShown(String packageName) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_onboarding_shown).setContent(String.valueOf(packageName));
        Analytics.track(event);
    }

    private void trackOnBoardingAccepted(String packageName) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_onboarding_accepted).setContent(String.valueOf(packageName));
        Analytics.track(event);
    }

    private void trackOnBoardingRemindLater(String packageName) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_onboarding_remind_later).setContent(String.valueOf(packageName));
        Analytics.track(event);
    }

    private void trackAssistantRemoved(String packageName) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_removed).setContent(String.valueOf(packageName));
        Analytics.track(event);
    }

    private void trackAssistantOpened(String packageName) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_opened).setContent(String.valueOf(packageName));
        Analytics.track(event);
    }

    private void trackAssistantCustomize(String packageName) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_customize_clicked).setContent(String.valueOf(packageName));
        Analytics.track(event);
    }

    private void trackAssistantMuted(String packageName, String mutedTime, Boolean isNotificationEnabled) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_muted).setContent(String.valueOf(packageName)).setDesc1(mutedTime).setDesc2(String.valueOf(isNotificationEnabled));
        Analytics.track(event);
    }

    private void trackAssistantAutoRemoved(String packageName, String topPackage) {
        Analytics.Builder event = new Analytics.Builder(AkoshaApplication.getInstance());
        event.setEventCategory(EventCategory.SHOPPING_ASSISTANT).setEventDesc(R.string.assistant_autoremoved).setContent(String.valueOf(packageName)).setDesc1(packageName);
        Analytics.track(event);
    }*/


    private class RemoveView extends LinearLayout {
        private View rootView;
        private WindowManager windowManager;
        private WindowManager.LayoutParams paramsRemoveView;
        private TextView removeText;
        private TextView customizeText;
        private ImageView customizeImg;
        private ImageView removeImg;

        private RemoveView(Context context, WindowManager windowManager) {
            super(context);
            this.windowManager = windowManager;
            this.init(context);
            paramsRemoveView = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    topOverlayHeightPx,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            paramsRemoveView.gravity = Gravity.TOP;
            this.removeImg = (ImageView) rootView.findViewById(R.id.remove_img);
            this.removeText = (TextView) rootView.findViewById(R.id.remove_txt);
            this.customizeImg = (ImageView) rootView.findViewById(R.id.customize_img);
            this.customizeText = (TextView) rootView.findViewById(R.id.customize_txt);
        }

        private void init(Context context) {
            rootView = LayoutInflater.from(context).inflate(R.layout.shopping_wizard_remove_layout, this);
        }

        public void addView() {
            windowManager.addView(rootView, paramsRemoveView);
            this.setVisibility(GONE);
        }

        public void removeView() {
            windowManager.removeView(rootView);
        }

        private void updateRemoveLayoutColor() {

            removeImg.setColorFilter(getResources().getColor(R.color.shopping_wizard_remove_color));
            removeText.setTextColor(getResources().getColor(R.color.shopping_wizard_remove_color));
            onRemoveView = true;
        }

        private void updateCustomizeLayoutColor() {
            customizeImg.setColorFilter(getResources().getColor(R.color.shopping_wizard_customize_color));
            customizeText.setTextColor(getResources().getColor(R.color.shopping_wizard_customize_color));
            onCustomize = true;
        }

        private void updateRemoveColorToOriginal() {
            onRemoveView = false;
            removeImg.setColorFilter(getResources().getColor(R.color.white));
            removeText.setTextColor(getResources().getColor(R.color.white));

        }

        private void updateCustomizeColorToOriginal() {
            onCustomize = false;
            customizeImg.setColorFilter(getResources().getColor(R.color.white));
            customizeText.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private class ShoppingWizardFTUE extends LinearLayout {
        private View rootView;
        private WindowManager windowManager;
        private WindowManager.LayoutParams paramsFTUE;
        private ImageView chatheadImg;
        private TextView getStartedButton;
        private TextView remindMeLaterButton;
        private View blinkLayout;

        private ShoppingWizardFTUE(Context context, WindowManager windowManager) {
            super(context);
            this.windowManager = windowManager;
            this.init(context);
            paramsFTUE = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSPARENT);
        }

        private void init(Context context) {
            rootView = LayoutInflater.from(context).inflate(R.layout.shopping_wizard_ftue, this);
            chatheadImg = (ImageView) rootView.findViewById(R.id.chathead_img);
            chatheadImg.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeView(true);
                }
            });
            getStartedButton = (TextView) rootView.findViewById(R.id.lets_get_started_button);
            remindMeLaterButton = (TextView) rootView.findViewById(R.id.reming_me_later_button);
            getStartedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //trackOnBoardingAccepted(itemModel.packageName);
                    removeView(false);
                }
            });
            blinkLayout = rootView.findViewById(R.id.grid_blinking_dot);
            remindMeLaterButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //trackOnBoardingRemindLater(itemModel.packageName);
                    removeView(true);
                    //MultiprocessPreference.getInstance().set(ShoppingAssistantSyncService.SHOPPING_ASSISTANT_MUTED_TIME, ONE_DAY_IN_MILLIS * 7 + AndroidUtilities.getUnixTimeStamp());
                    //ShoppingAssistantAppTaskScheduler.cancelShoppingAssistant(getBaseContext());
                    stopShoppingAssistantService();

                }
            });
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if (down) {
                        removeView(true);
                        return true;
                    }
                    return true;
            }
            return super.dispatchKeyEvent(event);
        }

        private void blinkDot() {
            ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 3.0f, 1.0f, 3.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation
                    .RELATIVE_TO_SELF, 0.5f);
            scaleAnimation.setDuration(500);
            scaleAnimation.setRepeatCount(Animation.INFINITE);
            AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
            alphaAnimation.setDuration(500);
            alphaAnimation.setRepeatCount(Animation.INFINITE);
            AnimationSet animationSet = new AnimationSet(false);
            animationSet.addAnimation(scaleAnimation);
            animationSet.addAnimation(alphaAnimation);
            blinkLayout.startAnimation(animationSet);
        }

        public void addView() {
            final Animation in = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shopping_wizard_bottom_up);
            //trackOnBoardingShown(itemModel.packageName);
            in.setDuration(700);
            rootView.setAnimation(in);
            chatheadView.setVisibility(GONE);
            isFTUEOpen = true;
            blinkDot();
            windowManager.addView(rootView, paramsFTUE);
        }

        public void removeView(boolean isFirstTime) {
            //ApplicationPreference.getInstance().set(IS_FIRST_TIME, isFirstTime);
            blinkLayout.setAnimation(null);
            isFTUEOpen = false;
            chatheadView.setVisibility(VISIBLE);
            shoppingContainerAvatar.getAnimation().cancel();
            windowManager.removeView(rootView);
        }
    }


}