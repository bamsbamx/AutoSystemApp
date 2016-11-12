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

package com.anrapps.test.autosystemapp.task;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.anrapps.test.autosystemapp.BuildConfig;
import com.anrapps.test.autosystemapp.R;
import com.stericson.RootShell.RootShell;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeoutException;

public class MoveToSystemTask extends AsyncTask<Void, Void, Void> {

    private static final String[] COMMANDS = new String[] {
            "mount -o remount,rw /system /system", //Make /system writable
            "mkdir -p -m 0755 %s",
            "cp -rp %s %s",
    };

    //Hold WeakReference to Context to avoid memory leaks
    private final WeakReference<Activity> mContextRef;

    private boolean finished, exception, rootDenied;

    public MoveToSystemTask(@NonNull Activity context) {
        this.mContextRef = new WeakReference<>(context);
    }

    @Override
    protected Void doInBackground(Void... params) {

        if (mContextRef.get() == null) {
            exception = true;
            return null;
        }

        final String appName = mContextRef.get().getString(R.string.app_name);
        final String dataApkPath;
        final String systemApkPath = buildSystemApkPath(appName);

        try {
            final String packageName = mContextRef.get().getPackageName();
            final ApplicationInfo applicationInfo = mContextRef.get().getPackageManager().getApplicationInfo(packageName, 0);
            dataApkPath = applicationInfo.sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            exception = true;
            return null;
        }

        try {
            if (new File(systemApkPath).exists()) {
                Log.d(getClass().getName(), "File is already present in system. Skipping...");
                return null; //System app does already exist
            }
            final File file = new File(dataApkPath);
            if (!file.exists()) {
                Log.w(getClass().getName(), "File " + dataApkPath + " does not exist neither. Return");
                exception = true;
                return null;
            }
            COMMANDS[1] = String.format(COMMANDS[1], buildSystemApkFolderPath(appName));
            COMMANDS[2] = String.format(COMMANDS[2], dataApkPath, systemApkPath);

            Command command = new Command(0, COMMANDS) {
                @Override public void commandOutput(int id, String line) {
                    if (BuildConfig.DEBUG) Log.d(getClass().getName(), "commandOutput: " + line);
                    super.commandOutput(id, line);
                    finished = true;
                }
                @Override public void commandTerminated(int id, String reason) {
                    if (BuildConfig.DEBUG) Log.d(getClass().getName(), "commandTerminated: " + reason);
                    finished = true;
                }
                @Override public void commandCompleted(int id, int exitCode) {
                    if (BuildConfig.DEBUG) Log.d(getClass().getName(), "commandCompleted. Exit with code: " + exitCode);
                    finished = true;
                }
            };
            RootShell.getShell(true).add(command);
            RootShell.closeAllShells();
        } catch (TimeoutException | RootDeniedException | IOException e) {
            e.printStackTrace();
            Log.e(getClass().getName(), e.getMessage());
            exception = true;
            rootDenied = e instanceof RootDeniedException;
        }

        //noinspection StatementWithEmptyBody
        while (!finished) {} //Wait until command execution gives a feedback before checking for the system apk file


        if (!new File(systemApkPath).exists()) {
            exception = true;
            Log.w(getClass().getName(), "System APK file does not exist (" + systemApkPath+ "). Problem occurred");
        } else {
            Log.d(getClass().getName(), "System APK file exists. Success");
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if (mContextRef.get() == null) return; //no need to give feedback
        if (!exception)
            Toast.makeText(mContextRef.get(), "Move to system succeeded, now reboot is needed", Toast.LENGTH_SHORT).show();
        else {
            if (rootDenied) Toast.makeText(mContextRef.get(), "Root denied", Toast.LENGTH_SHORT).show();
            else Toast.makeText(mContextRef.get(), "Error accessing root", Toast.LENGTH_SHORT).show();
        }

    }

    private static String buildSystemApkFolderPath(final String appName) {
        return "/system/priv-app/" + appName;
    }

    private static String buildSystemApkPath(final String appName) {
        return buildSystemApkFolderPath(appName) + "/" + appName + ".apk";
    }

}
