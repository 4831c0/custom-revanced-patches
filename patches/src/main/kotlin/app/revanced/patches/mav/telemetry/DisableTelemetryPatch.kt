package app.revanced.patches.mav.telemetry

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val disableMavTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Removes telemetry-related root/emulator checks and Play Integrity helper logic for MÁV.",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    apply {
        val crashlyticsCommonUtilsSource = "CommonUtils.java"
        val minimumBooleanChecks = 2
        val minimumIntegerChecks = 1
        val noArgs = emptyList<CharSequence>()

        val commonUtils = classDefs
            .filter { it.sourceFile == crashlyticsCommonUtilsSource }
            .firstNotNullOfOrNull { classDef ->
                val proxy = classBy { it.type == classDef.type } ?: return@firstNotNullOfOrNull null
                val boolCount = proxy.mutableClass.methods.count {
                    it.returnType == "Z" && it.parameterTypes == noArgs
                }
                val intCount = proxy.mutableClass.methods.count {
                    it.returnType == "I" && it.parameterTypes == noArgs
                }
                if (boolCount >= minimumBooleanChecks && intCount >= minimumIntegerChecks) proxy else null
            } ?: throw PatchException("Crashlytics CommonUtils class not found")

        val staticNoArgBool = commonUtils.mutableClass.methods.filter {
            it.returnType == "Z" && it.parameterTypes == noArgs
        }
        val staticNoArgInt = commonUtils.mutableClass.methods.filter {
            it.returnType == "I" && it.parameterTypes == noArgs
        }

        if (staticNoArgBool.size < minimumBooleanChecks || staticNoArgInt.size < minimumIntegerChecks) {
            throw PatchException("CommonUtils heuristic methods not found")
        }

        staticNoArgBool.forEach { method ->
            method.addInstructions(
                0,
                """
                        const/4 v0, 0x0
                        return v0
                """.trimIndent(),
            )
        }
        staticNoArgInt.forEach { method ->
            method.addInstructions(
                0,
                """
                        const/4 v0, 0x0
                        return v0
                """.trimIndent(),
            )
        }
    }
}

