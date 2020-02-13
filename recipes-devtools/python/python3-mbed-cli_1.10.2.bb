SUMMARY = "Python module that implements the Arm MBED cli tool"
HOMEPAGE = "https://github.com/ARMmbed/mbed-cli"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=4336ad26bb93846e47581adc44c4514d"

PYPI_PACKAGE = "mbed-cli"

inherit pypi setuptools3

SRC_URI[md5sum] = "389874c7a35527e30078654b064bb058"
SRC_URI[sha256sum] = "36dd5496fc5cdc992d5524360bc48c61290e0470f7565e048823a0eef5ca4634"

DEPENDS += " \
    ${PYTHON_PN}-pip \
    ${PYTHON_PN}-mbed-os-tools \
    ${PYTHON_PN}-pyserial \
"

RDEPENDS_${PN} = " \
    mercurial \
"

BBCLASSEXTEND = "native nativesdk"
