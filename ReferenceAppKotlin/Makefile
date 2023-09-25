# Copyright 2023 defsub
#
# This file is part of Takeout.
#
# Takeout is free software: you can redistribute it and/or modify it under the
# terms of the GNU Affero General Public License as published by the Free
# Software Foundation, either version 3 of the License, or (at your option)
# any later version.
#
# Takeout is distributed in the hope that it will be useful, but WITHOUT ANY
# WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
# FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for
# more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with Takeout.  If not, see <https://www.gnu.org/licenses/>.

VERSION = $(shell cat .version)

ADB = adb

APKSIGNER = apksigner

GRADLE = ./gradlew

GIT_VERSION ?= $(shell git log --format="%h" -n 1)

DEBUG_APK = ./build/app/outputs/apk/debug/app-debug.apk

RELEASE_APK = ./build/app/outputs/apk/release/app-release.apk

ASSETS = ./assets

all: debug

install: install-debug

debug: ${DEBUG_APK}

install-debug: debug
	${ADB} install ${DEBUG_APK}

${DEBUG_APK}:
	${GRADLE} assembleDebug

release: ${RELEASE_APK}

install-release: release
	${ADB} install ${RELEASE_APK}

${RELEASE_APK}:
	${GRADLE} assembleRelease

verify:
	${APKSIGNER} verify --verbose --print-certs ${RELEASE_APK}

clean:
	rm -f ${DEBUG_APK}
	rm -f ${RELEASE_APK}
	rm -rf ${ASSETS}
	${GRADLE} clean

tag:
	git tag --list | grep -q v${VERSION} || git tag v${VERSION}
	git push origin v${VERSION}

assets:
	mkdir ${ASSETS}
	cp ${RELEASE_APK} ${ASSETS}/com.takeoutfm.tv-${VERSION}.apk

version:
	scripts/version.sh && git commit -a
