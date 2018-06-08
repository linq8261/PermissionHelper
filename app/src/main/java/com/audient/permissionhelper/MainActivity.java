package com.audient.permissionhelper;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.audient.permission_helper.OnPermissionDeniedListener;
import com.audient.permission_helper.OnPermissionGrantedListener;
import com.audient.permission_helper.PermissionHelper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_test:
                PermissionHelper.requestPermissions(this, () -> {
                    // on granted
                    Toast.makeText(MainActivity.this, "onGranted", Toast.LENGTH_SHORT).show();

                }, () -> {
                    // on denied
                    Toast.makeText(MainActivity.this, "onDenied", Toast.LENGTH_SHORT).show();

                }, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                break;
        }
    }
}
