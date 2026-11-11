plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "pl.zse.dziennik.data"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(libs.coroutines.core)
    implementation(libs.datastore)
}
