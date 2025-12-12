pluginManagement {
    repositories {
        google ()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.github.com/google-ai-edge/ai-edge-litert") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.pkg.github.com/google-ai-edge/ai-edge-litert") }
    }
}

rootProject.name = "Human Follower"
include(":app")
