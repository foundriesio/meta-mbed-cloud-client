SUMMARY = "Python module that implements the Arm MBED OS tool"
HOMEPAGE = "https://github.com/ARMmbed/mbed-os-tools"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d2794c0df5b907fdace235a619d80314"

PYPI_PACKAGE = "mbed-os-tools"

inherit pypi setuptools3

SRC_URI[md5sum] = "972221ce5d48c28a7d218d62957e0aa5"
SRC_URI[sha256sum] = "a6a3462c224a130622afbe9abeaa12931925bee8f4b446bc06f22a4e2be08f39"

DEPENDS += " \
    ${PYTHON_PN}-pip \
    ${PYTHON_PN}-click \
    ${PYTHON_PN}-prettytable \
    ${PYTHON_PN}-requests \
"

BBCLASSEXTEND = "native nativesdk"
