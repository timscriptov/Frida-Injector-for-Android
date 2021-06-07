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
package com.mcal.fridainjectorpe.model;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.mcal.fridainjectorpe.data.NightMode;
import com.mcal.fridainjectorpe.data.ScreenMode;

import org.jetbrains.annotations.NotNull;

public abstract class BaseActivity extends AppCompatActivity {
    static {
        NightMode.setMode(NightMode.getCurrentMode());
    }

    protected final int REQUEST_CODE_SETTINGS = 0;
    private ScreenMode.Mode screenMode;
    private NightMode.Mode nightMode;
    //private WordWrap.Mode wordWrap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        screenMode = ScreenMode.getCurrentMode();
        nightMode = NightMode.getCurrentMode();
        //wordWrap = WordWrap.getCurrentMode();

        if (screenMode.equals(ScreenMode.Mode.FULLSCREEN)) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && ScreenMode.getCurrentMode().equals(ScreenMode.Mode.FULLSCREEN)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (!screenMode.equals(ScreenMode.getCurrentMode())) recreate();
        if (!nightMode.equals(NightMode.getCurrentMode())) recreate();
        //if (!wordWrap.equals(WordWrap.getCurrentMode())) recreate();

        if (requestCode == REQUEST_CODE_SETTINGS & resultCode == AppCompatActivity.RESULT_OK) {
            recreate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return true;
    }
}