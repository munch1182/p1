package com.munch1182.lib.result

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver

class ActivityResultCollect(private val owner: ActivityResultRegistryOwner) :
    DefaultLifecycleObserver {
}