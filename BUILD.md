# How to build

## Prerequisites(Windows)

* Msys2
* mingw64
* cmake
* ninja

### Build procedure

```
sh native/build.sh
cd ..
gradlew assemble
```

* NOTE: If you are building on windows, you need to use MSYS2 MinGW x64.

* NOTE: If you are building java-android, you need to set sdk.dir in local.properties.

Generated files:

```
./quack-android/build/outputs/aar/quack-android-debug.aar
./quack-android/build/outputs/aar/quack-android-release.aar
./quack-java/build/libs/quack-java-sources.jar
./quack-java/build/libs/quack-java.jar
./quack-jni/build/libs/quack-jni.jar
```

# How to use

## Gradle 

### repositories

Add the following to the repository information of gradle:

```
    repositories {
        google()
        mavenCentral()
        maven {
            url "https://uk-taniyama.github.io/maven/repository"
        }
    }
```

### Dependencies

for Libray:

```
    implementation "com.github.uk-taniyama.quack:quack-java:${versions.quack}"
    testImplementation "com.github.uk-taniyama.quack:quack-jni:${versions.quack}"
```

for Windows, Linux, MacOS:

```
    implementation "com.github.uk-taniyama.quack:quack-jni:${versions.quack}"
```

for Android:

```
    implementation "com.github.uk-taniyama.quack:quack-java:${versions.quack}"
    testImplementation "com.github.uk-taniyama.quack:quack-jni:${versions.quack}"
```
