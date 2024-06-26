kmpLibrary {
    dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    }

    targets {
        jvm {
            jdkVersion = 14

            dependencies {
                api("org.apache.commons:commons-lang3:3.14.0")
            }
        }
        js {
            environment = "node"

            dependencies {
                implementation("com.squareup.sqldelight:runtime:1.5.5")
            }
        }
    }
}
