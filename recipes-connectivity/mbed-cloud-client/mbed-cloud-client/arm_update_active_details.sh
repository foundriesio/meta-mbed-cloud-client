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

# Parse command line for: HEADER
. arm_update_cmdline.sh

# get active SHA from ostree admin status
CURRENT_SHA=$(ostree admin status | grep -e "^* " | awk '{print $3}' | cut -c-64)
if [ -z "${CURRENT_SHA}" ]; then
    exit 10
fi

# copy header with SHA to $HEADER
if ! cp "/var/sota/pelion/header-${CURRENT_SHA}" "${HEADER}"; then
    exit 11
fi

exit 0
