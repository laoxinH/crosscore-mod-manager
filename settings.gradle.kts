pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}
/*pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
    }
}*/
//pluginManagement {
//    repositories {
//        maven { url=uri ("https://jitpack.io") }
//        maven { url=uri ("https://maven.aliyun.com/repository/releases") }
////        maven { url 'https://maven.aliyun.com/repository/jcenter' }
//        maven { url=uri ("https://maven.aliyun.com/repository/google") }
//        maven { url=uri ("https://maven.aliyun.com/repository/central") }
//        maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { url=uri ("https://maven.aliyun.com/repository/public") }
//        google()
//        mavenCentral()
//        gradlePluginPortal()
//    }
//}
//dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
//    repositories {
//        maven { url=uri ("https://jitpack.io") }
//        maven { url=uri ("https://maven.aliyun.com/repository/releases") }
////        maven { url 'https://maven.aliyun.com/repository/jcenter' }
//        maven { url=uri ("https://maven.aliyun.com/repository/google") }
//        maven { url=uri ("https://maven.aliyun.com/repository/central") }
//        maven { url=uri ("https://maven.aliyun.com/repository/gradle-plugin") }
//        maven { url=uri ("https://maven.aliyun.com/repository/public") }
//        google()
//        mavenCentral()
//    }
//}

rootProject.name = "Mod Manager"
include(":app")
