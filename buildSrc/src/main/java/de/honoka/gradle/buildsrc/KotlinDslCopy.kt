package de.honoka.gradle.buildsrc

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.named

fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? = run {
    add("implementation", dependencyNotation)
}

val NamedDomainObjectContainer<Configuration>.implementation: NamedDomainObjectProvider<Configuration>
    get() = named<Configuration>("implementation")