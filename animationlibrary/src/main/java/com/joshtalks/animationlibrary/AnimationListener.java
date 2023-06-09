package com.joshtalks.animationlibrary;

/**
 * This interface is a custom listener to determine the end of an animation.
 * 
 * @author Phu
 * 
 */
public interface AnimationListener {

	/**
	 * This method is called when the animation ends.
	 * 
	 * @param animation
	 *            The Animation object.
	 */
    void onAnimationEnd(Animation animation);
}

