cmake_minimum_required(VERSION 3.22.1)

project("phantom_core")

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
