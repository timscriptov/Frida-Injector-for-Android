package com.mcal.fridainjectorpe.async;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

import com.mcal.fridainjectorpe.data.dto.applist.AppInteractor;
import com.mcal.fridainjectorpe.data.dto.applist.PackageInfoHolder;
import com.mcal.fridainjectorpe.utils.CommonUtils;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ApplicationLoader extends AsyncTask<String, String, ArrayList<PackageInfoHolder>> {
    public WeakReference<Context> context;
    AppInteractor interactor;
    private ProgressDialog packageLoadDialog;

    public ApplicationLoader(Context context, AppInteractor interactor) {
        this.context = new WeakReference<>(context);
        this.interactor = interactor;
    }

    @Override
    protected ArrayList<PackageInfoHolder> doInBackground(String... params) {
        publishProgress("Retrieving installed application");
        return getInstalledApps(context.get());
    }

    @Override
    protected void onPostExecute(ArrayList<PackageInfoHolder> AllPackages) {
        interactor.setup(AllPackages);
        if (!isCancelled()) {
            dismissProgressDialog();
        }
    }

    public void doProgress(String value) {
        publishProgress(value);
    }

    @Override
    protected void onPreExecute() {
        showProgressDialog();
    }

    @Override
    protected void onProgressUpdate(@NotNull String... text) {
        packageLoadDialog.setMessage(text[0]);
    }

    public void showProgressDialog() {
        if (packageLoadDialog == null) {
            packageLoadDialog = new ProgressDialog(context.get());
            packageLoadDialog.setIndeterminate(false);
            packageLoadDialog.setCancelable(false);
            packageLoadDialog.setInverseBackgroundForced(false);
            packageLoadDialog.setCanceledOnTouchOutside(false);
            packageLoadDialog.setMessage("Loading installed applications...");
        }
        packageLoadDialog.show();
    }

    public void dismissProgressDialog() {
        if (packageLoadDialog != null && packageLoadDialog.isShowing()) {
            packageLoadDialog.dismiss();
        }
    }

    public ArrayList<PackageInfoHolder> getInstalledApps(@NotNull Context context) {
        ArrayList<PackageInfoHolder> res = new ArrayList<>();
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);

        int totalPackages = packages.size();

        for (int i = 0; i < totalPackages; i++) {
            PackageInfo p = packages.get(i);
            if (!CommonUtils.isSystemPackage(p)) {
                ApplicationInfo appInfo = null;
                try {
                    appInfo = context.getPackageManager().getApplicationInfo(p.packageName, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                int count = i + 1;

                final PackageInfoHolder newInfo = new PackageInfoHolder();
                newInfo.packageLabel = p.applicationInfo.loadLabel(context.getPackageManager()).toString();

                doProgress("Loading application " + count + " of " + totalPackages + " (" + newInfo.packageLabel + ")");

                newInfo.packageName = p.packageName;
                newInfo.packageVersion = p.versionName;

                if (appInfo != null) {
                    newInfo.packageFilePath = appInfo.publicSourceDir;
                }

                newInfo.packageIcon = p.applicationInfo.loadIcon(context.getPackageManager());
                res.add(newInfo);
            }
        }
        Comparator<PackageInfoHolder> AppNameComparator = (o1, o2) -> o1.getPackageLabel().toLowerCase().compareTo(o2.getPackageLabel().toLowerCase());
        Collections.sort(res, AppNameComparator);
        return res;
    }
}