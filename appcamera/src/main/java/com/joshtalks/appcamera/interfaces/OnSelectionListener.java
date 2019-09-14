package com.joshtalks.appcamera.interfaces;

import android.view.View;

import com.joshtalks.appcamera.modals.Img;



public interface OnSelectionListener {
    void onClick(Img Img, View view, int position);

    void onLongClick(Img img, View view, int position);
}
