SUMMARY = "Arm Pelion Cloud Client"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4336ad26bb93846e47581adc44c4514d"

# FILESEXTRAPATHS_prepend := "${THISDIR}/${BPN}:"

SRCREV = "2278a25d10a9eb01f66f1250b028c73295634930"
SRCBRANCH = "4.3.0"
SRC_URI = " \
    git://github.com/ARMmbed/mbed-cloud-client-example.git;protocol=https \
    file://0001-fix-includes.ptc \
    file://0001-mbed-cloud-client-fix-includes.ptc \
    file://arm_uc_pal_linux_lmp.c \
    file://define-lmp.txt \
    file://arm_update_cmdline.sh \
    file://arm_update_activate.sh \
    file://arm_update_active_details.sh \
"

inherit python3native cmake

DEPENDS += "\
    ${PYTHON_PN}-native \
    ${PYTHON_PN}-pip-native \
    ${PYTHON_PN}-mbed-cli-native \
    glibc \
"

RDEPENDS_${PN} = " \
    libstdc++ \
    libgcc \
"

# set default define file (override this if needed)
MBED_CLOUD_DEFINE_FILE = "define-lmp.txt"

TARGET = "Yocto_Generic_YoctoLinux_mbedtls"

S = "${WORKDIR}/git"
S_GEN = "${WORKDIR}/git/__${TARGET}"
B = "${WORKDIR}/git/__${TARGET}"

OECMAKE_EXTRA_ROOT_PATH = "${STAGING_DIR_NATIVE}/usr/bin/${TARGET_SYS}"
OECMAKE_SOURCEPATH = "${S_GEN}"
OECMAKE_TARGET_COMPILE = "mbedCloudClientExample.elf"

# Allowed [Debug|Release]
RELEASE_TYPE = "Release"

EXTRA_OECMAKE = " \
    -DCMAKE_BUILD_TYPE=${RELEASE_TYPE} \
    -DEXTERNAL_DEFINE_FILE=${S}/${MBED_CLOUD_DEFINE_FILE} \
"

