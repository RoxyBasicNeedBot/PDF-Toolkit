cmake_minimum_required(VERSION 3.22.1)

project("roxy_magic_clean")

add_library(
    roxy_magic_clean
    SHARED
    roxy_magic_clean.cpp
    roxy_perspective_warp.cpp
)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

target_link_libraries(
    roxy_magic_clean
    ${log-lib}
    ${jnigraphics-lib}
)
