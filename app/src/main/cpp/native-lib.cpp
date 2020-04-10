#include <jni.h>
#include <string>
#include <iostream>
#include <opencv2/opencv.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgcodecs.hpp>
#include <android/native_window_jni.h>
#include <android/log.h>

#define TAG "JNITEST"
// 定义info信息
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
// 定义debug信息
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
// 定义error信息
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)

using namespace cv;
using namespace std;
ANativeWindow *window = 0;

class CascadeDetectorAdapter : public DetectionBasedTracker::IDetector {
public:
    CascadeDetectorAdapter(cv::Ptr<cv::CascadeClassifier> detector) :
            IDetector(),
            Detector(detector) {
        CV_Assert(detector);
    }

    void detect(const cv::Mat &Image, vector<cv::Rect> &objects) {
        Detector->detectMultiScale(Image, objects, scaleFactor, minNeighbours, 0, minObjSize,
                                   maxObjSize);
    }

    virtual ~CascadeDetectorAdapter() {

    }

private:
    CascadeDetectorAdapter();

    cv::Ptr<cv::CascadeClassifier> Detector;
};

DetectionBasedTracker *tracker = 0;

extern "C" JNIEXPORT jstring JNICALL
Java_com_wangxingxing_opencv_1face_1detect_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    string txt1 = "OpenCV version: ";
    string cv_version = CV_VERSION;

    string info;
    info = txt1;
    info += cv_version;
    return env->NewStringUTF(info.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxingxing_opencv_1face_1detect_MainActivity_init(JNIEnv *env, jobject thiz,
                                                             jstring _model) {
    LOGI("_init :%s", "==============begin===================");
    const char *model = env->GetStringUTFChars(_model, 0);
    if (model == NULL) {
        return;
    }

    if (tracker) {
        //内存泄漏
        tracker->stop();
        delete tracker;
        tracker = 0;
    }

    //1.makePtr 创建CascadeClassifier
    Ptr<CascadeClassifier> classifier = makePtr<CascadeClassifier>(model);
    //创建一个跟踪适配器
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(classifier);
    Ptr<CascadeClassifier> classifier1 = makePtr<CascadeClassifier>(model);
    //创建一个跟踪适配器
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(classifier1);
    //拿去用的跟踪器
    DetectionBasedTracker::Parameters DetectorParams;
    tracker = new DetectionBasedTracker(mainDetector, trackingDetector, DetectorParams);
    //开启跟踪器
    tracker->run();

    env->ReleaseStringUTFChars(_model, model);
    LOGI("_init :%s", "============ok=====================");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxingxing_opencv_1face_1detect_MainActivity_setSurface(JNIEnv *env, jobject thiz,
                                                                   jobject surface) {
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }
    window = ANativeWindow_fromSurface(env, surface);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxingxing_opencv_1face_1detect_MainActivity_postData(JNIEnv *env, jobject thiz,
                                                                 jbyteArray _data, jint width,
                                                                 jint height, jint camera_id) {
    //传过来的数据都是nv21的数据，而OpenCv是在Mat中处理的
    jbyte *data = env->GetByteArrayElements(_data, NULL);
    //所以将data数据添加到Mat中
    //1.高(nv21模型转换) 2.宽
    Mat src(height + height / 2, width, CV_8UC1, data);
    //颜色格式转换 nv21 转成 RGBA
    //将nv21的yuv数据转成rgba
    cvtColor(src, src, COLOR_YUV2RGBA_NV21);
    //如果正在写的过程中退出，导致文件丢失

    if (camera_id == 1) {
        //前置摄像头，需要逆时针旋转90度
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        //水平翻转 镜像,1水平 0为垂直
        flip(src, src, 1);
    } else {
        //顺时针旋转90度
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }
    Mat gray;
    //灰色
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    //增强对比度 (直方图均衡)
    equalizeHist(gray, gray);
    //优化
    vector<Rect> faces;
    //定位人脸 N个
    tracker->process(gray);
    //处理摄像头采集后的数据（Mat 灰度）
    tracker->getObjects(faces);
    for (int i = 0; i < faces.size(); i++) {
        //内存里面
        rectangle(src, faces.at(i), Scalar(255, 0, 255));
    }

/*    下面的代码不能展开（因为16 loop）
    for (Rect face : faces) {
        //画矩形
        //分别指定 bgra
        rectangle(src, face, Scalar(255, 0, 255)); //内存里面
    }*/

    if (window) {
        //设置windows的属性
        // 因为旋转了 所以宽、高需要交换
        //这里使用 cols 和rows 代表 宽、高 就不用关心上面是否旋转了
        ANativeWindow_setBuffersGeometry(window, src.cols, src.rows, WINDOW_FORMAT_RGBA_8888);
        ANativeWindow_Buffer buffer;
        do {
            //lock失败 直接break出去
            if (ANativeWindow_lock(window, &buffer, 0)) {
                ANativeWindow_release(window);
                window = 0;
                break;
            }

            //src.data ： rgba的数据
            //把src.data 一行一行的拷贝到 buffer.bits 里去
            //填充rgb数据给dst_data
            uint8_t *dst_data = static_cast<uint8_t *>(buffer.bits);
            //stride : 一行多少个数据 （RGBA） * 4
            int dst_line_size = buffer.stride * 4;

            //一行一行拷贝
            for (int i =0; i < buffer.height; ++i) {
                //void *memcpy(void *dest, const void *src, size_t n);
                //从源src所指的内存地址的起始位置开始拷贝n个字节到目标dest所指的内存地址的起始位置中
                memcpy(dst_data + i * dst_line_size,
                       src.data + i * src.cols * 4, dst_line_size);
            }

            //提交刷新
            ANativeWindow_unlockAndPost(window);
        } while (0);
    }
    //释放Mat
    //内部采用的 引用计数
    src.release();
    gray.release();
    env->ReleaseByteArrayElements(_data, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxingxing_opencv_1face_1detect_MainActivity_faceCollect(JNIEnv *env, jobject thiz,
                                                                    jbyteArray _data, jint width,
                                                                    jint height, jint camera_id) {
    jbyte *data = env->GetByteArrayElements(_data, NULL);
    Mat src(height + height / 2, width, CV_8UC1, data);
    cvtColor(src, src, COLOR_YUV2RGBA_NV21);
    if (camera_id == 1) {
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        flip(src, src, 1);
    } else {
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }
    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    equalizeHist(gray, gray);
    std::vector<Rect> faces;
    tracker->process(gray);
    tracker->getObjects(faces);

    // for (Rect face : faces) {

    for(int i =0;i<faces.size();i++){
        LOGI("the string is :%s","Rect");
        rectangle(src, faces.at(i), Scalar(255, 0, 255));

//        //将截取到的人脸保存到sdcard
//        Mat m;
//        //把img中的人脸部位拷到m中
//        src(faces.at(i)).copyTo(m);
//        //把人脸从新定义为24*24的大小图片
//        resize(m, m, Size(24, 24));
//        //置灰
//        cvtColor(m, m, COLOR_BGR2GRAY);
//        char p[100];
//        sprintf(p, "/sdcard/info/%d.jpg", index++);
//        //把mat写出为jpg文件
//        //这里可以控制一下数量
//        imwrite(p, m);
    }

    if (window) {
        ANativeWindow_setBuffersGeometry(window, src.cols,
                                         src.rows, WINDOW_FORMAT_RGBA_8888);
        ANativeWindow_Buffer buffer;
        do {
            if (ANativeWindow_lock(window, &buffer, 0)) {
                ANativeWindow_release(window);
                window = 0;
                break;
            }

            uint8_t *dst_data = static_cast<uint8_t *>(buffer.bits);
            int dst_line_size = buffer.stride * 4;
            for (int i = 0; i < buffer.height; ++i) {
                memcpy(dst_data + i * dst_line_size,
                       src.data + i * src.cols * 4, dst_line_size);
            }
            ANativeWindow_unlockAndPost(window);
        } while (0);
    }

    src.release();
    gray.release();

    env->ReleaseByteArrayElements(_data, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_wangxingxing_opencv_1face_1detect_MainActivity_release(JNIEnv *env, jobject thiz) {
    if (tracker) {
        tracker->stop();
        delete tracker;
        tracker = 0;
    }
}