# customize generate_toolchain_file to include full CC paths
cmake_do_generate_toolchain_file() {
	if [ "${BUILD_SYS}" = "${HOST_SYS}" ]; then
		cmake_crosscompiling="set( CMAKE_CROSSCOMPILING FALSE )"
	fi
	cat > ${WORKDIR}/toolchain.cmake <<EOF
# CMake system name must be something like "Linux".
# This is important for cross-compiling.
$cmake_crosscompiling
set( CMAKE_SYSTEM_NAME `echo ${TARGET_OS} | sed -e 's/^./\u&/' -e 's/^\(Linux\).*/\1/'` )
set( CMAKE_SYSTEM_PROCESSOR ${@map_target_arch_to_uname_arch(d.getVar('TARGET_ARCH'))} )
set( CMAKE_C_COMPILER ${OECMAKE_EXTRA_ROOT_PATH}/${OECMAKE_C_COMPILER} )
set( CMAKE_CXX_COMPILER ${OECMAKE_EXTRA_ROOT_PATH}/${OECMAKE_CXX_COMPILER} )
set( CMAKE_C_COMPILER_LAUNCHER ${OECMAKE_C_COMPILER_LAUNCHER} )
set( CMAKE_CXX_COMPILER_LAUNCHER ${OECMAKE_CXX_COMPILER_LAUNCHER} )
set( CMAKE_ASM_COMPILER ${OECMAKE_C_COMPILER} )
set( CMAKE_AR ${OECMAKE_AR} CACHE FILEPATH "Archiver" )
set( CMAKE_C_FLAGS "${OECMAKE_C_FLAGS}" CACHE STRING "CFLAGS" )
set( CMAKE_CXX_FLAGS "${OECMAKE_CXX_FLAGS}" CACHE STRING "CXXFLAGS" )
set( CMAKE_ASM_FLAGS "${OECMAKE_C_FLAGS}" CACHE STRING "ASM FLAGS" )
set( CMAKE_C_FLAGS_RELEASE "${OECMAKE_C_FLAGS_RELEASE}" CACHE STRING "Additional CFLAGS for release" )
set( CMAKE_CXX_FLAGS_RELEASE "${OECMAKE_CXX_FLAGS_RELEASE}" CACHE STRING "Additional CXXFLAGS for release" )
set( CMAKE_ASM_FLAGS_RELEASE "${OECMAKE_C_FLAGS_RELEASE}" CACHE STRING "Additional ASM FLAGS for release" )
set( CMAKE_C_LINK_FLAGS "${OECMAKE_C_LINK_FLAGS}" CACHE STRING "LDFLAGS" )
set( CMAKE_CXX_LINK_FLAGS "${OECMAKE_CXX_LINK_FLAGS}" CACHE STRING "LDFLAGS" )

# only search in the paths provided so cmake doesnt pick
# up libraries and tools from the native build machine
set( CMAKE_FIND_ROOT_PATH ${STAGING_DIR_HOST} ${STAGING_DIR_NATIVE} ${CROSS_DIR} ${OECMAKE_PERLNATIVE_DIR} ${OECMAKE_EXTRA_ROOT_PATH} ${EXTERNAL_TOOLCHAIN} ${HOSTTOOLS_DIR})
set( CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY )
set( CMAKE_FIND_ROOT_PATH_MODE_PROGRAM ${OECMAKE_FIND_ROOT_PATH_MODE_PROGRAM} )
set( CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY )
set( CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY )
set( CMAKE_PROGRAM_PATH "/" )

# Use qt.conf settings
set( ENV{QT_CONF_PATH} ${WORKDIR}/qt.conf )

# We need to set the rpath to the correct directory as cmake does not provide any
# directory as rpath by default
set( CMAKE_INSTALL_RPATH ${OECMAKE_RPATH} )

# Use RPATHs relative to build directory for reproducibility
set( CMAKE_BUILD_RPATH_USE_ORIGIN ON )

# Use our cmake modules
list(APPEND CMAKE_MODULE_PATH "${STAGING_DATADIR}/cmake/Modules/")

# add for non /usr/lib libdir, e.g. /usr/lib64
set( CMAKE_LIBRARY_PATH ${libdir} ${base_libdir})

# add include dir to implicit includes in case it differs from /usr/include
list(APPEND CMAKE_C_IMPLICIT_INCLUDE_DIRECTORIES ${includedir})
list(APPEND CMAKE_CXX_IMPLICIT_INCLUDE_DIRECTORIES ${includedir})

EOF
}

