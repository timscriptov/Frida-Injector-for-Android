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
package com.mcal.fridainjectorpe.data;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.mcal.fridainjectorpe.App;

public final class Preferences {
    private static SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(App.getContext());

    public static void apkPath(String path) {
        preferences.edit().putString("apk_path", path).apply();
    }

    public static String packageName() {
        return preferences.getString("apk_package_name", "");
    }

    public static void packageName(String pkg) {
        preferences.edit().putString("apk_package_name", pkg).apply();
    }

    public static void setWordWrap(boolean value) {
        preferences.edit().putBoolean("wordwrap", value).apply();
    }

    public static boolean isWordWrap() {
        return preferences.getBoolean("wordwrap", false);
    }

    public static void setNightMode(boolean value) {
        preferences.edit().putBoolean("night_mode", value).apply();
    }

    public static boolean isNightMode() {
        return preferences.getBoolean("night_mode", false);
    }

    public static void setFullscreenMode(boolean value) {
        preferences.edit().putBoolean("fullscreen_mode", value).apply();
    }

    public static boolean isFullscreenMode() {
        return preferences.getBoolean("fullscreen_mode", false);
    }
}