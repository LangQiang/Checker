package com.lq.check;

import android.content.Context;
import android.util.Log;

import org.lionsoul.ip2region.DbSearcher;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

public class CheckedCore implements IXposedHookLoadPackage {

    private boolean isContextGain;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) {

        Log.e("sensitive", "handleLoadPackage");
        getContext(loadPackageParam, new OnContextGainListener() {
            @Override
            public void onGain(Context context) {
                Log.e("sensitive","onGain invoke");
                if (context == null) {
                    return;
                }
                for (SensitiveApiInfo sensitiveApiInfo : CheckLoader.load(context)) {
                    Log.e("sensitive", "register");
                    registerHook(sensitiveApiInfo, loadPackageParam);

                }
            }
        });

        try {
            findAndHookMethod("java.net.Socket",
                    loadPackageParam.classLoader,
                    "connect", SocketAddress.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            try {
                                Object arg = param.args[0];
                                InetSocketAddress inetSocketAddress = (InetSocketAddress) arg;
                                String hostAddress = inetSocketAddress.getAddress().getHostAddress();

                                DbSearcher dbSearcher = CheckLoader.loadIpDb();
                                String region = "";
                                if (dbSearcher != null) {
                                    region = dbSearcher.memorySearch(hostAddress).getRegion();
                                }
                                Log.e("sensitive", region);
                            } catch (Exception e) {
                                Log.e("sensitive", e.getMessage());
                            }
//                            Printer.printStackTrace(new SensitiveApiInfo("java.net.Socket", "connect", "find IP"), loadPackageParam.processName);
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                        }
                    });
        } catch (Throwable r) {
            // r.printStackTrace();
        }

    }

    private void registerHook(final SensitiveApiInfo sensitiveApiInfo, final XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            findAndHookMethod(sensitiveApiInfo.classFullName,
                    loadPackageParam.classLoader,
                    sensitiveApiInfo.methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            param.setResult("Mac?Hooked!");
                            super.beforeHookedMethod(param);
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            XposedBridge.log("handleLoadPackage getHardwareAddress hooked");
                            Printer.printStackTrace(sensitiveApiInfo, loadPackageParam.processName);
                        }
                    });
        } catch (Throwable r) {
            // r.printStackTrace();
        }
    }

    private void getContext(XC_LoadPackage.LoadPackageParam loadPackageParam, final OnContextGainListener contextGainListener) {
        try {
            Class<?> ContextClass = findClass("android.content.ContextWrapper", loadPackageParam.classLoader);
            findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    if (isContextGain) {
                        return;
                    }
                    isContextGain = true;
                    if (contextGainListener != null) {
                        contextGainListener.onGain((Context) param.getResult());
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("getContext error:" + t);
        }
    }
}
