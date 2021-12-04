# How to build

## for Windows

### Prerequisites

* Msys2
* mingw64
* cmake
* ninja

### Build procedure

at MSYS2 MinGW x64:

```
native/build.bat
cd ..
gradlew assemble
```

NOTE: If you are building java-android, you need to set sdk.dir in local.properties.

Generated files:

```
./quack-android/build/outputs/aar/quack-android-debug.aar
./quack-android/build/outputs/aar/quack-android-release.aar
./quack-java/build/libs/quack-java-sources.jar
./quack-java/build/libs/quack-java.jar
./quack-jni/build/libs/quack-jni.jar
```
