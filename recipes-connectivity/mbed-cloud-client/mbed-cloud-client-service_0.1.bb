SUMMARY = "Arm MBED Cloud Client Service"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

inherit allarch systemd

SRC_URI = " \
    file://mbed-cloud-client.service \
"

S = "${WORKDIR}"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SYSTEMD_SERVICE_${PN} = "mbed-cloud-client.service"
SYSTEMD_AUTO_ENABLE_${PN} = "enable"

do_install () {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/mbed-cloud-client.service ${D}${systemd_system_unitdir}
}

FILES_${PN} += "${systemd_system_unitdir}/mbed-cloud-client.service"
FILES_${PN} += "${systemd_unitdir}/system-preset"
