plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.10"
    id("org.jetbrains.intellij") version "1.8.0"
}

group = "chen.yyds"
version = "1.11"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2021.3.3")
    type.set("PC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versionsclass ResOcr:
    //    prob: float
    //    text: str
    //    x1: float
    //    y1: float
    //    x2: float
    //    y2: float
    //    x3: float
    //    y3: float
    //    x4: float
    //    y4: float
    //
    //    def __init__(self, prob: float, text: str, x1: float, y1: float, x2: float, y2: float, x3: float, y3: float,
    //                 x4: float, y4: float):
    //        self.prob = prob
    //        self.text = text
    //        self.x1 = x1
    //        self.y1 = y1
    //        self.x2 = x2
    //        self.y2 = y2
    //        self.x3 = x3
    //        self.y3 = y3
    //        self.x4 = x4
    //        self.y4 = y4
    //
    //    def __repr__(self):
    //        return '{{ prob={},text="{}",x1={},y1={},x2={},y2={},x3={},y3={},x4={},y4={} }}'.format(self.prob, self.text,
    //                                                                                                self.x1, self.y1,
    //                                                                                                self.x2, self.y2,
    //                                                                                                self.x3, self.y3,
    //                                                                                                self.x4, self.y4)
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("223.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
