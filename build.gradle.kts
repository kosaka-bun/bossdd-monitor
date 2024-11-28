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
    }
}

dependencies {
    kotlin(project)
    libs.versions.kotlin.coroutines
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.honoka.framework.utils)
    implementation("cn.hutool:hutool-all:5.8.18")
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.5")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.hibernate.validator:hibernate-validator")
    implementation("org.springframework.boot:spring-boot-configuration-processor".also {
        annotationProcessor(it)
    })
    compileOnly(libs.lombok.also {
        annotationProcessor(it)
        testCompileOnly(it)
        testAnnotationProcessor(it)
    })
    //Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks {
    compileJava {
        options.encoding = StandardCharsets.UTF_8.name()
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