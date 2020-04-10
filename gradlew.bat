apply plugin: 'com.android.application'

android {
    compileSdkVersion 29
    buildToolsVersion "29.0.3"
    defaultConfig {
        applicationId "com.ashd.keystone"
        minSdkVersion 21
        targetSdkVersion 29
        versionCode 100
        versionName "1.0.0"
        flavorDimensions "1.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        releaseSign {
            keyAlias 'aishang.keystore'
            keyPassword 'rswzjq!@#$'
            storeFile file('D:\\GitWorkspace\\AshdKeystone\\keystore\\rk7.1_rger.jks')
            storePassword 'rswzjq!@#$'
        }
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            minifyEnabled false
            signingConfig signingConfigs.releaseSign
        }
    }

    productFlavors {
        ashd {}
    }

    android.applicationVariants.all { variant ->
        variant.outputs.all {
            outputFileName = "AshdKeystone_${variant.productFlavors[0].name}_c${defaultConfig.versionCode}_v${defaultConfig.versionName}_${buildType.name}_${releaseTime()}.apk"
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    lintOptions {
        checkReleaseBuilds false
        // Or, if you prefer, you can continue to check for errors in release builds,
        // but continue the build even when errors are found:
        abortOnError false
    }
}

def releaseTime() {
    return new Date().format("yyyyMMdd-HHmmss", TimeZone.getDefault())
}

dependencies {
//    implementation fileTree(dir: 'libs', include: ['*.jar'])
    compileOnly files("libs/layoutlib.jar")
    implementation 'androidx.appcompat:appcompat:1.1.0'
    implementation 'androidx.constraintlayout:constraintlayout:1.1.3'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'androidx.test:runner:1.1.1'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.2.0'

    implementation 'com.nostra13.u