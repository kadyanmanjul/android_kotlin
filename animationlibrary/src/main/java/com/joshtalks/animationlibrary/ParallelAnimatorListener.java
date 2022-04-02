package com.joshtalks.animationlibrary;

/**
 * This method is called when the parallel animation ends.
 * 
 * @author SiYao
 * 
 */
public interface ParallelAnimatorListener {

	/**
	 * This method is called when the parallel animation ends.
	 * 
	 * @param parallelAnimator
	 *            The ParallelAnimator object.
	 */
    void onAnimationEnd(ParallelAnimator parallelAnimator);
}