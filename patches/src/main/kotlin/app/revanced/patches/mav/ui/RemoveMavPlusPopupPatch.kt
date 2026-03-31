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
        val popupCallbackInterface = "Lk8/d\$a;"
        val appContextProviderClass = "Lhu/mavszk/vonatinfo2/VonatInfo;"
        val prefsKeysClass = "Lk8/o1;"
        val sharedPrefsField = "a"
        val popupSeenField = "q"

        val candidateClass = classDefs
            .filter { classDef -> classDef.interfaces.contains(popupCallbackInterface) }
            .maxByOrNull { classDef ->
                classDef.methods
                    .filter { it.returnType == "V" && it.parameterTypes.isEmpty() }
                    .maxOfOrNull { it.implementation?.instructions?.count() ?: 0 } ?: 0
            } ?: throw PatchException("Popup callback class not found")

        val classProxy = classBy { it.type == candidateClass.type }
            ?: throw PatchException("Popup callback proxy not found")

        val method = classProxy.mutableClass.methods
            .filter { it.returnType == "V" && it.parameterTypes.isEmpty() }
            .maxByOrNull { it.implementation?.instructions?.count() ?: 0 }
            ?: throw PatchException("Popup decision method not found")

        method.addInstructions(
            0,
            """
                    invoke-static {}, $appContextProviderClass->e()Landroid/content/Context;
                    move-result-object v0
                    sget-object v1, $prefsKeysClass->$sharedPrefsField:Ljava/lang/String;
                    const/4 v2, 0x0
                    invoke-virtual {v0, v1, v2}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
                    move-result-object v0
                    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
                    move-result-object v0
                    sget-object v1, $prefsKeysClass->$popupSeenField:Ljava/lang/String;
                    const/4 v2, 0x1
                    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
                    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
            """.trimIndent(),
        )
    }
}

