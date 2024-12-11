import de.honoka.gradle.buildsrc.kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.charset.StandardCharsets

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    alias(libs.plugins.dependency.management)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin)
    /*
     * Lombok Kotlin compiler plugin is an experimental feature.
     * See: https://kotlinlang.org/docs/components-stability.html.
     */
    alias(libs.plugins.kotlin.lombok)
    alias(libs.plugins.kotlin.spring)
}

group = "de.honoka.bossddmonitor"
version = libs.versions.root.get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = sourceCompatibility
}

dependencyManagement {
    imports {
        mavenBom(libs.kotlin.bom.get().toString())
        mavenBom(libs.selenium.bom.get().toString())
    }
}

dependencies {
    kotlin(project)
    libs.versions.kotlin.coroutines
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.qqrobot.spring.boot.starter)
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.5")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.seleniumhq.selenium:selenium-java")
    "org.springframework.boot:spring-boot-configuration-processor".let {
        compileOnly(it)
        annotationProcessor(it)
    }
    libs.lombok.let {
        compileOnly(it)
        annotationProcessor(it)
        testCompileOnly(it)
        testAnnotationProcessor(it)
    }
    //Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    compileJava {
        options.run {
            encoding = StandardCharsets.UTF_8.name()
            compilerArgs.run {
                add("-parameters")
            }
        }
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = java.sourceCompatibility.toString()
    }
    
    bootJar {
        archiveFileName.set("${project.name}.jar")
    }

    test {
        useJUnitPlatform()
    }
}