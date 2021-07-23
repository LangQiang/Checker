package com.lq.check;

import android.util.Log;

public class Printer {
    public static void printStackTrace(SensitiveApiInfo sensitiveApiInfo, String process) {
        Log.e("sensitive","action:" + sensitiveApiInfo.desc + "  method:" + sensitiveApiInfo.methodName);
        Log.e("sensitive","process:" + process);

        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements.length > 0) {
            for (StackTraceElement element : stackElements) {
                Log.e("sensitive","at " + element.getClassName() + "." + element.getMethodName() +
                        "(" + element.getFileName() + ":" + element.getLineNumber() + ")");
            }
        } else {
            try {
                RuntimeException e = new RuntimeException("<Start dump Stack !>");
                e.fillInStackTrace();
                Log.e("sensitive", "++++++++++++", e);
            } catch (Throwable r) {
                r.printStackTrace();
            }
        }
    }
}
