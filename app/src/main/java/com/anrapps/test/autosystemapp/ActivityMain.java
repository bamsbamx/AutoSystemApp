/*
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *                    Version 2, December 2004
 *
 * Copyright (c) 2016.  Aner Torre <anernaiz@gmail.com>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 *            DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 *   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 *  0. You just DO WHAT THE FUCK YOU WANT TO.
 *
 */

package com.anrapps.test.autosystemapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.anrapps.test.autosystemapp.task.MoveToSystemTask;
import com.anrapps.test.autosystemapp.task.RemoveFromSystemTask;

public class ActivityMain extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Checking for MODIFY_PHONE_STATE is enough to know if the app is SYSTEM app, because that
        //permission is only granted to SYSTEM apps which have that permission declared in their Manifest
        //Note that THIS permission is not changeable, which means it cannot be granted from ADB
        if (isPermissionGranted(this, Manifest.permission.MODIFY_PHONE_STATE)) setupUiForGrantedPermission();
        else setupUiForDeniedPermission();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_denied:
                new MoveToSystemTask(this).execute();
                break;
            case R.id.button_granted:
                new RemoveFromSystemTask(this).execute();
                break;
            default: //Nothing to do
        }
    }

    private void setupUiForDeniedPermission() {
        setContentView(R.layout.activity_main_denied);
        findViewById(R.id.button_denied).setOnClickListener(this);
    }

    private void setupUiForGrantedPermission() {
        setContentView(R.layout.activity_main_granted);
        findViewById(R.id.button_granted).setOnClickListener(this);
    }

    private static boolean isPermissionGranted(Context context, String permission) {
        final int result = context.checkCallingOrSelfPermission(permission);
        return (result == PackageManager.PERMISSION_GRANTED);
    }

}
