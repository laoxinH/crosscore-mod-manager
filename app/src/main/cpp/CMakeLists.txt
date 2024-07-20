cmake_minimum_required(VERSION 3.22.1)

set(MODULE_NAME AssetBundleJNI)

project(${MODULE_NAME})

include(ExternalProject)

set(C_FLAGS "${C_FLAGS} -O2 -fvisibility=hidden -fvisibility-inlines-hidden")
set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all")

include_directories(ABRecompressor/include)

add_subdirectory(ABRecompressor ABRecompressor)

set(CMAKE_CXX_STANDARD 20)

set(CXX_FLAGS "${CXX_FLAGS} -fno-exceptions -fno-rtti")

add_library(${MODULE_NAME} SHARED
        AssetBundleJNI.cpp
)

target_link_libraries(${MODULE_NAME} PRIVATE ABRecompressor)