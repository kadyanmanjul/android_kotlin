
package com.joshtalks.appcamera.utility.ui;


import com.joshtalks.appcamera.pix.JoshCameraActivity;

public interface FastScrollStateChangeListener {

    /**
     * Called when fast scrolling begins
     */
    void onFastScrollStart(JoshCameraActivity fastScroller);

    /**
     * Called when fast scrolling ends
     */
    void onFastScrollStop(JoshCameraActivity fastScroller);
}
