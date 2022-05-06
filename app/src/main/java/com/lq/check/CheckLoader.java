package com.lq.check;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

public class CheckLoader {

    private static DbSearcher dbSearcher = loadIpDb();

    @NonNull
    public static List<SensitiveApiInfo> load(@NonNull Context context) {
        return loadFromAssets(context);
    }

    @NonNull
    private static List<SensitiveApiInfo> loadFromAssets(Context context) {
        List<SensitiveApiInfo> result;
        try {
//            InputStream inputStream = context.getAssets().open("sensitive");
//            result = parse(inputStream);
            result = readFromSDCard(context);

        } catch (Exception e) {
            result = readFromSDCard(context);
            e.printStackTrace();
        }
        return result;
    }

    @NonNull
    private static List<SensitiveApiInfo> readFromSDCard(Context context) {
        List<SensitiveApiInfo> result = new ArrayList<>();
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/checkAssets", "sensitive");
        if (file.exists()) {
            Log.e("sensitive", "find sdCard file!");
        } else {
            Toast.makeText(context, "no assert in sdCard", Toast.LENGTH_SHORT).show();
        }
        try {
            result = parse(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    @NonNull
    private static List<SensitiveApiInfo> parse(InputStream inputStream) throws IOException {

        List<SensitiveApiInfo> result = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[1024];
        int len;
        while ((len = inputStream.read(bytes)) != -1) {
            sb.append(new String(bytes, 0, len));
        }
        inputStream.close();
        String[] split = sb.toString().split("(\\r?\\n)|(\\r)");
        for (String s : split) {
            String[] ele = s.split("#");
            if (ele.length != 3) {
                continue;
            }
            result.add(new SensitiveApiInfo(ele[0], ele[1], ele[2]));

            XposedBridge.log("Sensitives:  " + result);
        }
        return result;
    }

    public static DbSearcher loadIpDb() {

        if (dbSearcher == null) {

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/checkAssets", "ip2region.db");

            try {
                return new DbSearcher(new DbConfig(), file.getAbsolutePath());
            } catch (Exception ignore) {
                return null;
            }
        } else {
            return dbSearcher;
        }
    }

    public static void saveSensitiveItem(String sensitiveInfo) {

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/checkAssets");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return;
            }
        }

        File file = new File(dir, "sensitive");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(('\n' + sensitiveInfo + '\n').getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void deleteOneSensitiveItem(SensitiveApiInfo sensitiveInfo) {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/checkAssets");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return;
            }
        }

        File file = new File(dir, "sensitive");

        try {
            List<SensitiveApiInfo> result = parse(new FileInputStream(file));
            Iterator<SensitiveApiInfo> iterator = result.iterator();
            while (iterator.hasNext()) {
                SensitiveApiInfo next = iterator.next();
                if (sensitiveInfo.methodName.equals(next.methodName) && sensitiveInfo.classFullName.equals(next.classFullName)) {
                    iterator.remove();
                }
            }
            saveAll(result, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveAll(List<SensitiveApiInfo> sensitiveApiInfos, File file) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, true);
            for (SensitiveApiInfo sensitiveApiInfo : sensitiveApiInfos) {
                String sensitiveInfoStr = '\n' + sensitiveApiInfo.classFullName + "#" + sensitiveApiInfo.methodName + "#" + sensitiveApiInfo.desc + '\n';
                fileOutputStream.write(sensitiveInfoStr.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
