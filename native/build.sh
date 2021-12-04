#!/bin/bash
cd `dirname $0`
mkdir build
cd build
outputDir=../../quack-jni/src/main/resources/META-INF
mkdir -p $outputDir

unameS=`uname -s`
if [ $unameS == 'Linux' ]
then
	cmake .. -DCMAKE_C_COMPILER=gcc-9 -DCMAKE_CXX_COMPILER=g++-9 -DCMAKE_BUILD_TYPE=Release
	make
	ldd libquickjs.so
	cp libquickjs.so $outputDir
elif [ $unameS == 'Darwin' ]
then
	cmake .. -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++ -DCMAKE_BUILD_TYPE=Release
	make
	otool -L libquickjs.dylib
	cp libquickjs.dylib $outputDir
else # Windows ???
	cmake -DCMAKE_C_COMPILER=gcc.exe -DCMAKE_CXX_COMPILER=g++.exe -DCMAKE_BUILD_TYPE=Release .. -GNinja -B .
	ninja
fi
