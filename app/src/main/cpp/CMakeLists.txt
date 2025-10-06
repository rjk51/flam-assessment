cmake_minimum_required(VERSION 3.10.2)
project("edgedetectionviewer")

add_library(native-lib SHARED native-lib.cpp)

set(OpenCV_DIR "C:/opencv/OpenCV-android-sdk/sdk/native/jni/abi-x86")

find_package(OpenCV REQUIRED)
include_directories(${OpenCV_INCLUDE_DIRS})

target_link_libraries(native-lib ${OpenCV_LIBS} log)
