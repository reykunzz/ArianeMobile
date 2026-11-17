#include <jni.h>
#include "renderer/GLESRenderer.h"

static ariane::GLESRenderer g_renderer;
extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeCreate(JNIEnv*, jobject) { g_renderer.initialize(); }
extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeDraw(JNIEnv*, jobject, jint width, jint height) { g_renderer.draw(width, height); }
extern "C" JNIEXPORT void JNICALL
Java_com_ariane_mobile_MainActivity_nativeDestroy(JNIEnv*, jobject) { g_renderer.shutdown(); }
