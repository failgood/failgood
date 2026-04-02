plugins { id("buildgood.module") }

commonBuild { pitest {} }

dependencies { testImplementation(kotlin("test")) }
