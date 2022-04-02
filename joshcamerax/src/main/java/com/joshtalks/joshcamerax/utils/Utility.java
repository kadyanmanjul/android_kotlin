package com.joshtalks.joshcamerax.utils;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;



public class Utility {

  public static void setupStatusBarHidden(AppCompatActivity appCompatActivity) {
    Window w = appCompatActivity.getWindow();
    w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
  }

  public static void hideStatusBar(AppCompatActivity appCompatActivity) {
    synchronized (appCompatActivity) {
      Window w = appCompatActivity.getWindow();
      View decorView = w.getDecorView();
      // Hide Status Bar.
      int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
      decorView.setSystemUiVisibility(uiOptions);
    }
  }

}
