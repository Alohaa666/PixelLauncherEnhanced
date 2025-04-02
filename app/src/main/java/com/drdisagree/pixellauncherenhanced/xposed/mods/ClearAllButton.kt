package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import com.drdisagree.pixellauncherenhanced.R
import com.drdisagree.pixellauncherenhanced.data.common.Constants.RECENTS_CLEAR_ALL_BUTTON
import com.drdisagree.pixellauncherenhanced.xposed.HookRes.Companion.modRes
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.Helpers.toPx
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.callMethod
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.callStaticMethodSilently
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookConstructor
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookMethod
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs
import de.robv.android.xposed.XposedHelpers.findMethodBestMatch
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.lang.reflect.Method


class ClearAllButton(context: Context) : ModPack(context) {

    private var clearAllButton = false
    private var recentsViewInstance: Any? = null
    private var actionClearAllButton: Button? = null

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            clearAllButton = getBoolean(RECENTS_CLEAR_ALL_BUTTON, false)
        }

        when (key.firstOrNull()) {
            RECENTS_CLEAR_ALL_BUTTON -> updateVisibility()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("DiscouragedApi", "UseCompatLoadingForDrawables")
    override fun handleLoadPackage(loadPackageParam: LoadPackageParam) {
        val recentsViewClass = findClass("com.android.quickstep.views.RecentsView")
        val overviewModalTaskStateClass =
            findClass("com.android.launcher3.uioverrides.states.OverviewModalTaskState")
        val backgroundAppStateClass =
            findClass("com.android.launcher3.uioverrides.states.BackgroundAppState")
        val overviewStateClass =
            findClass("com.android.launcher3.uioverrides.states.OverviewState")
        val featureFlagsClass = findClass("com.android.launcher3.config.FeatureFlags")
        val recentsStateClass = findClass("com.android.quickstep.fallback.RecentsState")
        val overviewActionsViewClass = findClass("com.android.quickstep.views.OverviewActionsView")
        val dismissAllTasksMethod: Method =
            findMethodBestMatch(recentsViewClass, "dismissAllTasks", View::class.java)

        recentsViewClass
            .hookConstructor()
            .runAfter { param ->
                recentsViewInstance = param.thisObject
            }

        backgroundAppStateClass
            .hookMethod("getVisibleElements")
            .runAfter { param ->
                if (!clearAllButton) return@runAfter

                val result = param.result as Int
                param.result = result or CLEAR_ALL_BUTTON
            }

        overviewModalTaskStateClass
            .hookMethod("getVisibleElements")
            .runBefore { param ->
                if (!clearAllButton) return@runBefore

                param.result = OVERVIEW_ACTIONS
            }

        overviewStateClass
            .hookMethod("getVisibleElements")
            .runBefore { param ->
                if (!clearAllButton) return@runBefore

                val launcher = param.args[0]
                var elements: Int = OVERVIEW_ACTIONS
                val deviceProfile = launcher.callMethod("getDeviceProfile")

                val showFloatingSearch = if (deviceProfile.getField("isPhone") as Boolean) {
                    // Only show search in phone overview in portrait mode.
                    !(deviceProfile.getField("isLandscape") as Boolean)
                } else {
                    // Only show search in tablet overview if taskbar is not visible.
                    !(deviceProfile.getField("isTaskbarPresent") as Boolean) ||
                            param.thisObject.callMethod("isTaskbarStashed", launcher) as Boolean
                }

                if (showFloatingSearch) {
                    elements = elements or FLOATING_SEARCH_BAR
                }

                if (featureFlagsClass.callStaticMethodSilently("enableSplitContextual") as? Boolean != false &&
                    launcher.callMethod("isSplitSelectionActive") as Boolean
                ) {
                    elements = elements and CLEAR_ALL_BUTTON.inv()
                }

                param.result = elements
            }

        recentsStateClass
            .hookMethod("hasClearAllButton")
            .runBefore { param ->
                if (!clearAllButton) return@runBefore

                param.result = false
            }

        overviewActionsViewClass
            .hookMethod("onFinishInflate")
            .runAfter { param ->
                val mActionButtons = param.thisObject.getField("mActionButtons") as LinearLayout

                val contextThemeWrapper = ContextThemeWrapper(
                    mActionButtons.context,
                    mContext.resources.getIdentifier(
                        "ThemeControlHighlightWorkspaceColor",
                        "style",
                        mContext.packageName
                    )
                )

                actionClearAllButton = Button(
                    contextThemeWrapper,
                    null,
                    0,
                    mContext.resources.getIdentifier(
                        "OverviewActionButton",
                        "style",
                        mContext.packageName
                    )
                ).apply {
                    id = View.generateViewId()
                    text = modRes.getString(R.string.recents_clear_all)
                    layoutParams = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginStart = mContext.toPx(18)
                    }
                    setCompoundDrawablesWithIntrinsicBounds(
                        modRes.getDrawable(R.drawable.ic_clear_all),
                        null,
                        null,
                        null
                    )
                    setOnClickListener { view ->
                        dismissAllTasksMethod.invoke(recentsViewInstance, view)
                    }
                }

                updateVisibility()
                mActionButtons.addView(actionClearAllButton)
            }
    }

    @SuppressLint("DiscouragedApi")
    private fun updateVisibility() {
        if (clearAllButton) {
            actionClearAllButton?.visibility = View.VISIBLE
        } else {
            actionClearAllButton?.visibility = View.GONE
        }

        (actionClearAllButton?.parent as? LinearLayout)?.children?.forEach { child ->
            val params = child.layoutParams as ViewGroup.MarginLayoutParams

            if (params.marginStart != 0) {
                params.marginStart = if (clearAllButton) mContext.toPx(18)
                else mContext.resources.getDimensionPixelSize(
                    mContext.resources.getIdentifier(
                        "overview_actions_button_spacing",
                        "dimen",
                        mContext.packageName
                    )
                )
                child.layoutParams = params
            }
        }
    }

    companion object {
        private const val OVERVIEW_ACTIONS = 1 shl 3
        private const val CLEAR_ALL_BUTTON = 1 shl 4
        private const val FLOATING_SEARCH_BAR = 1 shl 7
    }
}