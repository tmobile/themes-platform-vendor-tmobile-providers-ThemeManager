LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := user eng

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	../ProfileManager/src/com/tmobile/profilemanager/Rosie.java \
	../ProfileManager/src/com/tmobile/profilemanager/utils/WallpaperUtilities.java

LOCAL_PACKAGE_NAME := ThemeManager

LOCAL_STATIC_JAVA_LIBRARIES := com.tmobile.themes \
    com.tmobile.widget

# Also link against our own custom library.
LOCAL_JAVA_LIBRARIES := com.htc.framework

# Specify certificate, to be used by this package ONLY.
# This way we guarantee that other packages won't be able to read
# DRM-locked resources from theme APKs.
# In order to uncomment the line below, we will need to add
# build/target/product/security/theme_manager.pk8 and theme_manager.x509.pem files.
LOCAL_CERTIFICATE := theme_manager_htc

include $(BUILD_PACKAGE)

# including the test apk
include $(call all-makefiles-under,$(LOCAL_PATH))
