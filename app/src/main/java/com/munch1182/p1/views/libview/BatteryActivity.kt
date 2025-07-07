package com.munch1182.p1.views.libview

import android.annotation.SuppressLint
import android.os.Bundle
import com.munch1182.p1.base.BaseActivity
import com.munch1182.p1.base.bind
import com.munch1182.p1.databinding.ActivityBatteryBinding

class BatteryActivity : BaseActivity() {

    private val bind by bind(ActivityBatteryBinding::inflate)

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind.changeLevel.setOnClickListener {
            var level = bind.battery.level - 10
            if (level < 0) level = 100
            bind.battery.setLevel(level)
        }
    }
}
