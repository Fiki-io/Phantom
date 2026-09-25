cmake_minimum_required(VERSION 3.22.1)

project("phantom_core")

# Size optimization flags for Release builds
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os -fno-exceptions -fno-rtti -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} -Wl,--gc-sections -Wl,--strip-all")

add_library(
    phantom_core
    SHARED
    phantom_core.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    phantom_core
    ${log-lib}
)
