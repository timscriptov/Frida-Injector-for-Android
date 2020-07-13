/*
 * Copyright (C) 2020 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mcal.fridainjectorpe.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;

import com.mcal.filepicker.model.DialogConfigs;
import com.mcal.filepicker.model.DialogProperties;
import com.mcal.filepicker.view.FilePickerDialog;
import com.mcal.fridainjectorpe.BuildConfig;
import com.mcal.fridainjectorpe.R;
import com.mcal.fridainjectorpe.editor.TextEditor;
import com.mcal.fridainjectorpe.injector.FridaAgent;
import com.mcal.fridainjectorpe.injector.FridaInjector;
import com.mcal.fridainjectorpe.injector.OnMessage;
import com.mcal.fridainjectorpe.model.BaseActivity;
import com.mcal.fridainjectorpe.view.AppListDialog;
import com.mcal.fridainjectorpe.view.CenteredToolBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends BaseActivity implements OnMessage {

    private CenteredToolBar toolbar;
    public TextEditor mEditor;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatEditText apkPackage;
    public AppCompatEditText scriptPath;
    public AppCompatImageButton selectApk, openScript;
    public AppCompatButton run;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatImageView apkIcon;
    private static SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar(getString(R.string.app_name));
        mEditor = findViewById(R.id.edit_code);
        mEditor.setText(getString(R.string.your_code));
        mEditor.requestFocus();
        apkPackage = findViewById(R.id.app_package);
        scriptPath = findViewById(R.id.script_path);
        apkIcon = findViewById(R.id.icon);
        selectApk = findViewById(R.id.select);
        openScript = findViewById(R.id.open_script);
        run = findViewById(R.id.run);
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo:
                mEditor.undo();
                break;
            case R.id.action_redo:
                mEditor.redo();
                break;
            case R.id.action_gotoline:
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LinearLayout ll = new LinearLayout(this);
                ll.setOrientation(LinearLayout.VERTICAL);
                ll.setPadding(40, 0, 40, 0);
                ll.setLayoutParams(layoutParams);
                final AppCompatEditText acet0 = new AppCompatEditText(this);
                acet0.setText("");
                acet0.setHint(R.string.enter_number_line);
                ll.addView(acet0);

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle(R.string.jump_to_line);
                dialog.setView(ll);
                dialog.setPositiveButton("Ok", (dialog1, which) -> {
                    if (!acet0.getText().toString().isEmpty()) {
                        mEditor.gotoLine(Integer.parseInt(acet0.getText().toString()));
                    } else {
                        Toast.makeText(this, "Null!", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
                break;
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                break;
            case R.id.action_about:
                AlertDialog.Builder dialogAbout = new AlertDialog.Builder(MainActivity.this);
                dialogAbout.setTitle(R.string.dialog_about_title);
                dialogAbout.setMessage("Frida Injector - Pocket Edition " + BuildConfig.VERSION_NAME + "\n\nCopyright 2020 Иван Тимашков");
                dialogAbout.setPositiveButton(android.R.string.ok, null);
                dialogAbout.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void init() {
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        selectApk.setOnClickListener(v -> {
            new AppListDialog(MainActivity.this, apkPackage);
        });
        openScript.setOnClickListener(v -> selectApkFromSdcard());
        run.setOnClickListener(v -> {
            try {
                // build an instance of FridaInjector providing binaries for arm/arm64/x86/x86_64 as needed
                // assets/frida-inject-12.10.4-android-arm64
                FridaInjector fridaInjector = new FridaInjector.Builder(MainActivity.this)
                        .withArmInjector("frida-inject-12.10.4-android-arm")
                        .withArm64Injector("frida-inject-12.10.4-android-arm64")
                        .withX86Injector("frida-inject-12.10.4-android-x86")
                        .withX86_64Injector("frida-inject-12.10.4-android-x86_64")
                        .build();

                // build an instance of FridaAgent
                FridaAgent fridaAgent = new FridaAgent.Builder(MainActivity.this)
                        .withAgentFromString(mEditor.getText().toString())
                        .withOnMessage(MainActivity.this)
                        .build();

                // register a custom interface
                fridaAgent.registerInterface("activityInterface", Interfaces.ActivityInterface.class);

                // inject app
                fridaInjector.inject(fridaAgent, sp.getString("apk_package_name", ""), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void setupToolbar(String title) {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
    }

    private void selectApkFromSdcard() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        properties.extensions = new String[]{".js", ".JS"};
        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties, R.style.AlertDialogTheme);
        dialog.setTitle(getString(R.string.select_script));
        dialog.setPositiveBtnName(getString(R.string.select));
        dialog.setNegativeBtnName(getString(android.R.string.cancel));
        dialog.setDialogSelectionListener(files -> {
            for (String path : files) {
                File file = new File(path);
                if (file.getName().endsWith(".js") || file.getName().endsWith(".JS")) {
                    scriptPath.setText(file.getAbsolutePath());
                    openFile(file.getAbsolutePath());
                }
            }
        });
        dialog.show();
    }

    private void openFile(String filePath) {
        try {
            InputStream is = new FileInputStream(new File(filePath));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[65535];
            for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
                os.write(buffer, 0, len);
            }
            byte[] bytes = os.toByteArray();
            mEditor.setText(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(String data) {
        try {
            JSONObject object = new JSONObject(data);
            Log.e("FridaAndroidInject", "app pid: " + object.getString("pid"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}