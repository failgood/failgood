plugins { id("com.android.library") }

android {
    namespace = "failgood.android"
    compileSdk = 35

    defaultConfig { minSdk = 26 }

    testOptions { unitTests.all { it.useJUnitPlatform() } }

    buildFeatures { buildConfig = false }
}

dependencies { api(project(":failgood")) }
