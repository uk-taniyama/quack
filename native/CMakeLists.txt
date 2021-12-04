cmake_minimum_required(VERSION 3.4.1)

add_definitions(-DCONFIG_VERSION="2021-03-27")

set(CMAKE_CXX_STANDARD 11)
set(BUILD_SHARED_LIBS ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

project(quickjs)

find_package(JNI REQUIRED)

message(STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
message(STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
message(STATUS "JAVA_INCLUDE_PATH=${JAVA_INCLUDE_PATH}")
message(STATUS "JAVA_INCLUDE_PATH2=${JAVA_INCLUDE_PATH2}")

include_directories(${JAVA_INCLUDE_PATH})
include_directories(${JAVA_INCLUDE_PATH2})

if(APPLE)
  add_definitions(-DCONFIG_BIGNUM)
  set(CMAKE_CXX_FLAGS "-Wextra -Wno-sign-compare -Wno-missing-field-initializers -Wundef -Wuninitialized -Wunused -Wno-unused-parameter -Wwrite-strings -Wchar-subscripts -funsigned-char")
  set(CMAKE_C_FLAGS "-Wextra -Wno-sign-compare -Wno-missing-field-initializers -Wundef -Wuninitialized -Wunused -Wno-unused-parameter -Wwrite-strings -Wchar-subscripts -funsigned-char")
else()
  add_definitions(-D_GNU_SOURCE)
  add_definitions(-DCONFIG_BIGNUM)
  set(CMAKE_CXX_FLAGS "-fpermissive -Wno-array-bounds -Wno-format-truncation")
  set(CMAKE_C_FLAGS "-Wno-array-bounds -Wno-format-truncation")
endif()

if(WIN32)
  set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static")
endif()

file(GLOB quickjs_SRC
  "quickjs/*.h"
  "quickjs/*.cpp"
  "quickjs/quickjs.c"
  "quickjs/libbf.c"
  "quickjs/quickjs-libc.c"
  "quickjs/libunicode.c"
  "quickjs/libregexp.c"
  "quickjs/cutils.c"
  "quickjs/quickjs-debugger.c"
)

file(GLOB quickjs_WIN_SRC
  "quickjs/quickjs-debugger-transport-win.c"
)

file(GLOB quickjs_NIX_SRC
  "quickjs/quickjs-debugger-transport-unix.c"
)

if(WIN32)
  add_definitions(-DWIN32_LEAN_AND_MEAN)
  add_definitions(-D_WIN32_WINNT=0x0600)
endif()

if(WIN32)
  add_library(quickjs SHARED
                    ${quickjs_SRC} ${quickjs_WIN_SRC})
else()
  add_library(quickjs SHARED
                    ${quickjs_SRC} ${quickjs_NIX_SRC})
endif()

if (CMAKE_BUILD_TYPE MATCHES Release)
  set_target_properties(quickjs PROPERTIES LINK_FLAGS_RELEASE -s)
endif()

if(WIN32)
  set_target_properties(quickjs PROPERTIES PREFIX "")
  target_link_libraries(quickjs -static-libstdc++ -static-libgcc pthread ws2_32)
else()
  target_link_libraries(quickjs m dl pthread)
endif()

set_target_properties(quickjs
          PROPERTIES
          RUNTIME_OUTPUT_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/../quack-jni/src/main/resources/META-INF")