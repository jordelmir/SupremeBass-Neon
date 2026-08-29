cmake_minimum_required(VERSION 3.22.1)
project("supremeacoustics" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(supreme_dsp SHARED
    native_dsp.cpp
    jni_bridge.cpp
)

target_include_directories(supreme_dsp PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(supreme_dsp
    oboe::oboe
    android
    log
)

target_compile_options(supreme_dsp PRIVATE
    -Wall
    -Wextra
    -O3
    -ffast-math
)
