package com.sws.opus;

/**
 * Created by sws 2019/5/13 14:22
 */
public class OpusUtil {

    static {
        System.loadLibrary("opus-lib");
    }

    private OpusUtil() {

    }

    /**
     * @param sampleRateHz 采样率 8000, 12000, 16000,24000, or 48000.
     * @param channel  number of channels (1 or 2) in input signal
     * @param complexity 0-10
     * @return -1 create fail
     */
    public static native long createEncoder(int sampleRateHz, int channel, int complexity);

    public static native long createDecoder(int sampleRateHz, int channel);

    public static native int encode(long handle, short[] in, int offset, byte[] out);

    public static native int decode(long handle, byte[] encode, short[] out);

    public static native void destroyEncoder(long handle);

    public static native void destroyDecoder(long handle);
}
