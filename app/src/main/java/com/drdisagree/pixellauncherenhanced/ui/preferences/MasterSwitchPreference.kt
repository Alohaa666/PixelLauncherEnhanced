package com.drdisagree.pixellauncherenhanced.ui.preferences

import android.content.Context
import android.util.AttributeSet
import androidx.preference.SwitchPreferenceCompat
import com.drdisagree.pixellauncherenhanced.R

class MasterSwitchPreference : SwitchPreferenceCompat {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        initResource()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initResource()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initResource()
    }

    constructor(context: Context) : super(context) {
        initResource()
    }

    private fun initResource() {
        layoutResource = R.layout.custom_preference_master_switch
        widgetLayoutResource = R.layout.preference_material_switch
    }
}