# include custom copy of cmake_do_configure to make sure ${B} isn't cleared
do_configure() {
    # clean the source-gen directory
    rm -rf "${S_GEN}"

    # deplay mbed dependencies
    cd "${S}"
    ${PYTHON} -m mbed deploy --no-requirements

    # generate new target files
    ${PYTHON} ./pal-platform/pal-platform.py -v deploy --target="${TARGET}" generate

    cp "${WORKDIR}/0001-fix-includes.ptc" "${S}/0001-fix-includes.patch"
    cd "${S}"
    git reset --hard
    git apply 0001-fix-includes.patch

    cp "${WORKDIR}/0001-mbed-cloud-client-fix-includes.ptc" "${S}/mbed-cloud-client/0001-mbed-cloud-client-fix-includes.patch"
    cd "${S}/mbed-cloud-client/"
    git reset --hard
    git apply 0001-mbed-cloud-client-fix-includes.patch

    # add LmP PAL file
    cp "${WORKDIR}/arm_uc_pal_linux_lmp.c" "${S}/mbed-cloud-client/update-client-hub/modules/pal-linux/source/"

    if [ -e "${WORKDIR}/${MBED_CLOUD_IDENTITY_CERT_FILE}" ]; then
        cp "${WORKDIR}/${MBED_CLOUD_IDENTITY_CERT_FILE}" "${S}/mbed_cloud_dev_credentials.c"
    else
        # Not set.
        echo "ERROR certification file does not set !!!"
        exit 1
    fi

    if [ -e "${WORKDIR}/${MBED_UPDATE_RESOURCE_FILE}" ]; then
        cp "${WORKDIR}/${MBED_UPDATE_RESOURCE_FILE}" "${S}/update_default_resources.c"
    fi

    if [ -e "${WORKDIR}/${MBED_CLOUD_DEFINE_FILE}" ]; then
        cp "${WORKDIR}/${MBED_CLOUD_DEFINE_FILE}" "${S}/"
    fi

    if [ "${OECMAKE_BUILDPATH}" ]; then
        bbnote "cmake.bbclass no longer uses OECMAKE_BUILDPATH.  The default behaviour is now out-of-tree builds with B=WORKDIR/build."
    fi

    cd ${B}
    if [ "${S_GEN}" != "${B}" ]; then
        rm -rf ${B}
        mkdir -p ${B}
        cd ${B}
    else
        find ${B} -name CMakeFiles -or -name Makefile -or -name cmake_install.cmake -or -name CMakeCache.txt -delete
    fi

    # Just like autotools cmake can use a site file to cache result that need generated binaries to run
    if [ -e ${WORKDIR}/site-file.cmake ] ; then
        oecmake_sitefile="-C ${WORKDIR}/site-file.cmake"
    else
        oecmake_sitefile=
    fi

    cmake \
      ${OECMAKE_GENERATOR_ARGS} \
      $oecmake_sitefile \
      ${OECMAKE_SOURCEPATH} \
      -DCMAKE_INSTALL_PREFIX:PATH=${prefix} \
      -DCMAKE_INSTALL_BINDIR:PATH=${@os.path.relpath(d.getVar('bindir'), d.getVar('prefix') + '/')} \
      -DCMAKE_INSTALL_SBINDIR:PATH=${@os.path.relpath(d.getVar('sbindir'), d.getVar('prefix') + '/')} \
      -DCMAKE_INSTALL_LIBEXECDIR:PATH=${@os.path.relpath(d.getVar('libexecdir'), d.getVar('prefix') + '/')} \
      -DCMAKE_INSTALL_SYSCONFDIR:PATH=${sysconfdir} \
      -DCMAKE_INSTALL_SHAREDSTATEDIR:PATH=${@os.path.relpath(d.getVar('sharedstatedir'), d.  getVar('prefix') + '/')} \
      -DCMAKE_INSTALL_LOCALSTATEDIR:PATH=${localstatedir} \
      -DCMAKE_INSTALL_LIBDIR:PATH=${@os.path.relpath(d.getVar('libdir'), d.getVar('prefix') + '/')} \
      -DCMAKE_INSTALL_INCLUDEDIR:PATH=${@os.path.relpath(d.getVar('includedir'), d.getVar('prefix') + '/')} \
      -DCMAKE_INSTALL_DATAROOTDIR:PATH=${@os.path.relpath(d.getVar('datadir'), d.getVar('prefix') + '/')} \
      -DLIB_SUFFIX=${@d.getVar('baselib').replace('lib', '')} \
      -DCMAKE_INSTALL_SO_NO_EXE=0 \
      -DCMAKE_TOOLCHAIN_FILE=${WORKDIR}/toolchain.cmake \
      -DCMAKE_NO_SYSTEM_FROM_IMPORTED=1 \
      ${EXTRA_OECMAKE} \
      -Wno-dev
}

do_install() {
    install -d "${D}${bindir}"
    install -m 755 "${B}/${RELEASE_TYPE}/mbedCloudClientExample.elf" "${D}${bindir}/mbedCloudClient"
    install -m 755 "${WORKDIR}/arm_update_cmdline.sh" "${D}${bindir}"
    install -m 755 "${WORKDIR}/arm_update_activate.sh" "${D}${bindir}"
    install -m 755 "${WORKDIR}/arm_update_active_details.sh" "${D}${bindir}"
}
