package app.revanced.patches.mav.ui

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val removeMavPlusPopupPatch = bytecodePatch(
    name = "Remove MÁV+ popup",
    description = "Remove MÁV+ popup.",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    apply {
        val popupCallbackClass = "Lhu/mavszk/vonatinfo2/gui/activity/d7;"
        val appContextProviderClass = "Lhu/mavszk/vonatinfo2/VonatInfo;"
        val prefsKeysClass = "Lk8/o1;"
        val sharedPrefsField = "a"
        val popupSeenField = "q"

        val classProxy = classBy { it.type == popupCallbackClass }
            ?: throw PatchException("Popup callback proxy not found")

        val method = classProxy.mutableClass.methods
            .firstOrNull { it.name == "h" && it.returnType == "V" && it.parameterTypes.isEmpty() }
            ?: throw PatchException("Popup decision method h() not found")

        method.addInstructions(
            0,
            """
                    new-instance v0, Ljava/text/SimpleDateFormat;
                    const-string v1, "yyyy-MM-dd"
                    invoke-static {}, Ljava/util/Locale;->getDefault()Ljava/util/Locale;
                    move-result-object v2
                    invoke-direct {v0, v1, v2}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;Ljava/util/Locale;)V
                    new-instance v1, Ljava/util/Date;
                    invoke-direct {v1}, Ljava/util/Date;-><init>()V
                    invoke-virtual {v0, v1}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;
                    move-result-object v2
                    invoke-static {}, $appContextProviderClass->e()Landroid/content/Context;
                    move-result-object v0
                    sget-object v1, $prefsKeysClass->$sharedPrefsField:Ljava/lang/String;
                    const/4 v3, 0x0
                    invoke-virtual {v0, v1, v3}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                    move-result-object v0
                    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v0
                    sget-object v1, $prefsKeysClass->$popupSeenField:Ljava/lang/String;
                    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
                    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
            """.trimIndent(),
        )
    }
}

