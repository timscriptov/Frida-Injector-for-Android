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
package com.mcal.fridainjectorpe.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatEditText;

import com.mcal.fridainjectorpe.R;
import com.mcal.fridainjectorpe.async.ApplicationLoader;
import com.mcal.fridainjectorpe.data.Preferences;
import com.mcal.fridainjectorpe.data.dto.applist.AppInteractor;
import com.mcal.fridainjectorpe.data.dto.applist.PackageInfoHolder;
import com.mcal.fridainjectorpe.data.dto.applist.ViewHolder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class AppListDialog implements AppInteractor {
    private Context mContext;
    private AlertDialog adx;
    private AppCompatEditText mEdit;

    public AppListDialog(Context c, AppCompatEditText e) {
        mContext = c;
        mEdit = e;
        ApplicationLoader runner = new ApplicationLoader(mContext, this);
        runner.execute();
    }

    @Override
    public void setup(ArrayList<PackageInfoHolder> packageInfoHolders) {
        final ArrayAdapter<PackageInfoHolder> aa = new ArrayAdapter<PackageInfoHolder>(mContext, R.layout.package_list_item, packageInfoHolders) {
            @NotNull
            @SuppressLint({"InflateParams", "SetTextI18n"})
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.package_list_item, null);
                }

                PackageInfoHolder pkg = getItem(position);

                ViewHolder holder = new ViewHolder();

                holder.packageLabel = convertView.findViewById(R.id.pkg_name);
                holder.packageName = convertView.findViewById(R.id.pkg_id);
                holder.packageVersion = convertView.findViewById(R.id.pkg_version);
                holder.packageFilePath = convertView.findViewById(R.id.pkg_dir);
                holder.packageIcon = convertView.findViewById(R.id.pkg_img);
                holder.position = position;

                convertView.setTag(holder);

                holder.packageLabel.setText(pkg.packageLabel);
                holder.packageName.setText(pkg.packageName);
                holder.packageVersion.setText("Version " + pkg.packageVersion);
                holder.packageFilePath.setText(pkg.packageFilePath);

                holder.packageIcon.setImageDrawable(pkg.packageIcon);

                return convertView;
            }
        };
        AlertDialog.Builder adb = new AlertDialog.Builder(mContext, R.style.AlertDialogTheme);
        adb.setTitle("Select apk");
        adb.setNegativeButton(android.R.string.cancel, null);
        adb.setAdapter(aa, (p1, p2) -> {
            PackageInfoHolder pkg = aa.getItem(p2);
            mEdit.setText(pkg.packageName);
            Preferences.packageName(pkg.packageName);
            adx.dismiss();
        });
        adb.create();
        adx = adb.show();
    }
}