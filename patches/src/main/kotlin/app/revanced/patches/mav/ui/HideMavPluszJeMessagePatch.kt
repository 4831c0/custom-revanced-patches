package app.revanced.patches.mav.ui

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation

@Suppress("unused")
val hideMavPluszJeMessagePatch = bytecodePatch(
    name = "Hide MÁVPlusz JE message",
    description = "Hides server-driven JE information popups that promote the MÁVPlusz app (HU/EN).",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    extendWith("extensions/extension.rve")

    apply {
        val jeMessagesHelperType = "Lk8/x;"
        val jeMessagesDaoType = "Lt8/i;"

        val helper = classBy { it.type == jeMessagesHelperType }
            ?: throw PatchException("JeMessagesHelper (k8.x) not found")

        val showMessages = helper.mutableClass.methods.firstOrNull {
            it.name == "e"
                    && it.returnType == "V"
                    && it.parameterTypes.size == 1
                    && it.parameterTypes.first() == "Landroid/app/Activity;"
        } ?: throw PatchException("JeMessagesHelper.showMessages(e) not found")

        val hasMessages = helper.mutableClass.methods.firstOrNull {
            it.name == "d" && it.returnType == "Z" && it.parameterTypes.isEmpty()
        } ?: throw PatchException("JeMessagesHelper.hasMessages(d) not found")

        // Stock b() opens the DB and never closes it (openDBCount leak → ErrorBox on next open).
        val messageIcon = helper.mutableClass.methods.firstOrNull {
            it.name == "b" && it.returnType == "I" && it.parameterTypes.isEmpty()
        } ?: throw PatchException("JeMessagesHelper.messageIcon(b) not found")

        showMessages.implementation = MutableMethodImplementation(1)
        showMessages.addInstructions(
            """
                invoke-static {p0}, Lapp/revanced/extension/mav/JeMessagesFilter;->showFiltered(Landroid/app/Activity;)V
                return-void
            """.trimIndent(),
        )

        hasMessages.implementation = MutableMethodImplementation(1)
        hasMessages.addInstructions(
            """
                invoke-static {}, Lapp/revanced/extension/mav/JeMessagesFilter;->hasNonPromoMessages()Z
                move-result v0
                return v0
            """.trimIndent(),
        )

        messageIcon.implementation = MutableMethodImplementation(1)
        messageIcon.addInstructions(
            """
                invoke-static {}, Lapp/revanced/extension/mav/JeMessagesFilter;->messageIconResId()I
                move-result v0
                return v0
            """.trimIndent(),
        )

        val dao = classBy { it.type == jeMessagesDaoType }
            ?: throw PatchException("JE messages DAO (t8.i) not found")

        val insertMessages = dao.mutableClass.methods.firstOrNull {
            it.name == "U1"
                    && it.returnType == "V"
                    && it.parameterTypes.size == 1
                    && it.parameterTypes.first() == "Ljava/util/ArrayList;"
        } ?: throw PatchException("JE messages insert (t8.i.U1) not found")

        insertMessages.addInstructions(
            0,
            """
                invoke-static {p0}, Lapp/revanced/extension/mav/JeMessagesFilter;->removeFromList(Ljava/util/ArrayList;)V
            """.trimIndent(),
        )
    }
}
