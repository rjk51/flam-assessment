#include <jni.h>
#include <opencv2/opencv.hpp>

extern "C"
JNIEXPORT void JNICALL
Java_com_example_edgedetectionviewer_MainActivity_processFrame(JNIEnv *env, jobject thiz, jlong matAddr) {
    // Reference to the Java-passed Mat (expected to be CV_8UC4 / RGBA)
    cv::Mat &mat = *(cv::Mat *)matAddr;

    if (mat.empty()) {
        return; // nothing to do
    }

    // Use thread-local Mats to avoid reallocating every frame (better for real-time)
    thread_local cv::Mat gray;
    thread_local cv::Mat edges;

    // Ensure gray and edges have the correct size and type; only allocate when needed
    if (gray.empty() || gray.cols != mat.cols || gray.rows != mat.rows || gray.type() != CV_8UC1) {
        gray.create(mat.rows, mat.cols, CV_8UC1);
    }
    if (edges.empty() || edges.cols != mat.cols || edges.rows != mat.rows || edges.type() != CV_8UC1) {
        edges.create(mat.rows, mat.cols, CV_8UC1);
    }

    // Convert RGBA -> Gray
    cv::cvtColor(mat, gray, cv::COLOR_RGBA2GRAY);

    // Gaussian blur (in-place is fine and avoids extra allocation)
    cv::GaussianBlur(gray, gray, cv::Size(5,5), 1.5, 1.5);

    // Added Canny edge detection
    cv::Canny(gray, edges, 100, 200);

    // Convert edges (gray) back to RGBA and store in mat for display (in-place)
    cv::cvtColor(edges, mat, cv::COLOR_GRAY2RGBA);
}
