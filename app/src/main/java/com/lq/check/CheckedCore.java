package com.lq.check;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.lionsoul.ip2region.DbSearcher;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

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

        XC_MethodHook handleLoadPackage_getHardwareAddress_hooked = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                            param.setResult("Mac?Hooked!");
                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                XposedBridge.log("handleLoadPackage getHardwareAddress hooked");

                if (printByParam(sensitiveApiInfo, loadPackageParam, param)) {
                    return;
                }
                Printer.printStackTrace(sensitiveApiInfo, loadPackageParam.processName);
            }

            private boolean printByParam(SensitiveApiInfo sensitiveApiInfo, XC_LoadPackage.LoadPackageParam loadPackageParam, MethodHookParam param) {
                if (!sensitiveApiInfo.classFullName.contains("android.provider.Settings")) {
                    return false;
                }
                Log.e("sensitive", sensitiveApiInfo.classFullName + "  测试打印");
                try {
                    if (((String) param.args[1]).equals(Settings.Secure.ANDROID_ID)) {
                        Printer.printStackTrace(sensitiveApiInfo, loadPackageParam.processName);
                        return true;
                    }
                } catch (Exception ignore) {

                }
                return false;
            }
        };

        try {
//            //todo 参数配置到文件里 先暂时特异处理 没时间整
//            if (sensitiveApiInfo.classFullName.contains("android.provider.Settings")) {
//                findAndHookMethod(sensitiveApiInfo.classFullName,
//                        loadPackageParam.classLoader,
//                        sensitiveApiInfo.methodName, ContentResolver.class, String.class,
//                        handleLoadPackage_getHardwareAddress_hooked);
//            } else if (sensitiveApiInfo.classFullName.contains("android.app.AlarmManager")) {
//                findAndHookMethod(sensitiveApiInfo.classFullName,
//                        loadPackageParam.classLoader,
//                        sensitiveApiInfo.methodName, int.class, long.class, PendingIntent.class,
//                        handleLoadPackage_getHardwareAddress_hooked);
//                findAndHookMethod(sensitiveApiInfo.classFullName,
//                        loadPackageParam.classLoader,
//                        sensitiveApiInfo.methodName, int.class, long.class, String.class, AlarmManager.OnAlarmListener.class,
//                        handleLoadPackage_getHardwareAddress_hooked);
//            } else {
//                List<Object> paramAndCallback = new ArrayList<>();
//                for (String param : sensitiveApiInfo.params) {
//                    paramAndCallback.add(Class.forName(param));
//                }
//                paramAndCallback.add(handleLoadPackage_getHardwareAddress_hooked);
//                findAndHookMethod(sensitiveApiInfo.classFullName,
//                        loadPackageParam.classLoader,
//                        sensitiveApiInfo.methodName,
//                        paramAndCallback);
//            }
            String methodStr = sensitiveApiInfo.methodName;
            String[] methodSpit = methodStr.split("&");
            String methodName = methodSpit[0];
            List<Object> paramAndCallback = new ArrayList<>();
            for (int i = 1; i < methodSpit.length; i ++) {
                Object obj = Utils.getParamClass(methodSpit[i]);
                if (obj != null) {
                    paramAndCallback.add(obj);
                }
            }
            paramAndCallback.add(handleLoadPackage_getHardwareAddress_hooked);

            findAndHookMethod(sensitiveApiInfo.classFullName,
                    loadPackageParam.classLoader,
                    methodName,
                    paramAndCallback.toArray());
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
