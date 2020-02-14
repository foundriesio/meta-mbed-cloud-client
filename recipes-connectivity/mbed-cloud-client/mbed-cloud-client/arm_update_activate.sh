#!/bin/sh
# ----------------------------------------------------------------------------
# Copyright 2016-2017 ARM Ltd.
# Copyright 2020 Foundries.io
#
# SPDX-License-Identifier: Apache-2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http:#www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ----------------------------------------------------------------------------

# dont exit on errors
set -x

# Parse command line for: HEADER, FIRMWARE
. arm_update_cmdline.sh

# source "${FIRMWARE}" file:
#     LMP_VERSION="184"
#     LMP_DOCKER_APPS="shellhttpd, x-kiosk"
#     LMP_TAGS="devel"
# TODO: Not sure this is safe as it's passed as a parameter
if ! . ${FIRMWARE} > /dev/null; then
    exit 1
fi

# backup /var/sota/sota.toml
if ! cp /var/sota/sota.toml /var/sota/sota.toml.bak > /dev/null; then
    exit 2
fi

if [ ! -z "${LMP_DOCKER_APPS}" ]; then
    # remove docker_apps line
    sed -i "/^docker_apps =.*$/d" /var/sota/sota.toml
    # insert docker_apps line after [pacman]
    sed -i "/^\[pacman\]/a docker_apps =\"${LMP_DOCKER_APPS}\"" /var/sota/sota.toml
fi
if [ ! -z "${LMP_TAGS}" ]; then
    # remove tags line
    sed -i "/^tags =.*$/d" /var/sota/sota.toml
    # insert tags line after [pacman]
    sed -i "/^\[pacman\]/a tags =\"${LMP_TAGS}\"" /var/sota/sota.toml
fi

# call aktualizr-lite update --update-name ${LMP_VERSION}
if ! aktualizr-lite update --update-name "${LMP_VERSION}"; then
    cp /var/sota/sota.toml.bak /var/sota/sota.toml
    exit 3
fi

# use ostree admin status to get pending SHA / current SHA
PENDING_SHA=$(ostree admin status | grep -e "(pending)$" | awk '{print $2}' | cut -c-64)
if [ -z "${PENDING_SHA}" ]; then
    # firmware update could have applied the same firmware with only docker-app changes
    # return current active in this case(?)
    PENDING_SHA=$(ostree admin status | grep -e "^* " | awk '{print $3}' | cut -c-64)
    if [ -z "${PENDING_SHA}" ]; then
        # TODO: revert?!
        cp /var/sota/sota.toml.bak /var/sota/sota.toml
        exit 4
    fi
fi

# save "${FIRMWARE}" file with pending SHA
if ! cp "${FIRMWARE}" /var/sota/pelion/firmware-${PENDING_SHA}; then
    # TODO: revert?!
    cp /var/sota/sota.toml.bak /var/sota/sota.toml
    exit 5
fi

# save "${HEADER}" file with pending SHA
if ! cp "${HEADER}" /var/sota/pelion/header-${PENDING_SHA}; then
    # TODO: revert?!
    cp /var/sota/sota.toml.bak /var/sota/sota.toml
    exit 6
fi

exit 0
