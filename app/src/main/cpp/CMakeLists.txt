cmake_minimum_required(VERSION 3.4.1)



add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        native-lib.cpp)



find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

#定义变量opencvlibs使后面的命令可以使用定位具体的库文件
set(opencvlibs "E:/AndroidStudioWorkspace/OpenCVFaceDetect/app/src/main/cpp/opencv/libs")
#调用头文件的具体路径
#此处和上边的路径都是OpenCv-android-sdk下载保存路径
include_directories(E:/AndroidStudioWorkspace/OpenCVFaceDetect/app/src/main/cpp/opencv/include)

add_library(libopencv_java3 SHARED IMPORTED)
#如果你引用了其他的so库，关联
set_target_properties(libopencv_java3 PROPERTIES IMPORTED_LOCATION
        "${opencvlibs}/${ANDROID_ABI}/libopencv_java3.so")


target_link_libraries( # Specifies the target library.
        native-lib
        libopencv_java3
        android

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})