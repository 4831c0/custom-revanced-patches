package app.revanced.patches.mav.auth

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch

@Suppress("unused")
val scheduleDailyTokenRefreshPatch = bytecodePatch(
    name = "Schedule daily token refresh",
    description = "Piggybacks token refresh on an already-registered JobScheduler service.",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    extendWith("extensions/extension.rve")

    apply {
        val jobServiceType = "Lcom/google/android/datatransport/runtime/scheduling/jobscheduling/JobInfoSchedulerService;"
        val jobServiceDef = classDefs.firstOrNull { it.type == jobServiceType || it.sourceFile == "JobInfoSchedulerService.java" }
            ?: throw PatchException("JobInfoSchedulerService class not found")

        val jobServiceClass = classBy { it.type == jobServiceDef.type }
            ?: throw PatchException("JobInfoSchedulerService proxy not found")

        val onStartJobMethod = jobServiceClass.mutableClass.methods.firstOrNull {
            it.name == "onStartJob" &&
                it.returnType == "Z" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes.first() == "Landroid/app/job/JobParameters;"
        } ?: throw PatchException("JobInfoSchedulerService.onStartJob(...) not found")

        onStartJobMethod.addInstructions(
            0,
            """
                invoke-static {}, Lapp/revanced/extension/mav/TokenRefreshHelper;->refreshIfNeeded()V
            """.trimIndent(),
        )

        val loginExtensionRequestType = "Lhu/mavszk/vonatinfo2/communication/request/c;"
        val loginExtensionRequestClass = classBy { it.type == loginExtensionRequestType }
            ?: throw PatchException("BejelentkezesHosszabbitas request class not found")

        val processResponseMethod = loginExtensionRequestClass.mutableClass.methods.firstOrNull {
            it.name == "k" &&
                it.returnType == "V" &&
                it.parameterTypes.size == 1 &&
                it.parameterTypes.first() == "Ljava/lang/Object;"
        } ?: throw PatchException("BejelentkezesHosszabbitas processResponse(k) not found")

        processResponseMethod.addInstructions(
            0,
            """
                invoke-static {p1}, Lapp/revanced/extension/mav/TokenRefreshHelper;->onLoginExtensionResponse(Ljava/lang/Object;)V
            """.trimIndent(),
        )
    }
}
