project(nfc_communicator)
cmake_minimum_required(VERSION 2.8)

add_definitions(
    -std=c++0x  # Or -std=c++11
    # Other flags
)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/cmake/modules/")

# Sources and header files
aux_source_directory(src/. SRC_LIST)
file(GLOB_RECURSE HEADER_LIST RELATIVE "${CMAKE_CURRENT_SOURCE_DIR}" "*.h")
message (STATUS "CMake current source dir: ${CMAKE_CURRENT_SOURCE_DIR}/src")
message (STATUS "Header list: ${HEADER_LIST}")

# Load external libraries

# NFC library
find_package(LibNfc REQUIRED)
message (STATUS "LIBNFC_LIBRARY: ${LIBNFC_LIBRARY}")
message (STATUS "LIBNFC_INCLUDE_DIR: ${LIBNFC_INCLUDE_DIR}")
include_directories(${LIBNFC_INCLUDE_DIR})

# Qt4 package
find_package(Qt4 COMPONENTS QTCORE QTDBUS)
set(QT_DONT_USE_QTGUI true)
include(${QT_USE_FILE})
#include_directories(${QT_INCLUDES})
# Qt Definitions
add_definitions(${QT_DEFINITIONS})
# Setup Qt
qt4_wrap_cpp(NFC_COMM_MOC_SOURCES ${HEADER_LIST})

add_executable(${PROJECT_NAME} ${SRC_LIST} ${NFC_COMM_MOC_SOURCES})
TARGET_LINK_LIBRARIES(${PROJECT_NAME} nfc ${QT_LIBRARIES})

###############################################################################
# Creates QT Creator specific file list for remote deployment on Raspberry Pi #
###############################################################################
file(WRITE "${CMAKE_SOURCE_DIR}/QtCreatorDeployment.txt" "/home/pi/nfc_communicator\n")

 macro(add_deployment_file SRC DEST)
    file(RELATIVE_PATH path ${CMAKE_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR})
    file(APPEND "${CMAKE_SOURCE_DIR}/QtCreatorDeployment.txt" "${path}/${SRC}:${DEST}\n")
endmacro()

macro(add_deployment_directory SRC DEST)
    file(GLOB_RECURSE files RELATIVE "${CMAKE_CURRENT_SOURCE_DIR}" "${SRC}/*")
    foreach(filename ${files})
        get_filename_component(path ${filename} PATH)
        add_deployment_file("${filename}" "${DEST}/${path}")
    endforeach(filename)
endmacro()

# Add all source and header files of the project
foreach(filename ${SRC_LIST})
    # Source files
    get_filename_component(path ${filename} PATH)
    add_deployment_file("${filename}" "${DEST}/${path}")
endforeach(filename)
foreach(filename ${HEADER_LIST})
    # Header files
    get_filename_component(path ${filename} PATH)
    add_deployment_file("${filename}" "${DEST}/${path}")
endforeach(filename)
# Add this CMake file itself
get_filename_component(path "CMakeLists.txt" PATH)
add_deployment_file("CMakeLists.txt" "${DEST}/${path}")
get_filename_component(path "cmake/modules/FindLibNfc.cmake" PATH)
add_deployment_file("cmake/modules/FindLibNfc.cmake" "${DEST}/${path}")
