#include <jni.h>
#include <vector>
#include "renderer/GLESRenderer.h"

static ariane::GLESRenderer g_renderer;

extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeCreate(JNIEnv*, jobject) {
    g_renderer.initialize();
}

extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeDraw(
    JNIEnv*, jobject, jint width, jint height) {
    g_renderer.draw(width,height);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeDestroy(JNIEnv*, jobject) {
    g_renderer.shutdown();
}

extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeSetPoints(
    JNIEnv* env, jobject, jfloatArray data) {
    if (!data) return;
    jsize n=env->GetArrayLength(data);
    std::vector<float> values(static_cast<size_t>(n));
    if (n>0) env->GetFloatArrayRegion(data,0,n,values.data());
    g_renderer.setPoints(values);
}

extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeSetCamera(
    JNIEnv*, jobject, jfloat x, jfloat y, jfloat zoom) {
    g_renderer.setCamera(x,y,zoom);
}
