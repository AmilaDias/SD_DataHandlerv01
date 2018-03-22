package sd2018.sd_datahandlerv01;

/**
 * Created by asdte on 3/22/2018.
 */


import android.app.Application;
import android.content.Context;
import com.secneo.sdk.Helper;



public class MApplication extends Application {
    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
    }
}
