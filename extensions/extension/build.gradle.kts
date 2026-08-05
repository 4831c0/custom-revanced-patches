extension {
    name = "extensions/extension.rve"
}

android {
    namespace = "app.revanced.extension"
}

// Optional: enable strongly-typed references to host app classes by providing a local APK/JAR.
// Example:
// ./gradlew -Pmav412ApiJar=/absolute/path/to/vonatinfo-4.12-api.jar :extensions:extension:assembleDebug
val mav412ApiJar = providers.gradleProperty("mav412ApiJar").orNull
if (!mav412ApiJar.isNullOrBlank()) {
    dependencies {
        compileOnly(files(mav412ApiJar))
    }
}
