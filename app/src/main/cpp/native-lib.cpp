#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT void JNICALL
Java_com_example_edgedetectionviewer_MainActivity_processFrame(JNIEnv *env, jobject thiz, jlong matAddr) {
    cv::Mat &mat = *(cv::Mat *)matAddr;

    // Example: convert to grayscale
    cv::cvtColor(mat, mat, cv::COLOR_RGBA2GRAY);
}
