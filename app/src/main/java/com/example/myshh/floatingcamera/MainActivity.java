package com.example.myshh.floatingcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            showOverlayPermissionDialog();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showOverlayPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Give permissions");
        builder.setMessage("Application don't have permissions to draw overlays, which is needed for floating camera, from where " +
                "you can take photos. Click ok and in next step give this application needed permissions.");
        builder.setPositiveButton("Ok", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
        });
        builder.setOnCancelListener(dialog -> Toast.makeText(this,
                "Draw over other app permission not available. Please give needed permissions for app to work.",
                Toast.LENGTH_LONG).show());
        builder.show();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    public void launchButtonClick(View v) {
        checkPermissions();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog();
        } else {
            startService(new Intent(MainActivity.this, FloatingViewService.class));
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            //Check if the permission is granted or not.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Please give needed permissions for app to work.",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void infoButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Give permissions");
        builder.setMessage("This application needs following permissions:\n" +
                "- Camera permission for taking photos\n" +
                "- Storage permission for saving photos\n" +
                "- Overlay permission for floating camera view\n" +
                "Please give this app all needed permissions for it to work properly.");
        builder.setPositiveButton("Ok", (dialog, which) -> {
        });
        builder.show();
    }
}
