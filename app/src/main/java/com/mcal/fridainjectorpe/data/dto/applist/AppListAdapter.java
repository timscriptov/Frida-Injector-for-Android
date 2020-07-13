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
package com.mcal.fridainjectorpe.data.dto.applist;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.List;

public class AppListAdapter extends ArrayAdapter<PackageInfoHolder> {

    public AppListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public AppListAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public AppListAdapter(@NonNull Context context, int resource, @NonNull PackageInfoHolder[] objects) {
        super(context, resource, objects);
    }

    public AppListAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull PackageInfoHolder[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public AppListAdapter(@NonNull Context context, int resource, @NonNull List<PackageInfoHolder> objects) {
        super(context, resource, objects);
    }

    public AppListAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<PackageInfoHolder> objects) {
        super(context, resource, textViewResourceId, objects);
    }
}