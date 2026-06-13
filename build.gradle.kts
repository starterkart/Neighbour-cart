// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
}

tasks.register<Copy>("restoreProguard") {
    from("assets/.aistudio/app/proguard-rules.pro")
    into("app/")
}

tasks.register<Copy>("restoreRes") {
    from("assets/.aistudio/app/src/main/res")
    into("app/src/main/res")
}
