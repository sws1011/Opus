#include <jni.h>
#include <string>
#include <opus.h>
#include "_android_log_print.h"


extern "C"
JNIEXPORT jlong JNICALL
Java_com_opus_OpusUtil_createEncoder(JNIEnv *env, jclass type, jint sampleRateHz, jint channel,
                                            jint complexity) {
    int error;
    OpusEncoder *opusEncoder = opus_encoder_create(sampleRateHz, channel, OPUS_APPLICATION_RESTRICTED_LOWDELAY, &error);

    if (error == OPUS_OK) {
        opus_encoder_ctl(opusEncoder, OPUS_SET_COMPLEXITY(complexity));
        opus_encoder_ctl(opusEncoder, OPUS_SET_VBR(0));
        opus_encoder_ctl(opusEncoder, OPUS_SET_VBR_CONSTRAINT(true));
        opus_encoder_ctl(opusEncoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
        opus_encoder_ctl(opusEncoder, OPUS_SET_LSB_DEPTH(16));
        opus_encoder_ctl(opusEncoder, OPUS_SET_DTX(0));
        opus_encoder_ctl(opusEncoder, OPUS_SET_INBAND_FEC(0));
        opus_encoder_ctl(opusEncoder, OPUS_SET_BITRATE(16000));
        opus_encoder_ctl(opusEncoder, OPUS_SET_PACKET_LOSS_PERC(0));
        return reinterpret_cast<jlong>(opusEncoder);
    }
    return -1;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_opus_OpusUtil_createDecoder(JNIEnv *env, jclass type, jint sampleRateHz, jint channel) {

    int error;

    OpusDecoder *opusDecoder = opus_decoder_create(sampleRateHz, channel, &error);

    if (OPUS_OK == error) {
        return reinterpret_cast<jlong>(opusDecoder);
    }
    return -1;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_opus_OpusUtil_destroyEncoder(JNIEnv *env, jclass type, jlong handle) {
    OpusEncoder *opusEncoder = (OpusEncoder *) handle;
    if (handle <= 0) {
        return;
    }
    opus_encoder_destroy(opusEncoder);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_opus_OpusUtil_destroyDecoder(JNIEnv *env, jclass type, jlong handle) {
    OpusDecoder *opusDecoder = (OpusDecoder *) handle;
    if (handle <= 0) {
        return;
    }
    opus_decoder_destroy(opusDecoder);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_opus_OpusUtil_encode(JNIEnv *env, jclass type, jlong handle, jshortArray in_, jint offset,
                                     jbyteArray out_) {
    jshort *in = env->GetShortArrayElements(in_, NULL);
    jsize inLen = env->GetArrayLength(in_);

    jbyte *out = env->GetByteArrayElements(out_, NULL);

    jsize outLen = env->GetArrayLength(out_);

    OpusEncoder *opusEncoder = (OpusEncoder *) handle;

    if (handle <= 0 || !out || !in) {
        LOGD("初始化数据错误---------");
        return -1;
    }
//    int bitrate;
//    opus_encoder_ctl(opusEncoder, OPUS_GET_BITRATE(&bitrate));
//    int size = opus_packet_get_samples_per_frame(reinterpret_cast<const unsigned char *>(out), 8000);
//    LOGD("opus_encoder_get_size = %d---------", bitrate);

    int encode = opus_encode(opusEncoder, in, inLen, reinterpret_cast<unsigned char *>(out), outLen);

    env->ReleaseShortArrayElements(in_, in, 0);
    env->ReleaseByteArrayElements(out_, out, 0);

    return encode;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_opus_OpusUtil_decode(JNIEnv *env, jclass type, jlong handle, jbyteArray encode_, jshortArray out_) {
    jbyte *encode = env->GetByteArrayElements(encode_, NULL);
    jshort *out = env->GetShortArrayElements(out_, NULL);

    jsize encodeLen = env->GetArrayLength(encode_);
    jsize outLen = env->GetArrayLength(out_);

    OpusDecoder *opusDecoder = (OpusDecoder *) handle;
    if (handle <= 0 || !encode || !out)
        return -1;
    if (encodeLen <= 0 || outLen <= 0) {
        return -1;
    }

    int decode = opus_decode(opusDecoder, reinterpret_cast<const unsigned char *>(encode), encodeLen, out, outLen, 0);

    env->ReleaseByteArrayElements(encode_, encode, 0);
    env->ReleaseShortArrayElements(out_, out, 0);
    return decode;
}