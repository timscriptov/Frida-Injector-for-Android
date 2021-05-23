package com.mcal.fridainjectorpe.activities;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.mcal.fridainjectorpe.R;
import com.mcal.fridainjectorpe.view.CenteredToolBar;

public class ExceptionActivity extends AppCompatActivity {

    private CenteredToolBar toolbar;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layoutParams.gravity = 17;
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(layoutParams);
        setContentView(linearLayout);
        toolbar = new CenteredToolBar(this);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Application Error");
        toolbar.setBackgroundColor(Color.parseColor(getString(R.color.colorPrimaryDark)));
        ScrollView sv = new ScrollView(this);
        AppCompatTextView error = new AppCompatTextView(this);
        sv.addView(error);
        linearLayout.addView(toolbar);
        linearLayout.addView(sv);
        error.setText(getIntent().getStringExtra("error"));
    }
}