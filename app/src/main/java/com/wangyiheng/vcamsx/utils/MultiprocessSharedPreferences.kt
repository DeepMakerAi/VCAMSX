package com.wangyiheng.vcamsx.utils

import com.crossbowffs.remotepreferences.RemotePreferenceProvider


class MultiprocessSharedPreferences : RemotePreferenceProvider("com.wangyiheng.vcamsx.preferences", arrayOf("main_prefs"))