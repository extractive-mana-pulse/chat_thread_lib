# CollapsibleChatThread

A Jetpack Compose library that renders a collapsible, infinitely-nestable comment thread with
file-tree-style connector lines. Driven by plain data — no manual height plumbing, no hard-coded
reply counts. Backed by `LazyColumn` so it scales to thousands of comments.

See **[chatthread/README.md](chatthread/README.md)** for the full installation guide, usage
examples, and API reference.

---

## Quick install

Add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency to `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.YOUR_GITHUB_USERNAME:chatthread:1.0.0")
}
```

Replace `YOUR_GITHUB_USERNAME` with the GitHub account that owns the repo and `1.0.0` with a
[release tag](https://github.com/YOUR_GITHUB_USERNAME/CollapsibleChatThread/releases).

---

## Minimum example

```kotlin
val comments = listOf(
    ThreadComment(
        id = "root", author = "Alice",
        avatarBackground = Color(0xFF75FABF),
        timestamp = "just now", body = "Hello world",
        replies = listOf(
            ThreadComment(
                id = "r1", author = "Bob",
                avatarBackground = Color(0xFFFFC368),
                timestamp = "5m", body = "Hi Alice",
            ),
        ),
    ),
)
val state = rememberThreadState(initiallyExpanded = setOf("root"))
CollapsibleChatThread(comments = comments, state = state)
```
