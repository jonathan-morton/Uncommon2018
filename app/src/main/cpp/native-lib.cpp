#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
Java_com_uncommonhacks_sweetman_yijian_kirtland_morton_uncommon2018_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
