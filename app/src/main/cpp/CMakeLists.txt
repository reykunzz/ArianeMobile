cmake_minimum_required(VERSION 3.22.1)
project(ariane_mobile LANGUAGES C CXX)

option(ARIANE_ENABLE_UPSTREAM_CORE "Build an Android-adapted Ariane core source tree" OFF)
set(ARIANE_CORE_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../native/ariane-core" CACHE PATH "Android-adapted Ariane core")

add_library(ariane_mobile SHARED
    jni_bridge.cpp
    renderer/GLESRenderer.cpp
    formats/IPLReader.cpp
    formats/IMGArchive.cpp
    engine/EngineStatus.cpp
)
target_compile_features(ariane_mobile PRIVATE cxx_std_17)
target_compile_options(ariane_mobile PRIVATE -Wall -Wextra -Wpedantic)
target_include_directories(ariane_mobile PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}" "${CMAKE_CURRENT_SOURCE_DIR}/formats" "${CMAKE_CURRENT_SOURCE_DIR}/engine")
find_library(log-lib log REQUIRED)
find_library(gles-lib GLESv3 REQUIRED)
target_link_libraries(ariane_mobile PRIVATE ${log-lib} ${gles-lib})

# Upstream Ariane currently has a desktop Premake/GLFW entry point. Link only after
# its game/editor core and Android GLES platform layer have been separated.
if(ARIANE_ENABLE_UPSTREAM_CORE)
    if(NOT EXISTS "${ARIANE_CORE_DIR}/CMakeLists.txt")
        message(FATAL_ERROR "No Android-adapted Ariane core CMakeLists.txt at ${ARIANE_CORE_DIR}")
    endif()
    add_subdirectory("${ARIANE_CORE_DIR}" ariane-core)
    target_link_libraries(ariane_mobile PRIVATE ariane_core)
    target_compile_definitions(ariane_mobile PRIVATE ARIANE_UPSTREAM_CORE_ENABLED=1)
else()
    message(STATUS "Building Android bootstrap adapter; upstream Ariane/librw not linked yet.")
endif()
