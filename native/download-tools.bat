@echo off

mkdir archive 2> NUL

@rem download mingw64 9.2

if not exist "MinGW" (
  if not exist "archive\mingw-17.1-without-git.exe" (
    curl -L "https://nuwen.net/files/mingw/mingw-17.1-without-git.exe" --output archive\mingw-17.1-without-git.exe
  )
  7z x archive\mingw-17.1-without-git.exe
)

@rem download cmake 3.17.3

if not exist "cmake" (
  if not exist "archive\cmake-3.21.0-windows-x86_64.zip" (
    curl -L "https://github.com/Kitware/CMake/releases/download/v3.21.0/cmake-3.21.0-windows-x86_64.zip" --output archive\cmake-3.21.0-windows-x86_64.zip
  )
  tar xf archive\cmake-3.21.0-windows-x86_64.zip
  rename cmake-3.21.0-windows-x86_64 cmake
)

@rem download ninja 1.10.0

if not exist "ninja" (
  mkdir ninja 2> NUL
  if not exist "archive\ninja-win.zip" (
    curl -L "https://github.com/ninja-build/ninja/releases/download/v1.10.2/ninja-win.zip" --output archive\ninja-win-1.10.2.zip
  )
  tar xf archive\ninja-win-1.10.2.zip -C ninja
)