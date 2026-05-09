cmake_minimum_required(VERSION 3.14)
project(core_engine)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

add_executable(core_engine main.cpp Agents.cpp)
