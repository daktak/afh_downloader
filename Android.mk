LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := afhDownloader
LOCAL_MODULE_TAGS := optional
LOCAL_PACKAGE_NAME := afhDownloader

afhdownloader_root  := $(LOCAL_PATH)
afhdownloader_dir   := app
afhdownloader_out   := $(OUT_DIR)/target/common/obj/APPS/$(LOCAL_MODULE)_intermediates
afhdownloader_build := $(afhdownloader_root)/$(afhdownloader_dir)/build
afhdownloader_apk   := build/outputs/apk/app-release-unsigned.apk

$(afhdownloader_root)/$(afhdownloader_dir)/$(afhdownloader_apk):
	rm -Rf $(afhdownloader_build)
	mkdir -p $(afhdownloader_out)
	ln -s $(afhdownloader_out) $(afhdownloader_build)
	echo "sdk.dir=$(ANDROID_HOME)" > $(afhdownloader_root)/local.properties
	cd $(afhdownloader_root) && git submodule update --recursive --init
	cd $(afhdownloader_root)/$(afhdownloader_dir) && JAVA_TOOL_OPTIONS="$(JAVA_TOOL_OPTIONS) -Dfile.encoding=UTF8" ../gradlew assembleRelease

LOCAL_CERTIFICATE := platform
LOCAL_SRC_FILES := $(afhdownloader_dir)/$(afhdownloader_apk)
LOCAL_MODULE_CLASS := APPS
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)

include $(BUILD_PREBUILT)
