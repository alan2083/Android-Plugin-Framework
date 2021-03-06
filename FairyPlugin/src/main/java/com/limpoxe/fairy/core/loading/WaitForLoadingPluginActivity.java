package com.limpoxe.fairy.core.loading;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.limpoxe.fairy.content.LoadedPlugin;
import com.limpoxe.fairy.content.PluginDescriptor;
import com.limpoxe.fairy.core.FairyGlobal;
import com.limpoxe.fairy.core.PluginLauncher;
import com.limpoxe.fairy.util.LogUtil;

/**
 * 这个页面要求尽可能的简单
 * Created by cailiming on 16/10/12.
 */

public class WaitForLoadingPluginActivity extends Activity {

    private PluginDescriptor pluginDescriptor;
    private Handler handler;
    private long loadingAt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //由于在PluginInstrumentaionWrapper中，忽略了对WaitForLoadingPluginActivity的Context
        //的处理，这里的savedInstanceState如果包含插件Fragment信息，会因Classloader没有更换导致
        //FragmentManager尝试自动恢复插件Fragment时出现ClassNotFound异常，
        //这里直接将savedInstanceState置空，忽略之
        if (savedInstanceState != null) {
            savedInstanceState.clear();
            savedInstanceState = null;
        }
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // 是否需要全屏取决于上个页面是否为全屏,
        // 目的是和上个页面保持一致, 否则被透视的页面会发生移动
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int resId = FairyGlobal.getLoadingResId();
        LogUtil.i("WaitForLoadingPluginActivity ContentView Id = " + resId);

        if (resId != 0) {
            setContentView(resId);
        }
        handler = new Handler();
        loadingAt = System.currentTimeMillis();

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceState.clear();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtil.i("WaitForLoadingPluginActivity Shown");
        if (pluginDescriptor != null && !PluginLauncher.instance().isRunning(pluginDescriptor.getPackageName())) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    PluginLauncher.instance().startPlugin(pluginDescriptor);

                    long remainTime = (loadingAt + FairyGlobal.getMinLoadingTime()) - System.currentTimeMillis();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LoadedPlugin loadedPlugin = PluginLauncher.instance().getRunningPlugin(pluginDescriptor.getPackageName());
                            if (loadedPlugin != null && loadedPlugin.pluginApplication != null) {
                                LogUtil.i("WaitForLoadingPluginActivity open target");
                                startActivity(getIntent());
                                finish();
                            } else {
                                LogUtil.w("WTF!", pluginDescriptor, loadedPlugin);
                                finish();
                            }
                        }
                    },  remainTime);
                }
            }).start();
        } else {
            LogUtil.w("WTF!", pluginDescriptor);
            finish();
        }
    }

    public void setTargetPlugin(PluginDescriptor pluginDescriptor) {
        this.pluginDescriptor = pluginDescriptor;
    }
}
