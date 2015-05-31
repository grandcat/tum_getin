# Find nfc.h
	
find_path(
    LIBNFC_INCLUDE_DIR
    "nfc/nfc.h"
    PATHS
        /usr/local/include
        /usr/include
)

if (NOT LIBNFC_INCLUDE_DIR)
    message (FATAL_ERROR "libnfc not found!")
endif ()

# Find library path
find_path(
    LIBNFC_LIBRARY_DIR
    "libnfc.so"
    PATHS
        /usr/local/lib
        /usr/lib
)

# Find libnfc.so dynamic library file.
FIND_LIBRARY(
    LIBNFC_LIBRARY
    NAMES
    nfc
    PATHS
        ${LIBNFC_LIBRARY_DIR}
)

INCLUDE(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(
	LIBNFC DEFAULT_MSG
	LIBNFC_INCLUDE_DIR
        LIBNFC_LIBRARY
)
MARK_AS_ADVANCED(LIBNFC_INCLUDE_DIRS LIBNFC_LIBRARY)

set (LIBNFC_FOUND YES)
