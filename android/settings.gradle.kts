pluginManagement {
    repositories {
        // google()은 Android · AndroidX · Google Android 묶음만(계획 9절).
        // com.android는 하위 묶음 + 그 묶음 자체(AGP가 쓰는 com.android:zipflinger · signflinger)
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google\\.android\\..*")
            }
        }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android(\\..*)?")
                includeGroupByRegex("com\\.google\\.android\\..*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "issuerank"
include(":app")
