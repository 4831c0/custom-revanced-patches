package app.revanced.patches.mav.ui

import app.revanced.patcher.patch.resourcePatch

@Suppress("unused")
val fixTopBarLayoutResourcesPatch = resourcePatch(
    name = "Fix top bar layout resources",
    description = "Removes legacy fitsSystemWindows from AppCompat action bar layouts.",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    apply {
        fun setFitsSystemWindowsFalse(layoutPath: String, tagName: String) {
            document(layoutPath).use { doc ->
                val node = doc.getElementsByTagName(tagName).item(0) ?: return@use
                val attr = node.attributes?.getNamedItem("android:fitsSystemWindows") ?: return@use
                attr.nodeValue = "false"
            }
        }

        setFitsSystemWindowsFalse(
            layoutPath = "res/layout/abc_screen_simple.xml",
            tagName = "androidx.appcompat.widget.FitWindowsLinearLayout",
        )
        setFitsSystemWindowsFalse(
            layoutPath = "res/layout/abc_screen_simple_overlay_action_mode.xml",
            tagName = "androidx.appcompat.widget.FitWindowsFrameLayout",
        )
        setFitsSystemWindowsFalse(
            layoutPath = "res/layout/abc_screen_toolbar.xml",
            tagName = "androidx.appcompat.widget.ActionBarOverlayLayout",
        )
    }
}
