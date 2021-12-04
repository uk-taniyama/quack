cmake_minimum_required(VERSION 3.4.1)

add_definitions(-DCONFIG_VERSION="2021-03-27")
add_definitions(-DCONFIG_DISABLE_STACK_CHECK)
add_definitions(-DCONFIG_DISABLE_WORKER)

file(GLOB quickjs_SRC
    "../../../../native/quickjs/*.h"
    "../../../../native/quickjs/*.cpp"
    "../../../../native/quickjs/quickjs.c"
    "../../../../native/quickjs/libbf.c"
    "../../../../native/quickjs/quickjs-libc.c"
    "../../../../native/quickjs/libunicode.c"
    "../../../../native/quickjs/libregexp.c"
    "../../../../native/quickjs/cutils.c"
    "../../../../native/quickjs/quickjs-debugger.c"
    "../../../../native/quickjs/quickjs-debugger-transport-unix.c"
)

add_library(quack SHARED ${quickjs_SRC})

target_link_libraries(quack)
