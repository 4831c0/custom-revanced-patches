package app.revanced.patches.mav.ui

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val fixTopBarAlignmentPatch = bytecodePatch(
    name = "Fix top bar alignment",
    description = "Fixes global top menu bar vertical alignment.",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    apply {
        val baseActivityType = "Lhu/mavszk/vonatinfo2/gui/activity/m;"
        val baseActivitySource = "BaseActivity.java"
        val actionBarOverlayType = "Landroidx/appcompat/widget/ActionBarOverlayLayout;"
        val actionBarOverlaySource = "ActionBarOverlayLayout.java"
        val toolbarType = "Landroidx/appcompat/widget/Toolbar;"
        val toolbarViewIdHex = "0x7f090034"
        val statusBarHeightKey = "status_bar_height"
        val dimenType = "dimen"
        val androidPackage = "android"

        val baseActivityClassDef = classDefs.firstOrNull { it.type == baseActivityType }
            ?: classDefs.firstOrNull { it.sourceFile == baseActivitySource }
            ?: throw PatchException("BaseActivity class not found")
        val baseActivityClass = classBy { it.type == baseActivityClassDef.type }
            ?: throw PatchException("BaseActivity proxy not found")
        val resumeMethod = baseActivityClass.mutableClass.methods.firstOrNull { method ->
            method.name == "onResume" &&
                method.returnType == "V" &&
                method.parameterTypes.isEmpty()
        } ?: throw PatchException("BaseActivity.onResume() not found")
        val actionBarOverlayClassDef = classDefs.firstOrNull { it.type == actionBarOverlayType }
            ?: classDefs.firstOrNull { it.sourceFile == actionBarOverlaySource }
            ?: throw PatchException("ActionBarOverlayLayout class not found")
        val actionBarOverlayClass = classBy { it.type == actionBarOverlayClassDef.type }
            ?: throw PatchException("ActionBarOverlayLayout proxy not found")
        val applyInsetsMethod = actionBarOverlayClass.mutableClass.methods.firstOrNull { method ->
            method.name == "onApplyWindowInsets" &&
                method.returnType == "Landroid/view/WindowInsets;" &&
                method.parameterTypes.map(CharSequence::toString) == listOf("Landroid/view/WindowInsets;")
        } ?: throw PatchException("ActionBarOverlayLayout.onApplyWindowInsets(WindowInsets) not found")
        applyInsetsMethod.addInstructions(
            0,
            """
                    invoke-virtual {p1}, Landroid/view/WindowInsets;->getSystemWindowInsetLeft()I
                    move-result v0
                    invoke-virtual {p1}, Landroid/view/WindowInsets;->getSystemWindowInsetRight()I
                    move-result v1
                    invoke-virtual {p1}, Landroid/view/WindowInsets;->getSystemWindowInsetBottom()I
                    move-result v2
                    const/4 v3, 0x0
                    invoke-virtual {p1, v0, v3, v1, v2}, Landroid/view/WindowInsets;->replaceSystemWindowInsets(IIII)Landroid/view/WindowInsets;
                    move-result-object p1
            """.trimIndent(),
        )
        resumeMethod.addInstructions(
            0,
            """
                    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
                    move-result-object v0
                    const-string v1, "$statusBarHeightKey"
                    const-string v2, "$dimenType"
                    const-string v3, "$androidPackage"
                    invoke-virtual {v0, v1, v2, v3}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
                    move-result v1
                    const/4 v2, 0x0
                    if-lez v1, :mav_fix_top_bar_margin_apply
                    invoke-virtual {v0, v1}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I
                    move-result v2
                    :mav_fix_top_bar_margin_apply
                    const v0, $toolbarViewIdHex
                    invoke-virtual {p0, v0}, Lg/g;->findViewById(I)Landroid/view/View;
                    move-result-object v0
                    check-cast v0, $toolbarType
                    if-eqz v0, :mav_fix_top_bar_margin_done
                    invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
                    move-result-object v1
                    instance-of v3, v1, Landroid/view/ViewGroup${'$'}MarginLayoutParams;
                    if-eqz v3, :mav_fix_top_bar_margin_done
                    check-cast v1, Landroid/view/ViewGroup${'$'}MarginLayoutParams;
                    const/4 v3, 0x0
                    invoke-virtual {v1, v3, v2, v3, v3}, Landroid/view/ViewGroup${'$'}MarginLayoutParams;->setMargins(IIII)V
                    invoke-virtual {v0, v1}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V
                    :mav_fix_top_bar_margin_done
            """.trimIndent(),
        )
    }
}

