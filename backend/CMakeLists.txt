cmake_minimum_required(VERSION 3.16)
project(core_engine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${CMAKE_CURRENT_SOURCE_DIR})

add_executable(core_engine
    main.cpp
    Scheduler.cpp
)

target_compile_features(core_engine PRIVATE cxx_std_17)
