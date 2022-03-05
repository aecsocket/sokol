<div align="center">

<a href="https://aecsocket.github.io/sokol"><h1>Sokol</h1></a> <!-- TODO add banner -->

`2.0.1-SNAPSHOT`:
[![build](https://github.com/aecsocket/sokol/actions/workflows/build.yml/badge.svg)](https://github.com/aecsocket/sokol/actions/workflows/build.yml)

</div>

Platform-agnostic data-driven item framework, allowing server owners to configure items
and plugin developers to register custom item behaviour.

# Features

- [x] Node-based tree structure for items - items consist of a tree of components
- [x] Features drive component behaviour, and many features can be added to one component
- [x] Tree-wide stat function, making it easy to modify properties throughout the tree
- [x] Rules determining when stats or other functions apply to nodes
- [x] Extendable and customisable, allowing developers to write their own features
- [x] All licensed under GNU GPL v3 - free and open source

# Usage

## Downloads

### Dependencies

<details open>
<summary>Paper</summary>

* [Java >=17](https://adoptium.net/)
* [Paper >=1.18.2](https://papermc.io/)

</details>

### [Stable Releases](https://github.com/aecsocket/sokol/releases)

### [Latest Snapshots](https://github.com/aecsocket/sokol/actions/workflows/build.yml)

**Note: you need to download the `-all` jar.** 

## Packages

Using any package from the GitHub Packages registry requires you to
authorize with GitHub Packages.

### To create a token for yourself:

1. Visit https://github.com/settings/tokens/new
2. Create a token with only the `read:packages` scope
3. Save that token as an environment variable, `GPR_TOKEN`
4. Save your GitHub username as another environment variable, `GPR_ACTOR`

### To use a token in a workflow run:

Include the `github.actor` and `secrets.GITHUB_TOKEN` variables in the `env` block of your step:

```yml
- name: "Build"
  run: ./gradlew build
  env:
    GPR_ACTOR: "${{ github.actor }}"
    GPR_TOKEN: "${{ secrets.GITHUB_TOKEN }}"
```

### To use the token in your environment variable:

Use the `GPR_ACTOR`, `GPR_TOKEN` environment variables in your build scripts:

```kotlin
// authenticating with the repository
credentials {
    username = System.getenv("GPR_ACTOR")
    password = System.getenv("GPR_TOKEN")
}
```

**Note: Never include your token directly in your build scripts!**

Always use an environment variable (or similar).

<details>
<summary>Maven</summary>

### [How to authorize](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

#### In `~/.m2/settings.xml`

```xml
<servers>
  <server>
    <id>github-sokol</id>
    <username>[username]</username>
    <password>[token]</password>
  </server>
</servers>
```

#### In `pom.xml`

Repository
```xml
<repositories>
  <repository>
    <id>github-sokol</id>
    <url>https://maven.pkg.github.com/aecsocket/sokol</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>
```

Dependency
```xml
<dependencies>
  <dependency>
    <groupId>com.github.aecsocket</groupId>
    <artifactId>sokol-[module]</artifactId>
    <version>[version]</version>
  </dependency>
</dependencies>
```

</details>

<details>
<summary>Gradle</summary>

The Kotlin DSL is used here.

### [How to authorize](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)

Repository
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/aecsocket/sokol")
        credentials {
            username = System.getenv("GPR_ACTOR")
            password = System.getenv("GPR_TOKEN")
        }
    }
}
```

Dependency
```kotlin
dependencies {
    compileOnly("com.github.aecsocket", "sokol-[module]", "[version]")
}
```

</details>

# Documentation

### [Javadoc](https://aecsocket.github.io/sokol/docs)

### [Wiki](https://github.com/aecsocket/sokol/wiki)
