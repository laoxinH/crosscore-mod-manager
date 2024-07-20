cmake_minimum_required(VERSION 3.14)

set(MODULE_NAME ABRecompressor)

project(${MODULE_NAME})

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED True)

set(C_FLAGS "${C_FLAGS} -O2 -fvisibility=hidden")
set(LINKER_FLAGS "${LINKER_FLAGS} -Wl,-exclude-libs,ALL -Wl,--gc-sections -Wl,--strip-all")

include_directories(include)

include(ExternalProject)

find_package(lz4)
if(NOT LZ4_FOUND)
    message("Will attempt to fetch and build lz4")
    message("===================================")
    ExternalProject_Add(
            lz4
            PREFIX "${CMAKE_BINARY_DIR}/lz4"
            GIT_REPOSITORY https://github.com/lz4/lz4.git
            GIT_TAG v1.9.4
            UPDATE_COMMAND ""
            PATCH_COMMAND 
                ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/patches/lz4_CMakeLists.txt <SOURCE_DIR>/CMakeLists.txt
                COMMAND ${CMAKE_COMMAND} -E copy <SOURCE_DIR>/build/cmake/lz4Config.cmake.in <SOURCE_DIR>/lz4Config.cmake.in
            CMAKE_ARGS -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK}/build/cmake/android.toolchain.cmake
                -DANDROID_ABI=${ANDROID_ABI}
                -DANDROID_PLATFORM=${ANDROID_PLATFORM}
                -DBUILD_SHARED_LIBS=OFF
            INSTALL_COMMAND ""
    )
    ExternalProject_Get_Property( lz4 SOURCE_DIR BINARY_DIR )
    set( LZ4_INCLUDE_DIR "${SOURCE_DIR}/lib" )
    set( LZ4_STATIC_LIB "${BINARY_DIR}/liblz4.a" )
else()
    message("Found lz4 library: ${LZ4_LIBRARIES}")
    message("====================================")
endif()

find_package(LibLZMA)
if(NOT LIBLZMA_FOUND)
    message("Will attempt to fetch and build liblzma")
    message("=======================================")
    ExternalProject_Add(
            lzma
            PREFIX "${CMAKE_BINARY_DIR}/lzma"
            GIT_REPOSITORY https://github.com/tukaani-project/xz
            GIT_TAG v5.6.2
            UPDATE_COMMAND ""
            CMAKE_ARGS -DCMAKE_TOOLCHAIN_FILE=${ANDROID_NDK}/build/cmake/android.toolchain.cmake
                -DANDROID_ABI=${ANDROID_ABI}
                -DANDROID_PLATFORM=${ANDROID_PLATFORM}
                -DBUILD_SHARED_LIBS=OFF
            INSTALL_COMMAND ""
    )

    ExternalProject_Get_Property(lzma SOURCE_DIR BINARY_DIR)
    set(LZMA_INCLUDE_DIR "${SOURCE_DIR}/src/liblzma/api")
    set(LZMA_STATIC_LIB "${BINARY_DIR}/liblzma.a")
else()
    message("Found liblzma library: ${LIBLZMA_LIBRARIES}")
    message("===========================================")
endif()

add_library(${MODULE_NAME} STATIC 
    src/ABRecompressor.cpp
    src/BlocksAndDirectoryInfo.cpp
    src/BundleFile.cpp
    src/Compression.cpp
    src/Header.cpp)

target_include_directories(${MODULE_NAME} PRIVATE ${LZ4_INCLUDE_DIR} ${LZMA_INCLUDE_DIR})

add_dependencies(${MODULE_NAME} lz4 lzma)

target_compile_definitions(${MODULE_NAME} PRIVATE ABRECOMPRESSOR_EXPORTS)

target_link_libraries(ABRecompressor ${LZ4_STATIC_LIB} ${LZMA_STATIC_LIB})