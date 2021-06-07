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
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
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
import com.mcal.fridainjectorpe.data.Preferences;
import com.mcal.fridainjectorpe.databinding.ActivityMainBinding;
import com.mcal.fridainjectorpe.databinding.DialogGotoBinding;
import com.mcal.fridainjectorpe.editor.TextEditor;
import com.mcal.fridainjectorpe.editor.lang.javascript.JavaScriptLanguage;
import com.mcal.fridainjectorpe.injector.FridaAgent;
import com.mcal.fridainjectorpe.injector.FridaInjector;
import com.mcal.fridainjectorpe.injector.OnMessage;
import com.mcal.fridainjectorpe.model.BaseActivity;
import com.mcal.fridainjectorpe.utils.ExceptionHandler;
import com.mcal.fridainjectorpe.view.AppListDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends BaseActivity implements OnMessage {

    @SuppressLint("StaticFieldLeak")
    public static AppCompatEditText apkPackage;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatImageView apkIcon;
    public TextEditor editor;
    public AppCompatEditText scriptPath;
    public AppCompatImageButton selectApk, openScript;
    public AppCompatButton run;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editor = binding.codeEditor;
        editor.setText(getString(R.string.your_code));
        editor.requestFocus();
        editor.setWordWrap(Preferences.isWordWrap());
        editor.setAutoIndentWidth(4);
        editor.setLanguage(JavaScriptLanguage.getInstance());
        editor.setShowLineNumbers(true);
        editor.setHighlightCurrentRow(true);
        editor.setTabSpaces(4);
        ;

        apkPackage = binding.appPackage;
        scriptPath = binding.scriptPath;
        apkIcon = binding.icon;
        selectApk = binding.select;
        openScript = binding.openScript;
        run = binding.run;

        binding.toolbar.setOnMenuItemClickListener((menu) -> {
            int id = menu.getItemId();
            if (id == R.id.action_undo) {
                editor.undo();
            } else if (id == R.id.action_redo) {
                editor.redo();
            } else if (id == R.id.action_gotoline) {
                DialogGotoBinding gotoBinding = DialogGotoBinding.inflate(getLayoutInflater());
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle(R.string.jump_to_line);
                dialog.setView(gotoBinding.getRoot());
                dialog.setPositiveButton("Ok", (dialog1, which) -> {
                    if (!gotoBinding.numberLine.getText().toString().isEmpty()) {
                        editor.gotoLine(Integer.parseInt(gotoBinding.numberLine.getText().toString()));
                    } else {
                        Toast.makeText(this, "Null!", Toast.LENGTH_SHORT).show();
                    }
                });
                dialog.show();
            } else if (id == R.id.action_settings) {
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
            } else if (id == R.id.action_about) {
                AlertDialog.Builder dialogAbout = new AlertDialog.Builder(MainActivity.this);
                dialogAbout.setTitle(R.string.dialog_about_title);
                dialogAbout.setMessage("Frida Injector - Pocket Edition " + BuildConfig.VERSION_NAME + "\n\nCopyright 2020-2021 Иван Тимашков");
                dialogAbout.setPositiveButton(android.R.string.ok, null);
                dialogAbout.show();
            } else {
                return false;
            }
            return true;
        });

        init();
    }

    public void init() {
        selectApk.setOnClickListener(v -> {
            new AppListDialog(MainActivity.this, apkPackage);
        });
        openScript.setOnClickListener(v -> selectApkFromSdcard());
        run.setOnClickListener(v -> {
            try {
                if (!apkPackage.getText().toString().isEmpty()) {
                    //if (!scriptPath.getText().toString().isEmpty()) {
                    if (!editor.getText().toString().isEmpty()) {
                        // build an instance of FridaInjector providing binaries for arm/arm64/x86/x86_64 as needed
                        // assets/frida-inject-12.10.4-android-arm64
                        FridaInjector fridaInjector = new FridaInjector.Builder(MainActivity.this)
                                .withArmInjector("frida-inject-14.2.18-android-arm")
                                .withArm64Injector("frida-inject-14.2.18-android-arm64")
                                .withX86Injector("frida-inject-14.2.18-android-x86")
                                .withX86_64Injector("frida-inject-14.2.18-android-x86_64")
                                .build();

                        // build an instance of FridaAgent
                        FridaAgent fridaAgent = new FridaAgent.Builder(MainActivity.this)
                                .withAgentFromString(editor.getText().toString())
                                .withOnMessage(MainActivity.this)
                                .build();

                        // register a custom interface
                        fridaAgent.registerInterface("activityInterface", Interfaces.ActivityInterface.class);

                        // inject app

                        fridaInjector.inject(fridaAgent, Preferences.packageName(), true);
                    } else {
                        Toast.makeText(this, "Please enter package name!", Toast.LENGTH_SHORT).show();
                    }
                    //} else {
                    //    Toast.makeText(this, "Null!", Toast.LENGTH_SHORT).show();
                    //}
                } else {
                    Toast.makeText(this, "Please enter code!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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
            editor.setText(new String(bytes, StandardCharsets.UTF_8));
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