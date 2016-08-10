package soganiabhijeet.com.myapplication;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;

/**
 * Created by abhijeetsogani on 7/2/16.
 */

public class Utils {
    public static String LogTag = "henrytest";
    public static String EXTRA_MSG = "extra_msg";


    public static boolean canDrawOverlays(Context context){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }else{
            return Settings.canDrawOverlays(context);
        }


    }
    public static int getDpInPixels(Context context, int dp) {
        Resources r = context.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;

    }


}