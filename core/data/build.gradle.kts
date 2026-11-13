plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}
android {
    namespace = "pl.zse.dziennik.data"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets["test"].resources.srcDir(project(":core:database").file("schemas"))
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(libs.coroutines.core)
    implementation(libs.room.ktx)
    implementation(libs.datastore)
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.runtime)
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    maxHeapSize = "1g"
    doFirst { layout.buildDirectory.dir("test-home").get().asFile.mkdirs() }
    systemProperty("user.home", layout.buildDirectory.dir("test-home").get().asFile.absolutePath)
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
}
