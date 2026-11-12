pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Luma"
include(":app", ":core:model", ":core:database", ":core:data", ":core:network", ":core:designsystem", ":feature:dashboard")
