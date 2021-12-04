@REM @echo off

cd /D "%~dp0"

set ROOT_DIR=%~dp0

endlocal

mkdir build 2> NUL
cd build

cmake -DCMAKE_C_COMPILER=gcc.exe -DCMAKE_CXX_COMPILER=g++.exe -DCMAKE_BUILD_TYPE=Release .. -GNinja -B .
ninja
