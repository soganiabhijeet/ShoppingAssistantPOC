package soganiabhijeet.com.myapplication;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by abhijeetsogani on 8/4/16.
 */

public class HelpChatAccessibilityService extends AccessibilityService {

    private static final String TAG = HelpChatAccessibilityService.class.getSimpleName();
    private static final String PKG_PHONE_APP = "com.android.chrome";
    private static final String SYSTEM_DIALOG = "alert";

    private static boolean landingActivityVisible = false;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //getChromeUrl(getRootInActiveWindow());
        /*if (event == null || event.getSource() == null || TextUtils.isEmpty(event.getSource().getViewIdResourceName())) {
            return;
        }
        debug("Event type " + event.getEventType());
        debug("Event resource id " + event.getSource().findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar"));
        List<AccessibilityNodeInfo> accessibilityNodeInfos=event.getSource().findAccessibilityNodeInfosByViewId("lst-ib");
        for(AccessibilityNodeInfo accessibilityNodeInfo:accessibilityNodeInfos){
            debug("getContentDescription " +accessibilityNodeInfo.getContentDescription());
        }*/
        debug("Event type " + event.getEventType());
        getChromeUrl(getRootInActiveWindow());
    }

    public void getChromeUrl(AccessibilityNodeInfo nodeInfo) {
        //Use the node info tree to identify the proper content.
        //For now we'll just log it to logcat.
        Log.d(TAG, toStringHierarchy(nodeInfo, 0));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private String toStringHierarchy(AccessibilityNodeInfo info, int depth) {
        if (info == null) return "";

        String result = "depth is " + depth;

        if ("com.android.chrome:id/url_bar".equals(info.getViewIdResourceName())) {
            debug("Url is " + info.getText());
        }
        for (int i = 0; i < depth; i++) {
            result += "  ";
        }

        //result += info.toString();

        for (int i = 0; i < info.getChildCount(); i++) {
            result += "\n" + toStringHierarchy(info.getChild(i), depth + 1);
        }

        return result;
    }

    private void closeUssdPopUp(AccessibilityNodeInfo accessibilityNodeInfo) {
        debug("Try to close ussd popup.");
        if (accessibilityNodeInfo == null) {
            debug("can't close ussd pop up no info");
            return;
        }
        if (!landingActivityVisible) {
            debug("can't close landing activity not open");
            return;
        }
        for (int i = 0; i < accessibilityNodeInfo.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = accessibilityNodeInfo.getChild(i);
            if (nodeInfo != null) {
                CharSequence text = nodeInfo.getText();
                if (!TextUtils.isEmpty(text)
                        && (nodeInfo.getClassName().equals(Button.class.getName())
                        || nodeInfo.getClassName().equals(TextView.class.getName()))) {
                    debug("ussd pop up closed.");
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        debug("accessibility service onServiceConnected");
        //SimPreferencesManager.getInstance().setAccessibilityServiceEnable();
        debug("HelpChatAccessibilityService enabled connected.");
        /*if (AndroidUtilities.isAccessibilityServiceEnabled(this)
                && ApplicationPreference.getInstance().getBoolean(PreferenceKeys.ACCESSIBILITY_ENABLE_CLICKED, false)) {
            if(SimPreferencesManager.getInstance().isDataCardSegmentEventsOn()) {
                sendPermissionEvent();
            }
            Intent intent = new Intent(this, AccessibilityTutorialActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);*/
        //ApplicationPreference.getInstance().set(PreferenceKeys.ACCESSIBILITY_ENABLE_CLICKED, false);
    }
    //}


    @Override
    public void onInterrupt() {
        debug("onInterrupt called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        debug("accessibility service destroyed");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        debug("accessibility service onUnbind");
        return super.onUnbind(intent);
    }

    private static void debug(Object object) {
        Log.d(TAG, object.toString());
    }

    public static boolean isLandingActivityVisible() {
        return landingActivityVisible;
    }

    public static void setIsLandingActivityVisible(boolean landingActivityVisible) {
        HelpChatAccessibilityService.landingActivityVisible = landingActivityVisible;
    }

}

