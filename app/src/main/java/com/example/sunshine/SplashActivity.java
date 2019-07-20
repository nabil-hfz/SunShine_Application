package com.example.sunshine;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import gr.net.maroulis.library.EasySplashScreen;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasySplashScreen config = new EasySplashScreen(SplashActivity.this)
                .withFullScreen()
                .withTargetActivity(MainActivity.class)
                .withSplashTimeOut(3000)
                .withBackgroundResource(R.color.white)
                .withFooterText("All rights reserved")
                .withLogo(R.mipmap.ic_launcher)
                .withAfterLogoText("Sunshine App");

        config.getAfterLogoTextView().setTextColor(getResources().getColor(R.color.primary_text));
        config.getLogo().setMinimumWidth(120);
        config.getLogo().setMinimumHeight(120);
        config.getFooterTextView().setTextColor(getResources().getColor(R.color.primary_text));

        //set your own animations
        myCustomTextViewAnimation(config.getFooterTextView());

        View easySplashScreenView = config.create();

        setContentView(easySplashScreenView);
    }

    private void myCustomTextViewAnimation(TextView tv) {
        Animation animation = new TranslateAnimation(0, 0, 480, 0);
        animation.setDuration(900);
        tv.startAnimation(animation);
    }
}
