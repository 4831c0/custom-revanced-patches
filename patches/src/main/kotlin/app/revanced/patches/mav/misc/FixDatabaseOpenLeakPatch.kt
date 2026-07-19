package app.revanced.patches.mav.misc

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch

/**
 * Stock Vonatinfó tracks SQLite opens with [openDBCount] and crashes via ErrorBox when a
 * caller forgets to close (seen after JE message icon helper and similar paths).
 * Reset the counter at the start of each open so a prior leak cannot block [BaseActivity.onResume].
 */
@Suppress("unused")
val fixDatabaseOpenLeakPatch = bytecodePatch(
    name = "Fix database open leak",
    description = "Prevents ErrorBox when DatabaseHelper openDBCount was left non-zero by a prior leak.",
) {
    compatibleWith("hu.mavszk.vonatinfo"("4.12"))

    apply {
        val helper = classBy { it.type == "Lw6/b;" }
            ?: throw PatchException("DatabaseHelper (w6.b) not found")

        val getDatabase = helper.mutableClass.methods.firstOrNull {
            it.name == "i"
                    && it.returnType == "Landroid/database/sqlite/SQLiteDatabase;"
                    && it.parameterTypes.size == 1
                    && it.parameterTypes.first() == "Z"
        } ?: throw PatchException("DatabaseHelper.getDatabase(i) not found")

        // Instance method with locals: v0 is a scratch local, not `this` (that is p0).
        getDatabase.addInstructions(
            0,
            """
                const/4 v0, 0x0
                sput v0, Lw6/b;->openDBCount:I
            """.trimIndent(),
        )
    }
}
