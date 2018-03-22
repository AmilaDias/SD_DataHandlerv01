#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_sd2018_sd_1datahandlerv01_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
