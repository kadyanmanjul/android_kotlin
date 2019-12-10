package com.joshtalks.joshskills.core.videotranscoder.compat;

import android.media.MediaCodec;
import android.os.Build;

import java.nio.ByteBuffer;

/**
 * A Wrapper to MediaCodec that facilitates the use of API-dependent get{Input/Output}Buffer methods,
 * in order to prevent: http://stackoverflow.com/q/30646885
 */
public class MediaCodecBufferCompatWrapper {

    final MediaCodec mMediaCodec;
    final ByteBuffer[] mInputBuffers;
    final ByteBuffer[] mOutputBuffers;

    public MediaCodecBufferCompatWrapper(MediaCodec mediaCodec) {
        mMediaCodec = mediaCodec;

        mInputBuffers = mOutputBuffers = null;
    }

    public ByteBuffer getInputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mMediaCodec.getInputBuffer(index);
        }
        assert mInputBuffers != null;
        return mInputBuffers[index];
    }

    public ByteBuffer getOutputBuffer(final int index) {
        if (Build.VERSION.SDK_INT >= 21) {
            return mMediaCodec.getOutputBuffer(index);
        }
        assert mOutputBuffers != null;
        return mOutputBuffers[index];
    }
}
