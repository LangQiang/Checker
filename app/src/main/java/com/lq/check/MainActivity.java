package com.lq.check;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lq.imei.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MyAdapter myAdapter;
    private EditText et;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.lv);
        myAdapter = new MyAdapter(this);
        listView.setAdapter(myAdapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("删除");
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CheckLoader.deleteOneSensitiveItem((SensitiveApiInfo) myAdapter.getItem(position));
                        myAdapter.deleteItem(position);
                        alertDialog.dismiss();
                    }
                });
                alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
                return true;
            }
        });

        et = findViewById(R.id.et);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                add();
            }
        });

        int permissionInt = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionInt == PackageManager.PERMISSION_GRANTED) {
            onGain();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }
    }

    private void add() {
        if (et.getText() == null) {
            return;
        }
        String sensitiveStr = et.getText().toString();
        if (sensitiveStr.split("#").length != 3) {
            Toast.makeText(this, "格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sensitiveStr.contains("\r\n") || sensitiveStr.contains("\n") || sensitiveStr.contains("\r")) {
            Toast.makeText(this, "不能包含换行符", Toast.LENGTH_SHORT).show();
            return;
        }
        int permissionInt = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionInt == PackageManager.PERMISSION_GRANTED) {
            CheckLoader.saveSensitiveItem(sensitiveStr);
            onGain();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onGain();
            } else {
                Toast.makeText(this, "请开启读写权限！", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onGain() {
        et.setVisibility(View.VISIBLE);
        List<SensitiveApiInfo> load = CheckLoader.load(this);
        myAdapter.setNewData(load);
    }

    private static class MyAdapter extends BaseAdapter {

        private List<SensitiveApiInfo> data;
        private Context context;

        MyAdapter(Context context) {
            data = new ArrayList<>();
            this.context =context;
        }

        void setNewData(List<SensitiveApiInfo> data) {
            if (data == null) {
                this.data = new ArrayList<>();
            } else {
                this.data = data;
            }
            notifyDataSetChanged();
        }

        void deleteItem(int position) {
            if (this.data.size() > position) {
                this.data.remove(position);
                notifyDataSetChanged();
            }
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(context, R.layout.sensitive_item_layout, null);
            }

            SensitiveApiInfo sensitiveApiInfo = data.get(position);

            TextView tv = convertView.findViewById(R.id.tv);
            tv.setText(String.format("%s#%s#%s", sensitiveApiInfo.classFullName, sensitiveApiInfo.methodName, sensitiveApiInfo.desc));

            return convertView;
        }
    }
}
