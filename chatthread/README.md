# CollapsibleChatThread

A small Jetpack Compose library that renders a collapsible, infinitely-nestable comment tree with
file-tree-style connector lines. Driven by plain data — no manual height plumbing, no hard-coded
reply counts. Scales to thousands of comments because it's backed by a `LazyColumn`.

![demo](docs/screenshot.png)

- **Recursive data**: each `ThreadComment` can carry any depth of `replies`.
- **Lazy**: rows render on demand via `LazyColumn`; cost scales with what's visible.
- **Self-contained rows**: connectors are drawn per-row via a `Modifier`, so rows never measure
  their siblings or parent. No `onGloballyPositioned` round-trips.
- **Skinnable**: every color/dimension/icon comes from `ThreadStyle.Default.copy(...)`.
- **Config-change safe**: expanded state is persisted via `rememberSaveable`.

---

## 1. Add the dependency

The library is published via [JitPack](https://jitpack.io).

**Step 1 — add the JitPack repository** to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2 — add the implementation dependency** to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.YOUR_GITHUB_USERNAME:chatthread:1.0.0")
}
```

Replace `YOUR_GITHUB_USERNAME` with the GitHub account that owns the repo and `1.0.0` with a
[release tag](https://github.com/YOUR_GITHUB_USERNAME/CollapsibleChatThread/releases).

Sync Gradle. You can now `import com.example.chatthread.*` from your app.

---

## 2. Minimum working example

```kotlin
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import com.example.chatthread.CollapsibleChatThread
import com.example.chatthread.ThreadComment
import com.example.chatthread.rememberThreadState

class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val comments = listOf(
                ThreadComment(
                    id = "root",
                    author = "Alice",
                    avatarBackground = Color(0xFF75FABF),
                    timestamp = "just now",
                    body = "Hello world",
                    replies = listOf(
                        ThreadComment(
                            id = "r1",
                            author = "Bob",
                            avatarBackground = Color(0xFFFFC368),
                            timestamp = "5m",
                            body = "Hi Alice",
                        ),
                    ),
                ),
            )
            val state = rememberThreadState(initiallyExpanded = setOf("root"))
            CollapsibleChatThread(comments = comments, state = state)
        }
    }
}
```

That's it. No icons, no custom style — the defaults render a dark-themed, text-only thread.

---

## 3. The five things you'll want to customize

### 3.1 Icons

Icons are declared as `@Composable () -> Painter?` on `ThreadStyle`. Return `painterResource(...)`
to show an icon, or `null` to render a text-only chip.

```kotlin
val style = ThreadStyle.Default.copy(
    expandIcon   = { painterResource(R.drawable.plus_circle)  },
    collapseIcon = { painterResource(R.drawable.minus_circle) },
    replyIcon    = { painterResource(R.drawable.reply)        },
)
```

### 3.2 Colors and dimensions

Every geometric value drives both layout *and* connector drawing, so changing one value keeps the
tree lines aligned. For example, a chunkier look:

```kotlin
val chunky = ThreadStyle.Default.copy(
    avatarSize = 36.dp,
    indentPerLevel = 40.dp,
    connectorColor = Color(0xFF4A5568),
    connectorStrokeWidth = 2.5.dp,
    connectorCornerRadius = 16.dp,
)
```

### 3.3 Typography

All text styles are `TextStyle` fields on `ThreadStyle`. Override any of them:

```kotlin
ThreadStyle.Default.copy(
    authorStyle = MaterialTheme.typography.titleSmall,
    bodyStyle   = MaterialTheme.typography.bodyMedium,
)
```

### 3.4 Hoisted expansion state

`ThreadState` is a plain holder you can drive from outside the composable — great for "Expand all"
or deep-linking into a specific comment:

```kotlin
val state = rememberThreadState()
Button(onClick = { comments.forEach { state.expand(it.id) } }) {
    Text("Expand all roots")
}
CollapsibleChatThread(comments = comments, state = state)
```

The state survives configuration changes via its `Saver`.

### 3.5 Reply click handler

The library does not ship a composer. `onReplyClick` hands you the tapped comment; the caller
opens whatever UI they want:

```kotlin
CollapsibleChatThread(
    comments = comments,
    onReplyClick = { parent -> openComposerFor(parent.id) },
)
```

---

## 4. Building your comment tree

Rules:

- `ThreadComment.id` must be unique across the tree. It's used as the `LazyColumn` item key *and*
  as the expansion-state key. Reusing an id between two nodes produces undefined behavior.
- `replies` is just `List<ThreadComment>` — there is no depth cap.
- `avatarInitial` defaults to the first char of `author`; override it if you need localized
  initials, emoji, etc.
- Optional fields (`channel`, `title`, `tags`) render only when present — leave them null/empty
  for a plain reply.

If your data comes from a flat table (parent_id → comment), build the tree once before handing it
to the composable:

```kotlin
fun buildTree(flat: List<Row>): List<ThreadComment> {
    val byParent = flat.groupBy { it.parentId }
    fun build(id: String?): List<ThreadComment> = byParent[id].orEmpty().map { row ->
        ThreadComment(
            id = row.id,
            author = row.author,
            avatarBackground = row.color,
            timestamp = row.timestamp,
            body = row.body,
            replies = build(row.id),
        )
    }
    return build(null)
}
```

Wrap the result in `remember(flat) { buildTree(flat) }` so the tree is only rebuilt when the
source changes.

---

## 5. Edge-to-edge / system bars

`CollapsibleChatThread` exposes a `contentPadding: PaddingValues` parameter that is forwarded
straight to the internal `LazyColumn`. Pass the system-bar insets through it when you want
content to scroll *under* translucent bars while staying tappable at rest:

```kotlin
val insets = WindowInsets.systemBars.asPaddingValues()
CollapsibleChatThread(
    comments = comments,
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(
        top = insets.calculateTopPadding(),
        bottom = insets.calculateBottomPadding(),
    ),
)
```

See `app/src/main/java/com/example/collapsiblechatthread/MainActivity.kt` for a working example
that adds a status-bar gradient overlay as well.

---

## 6. Performance notes

- The flattening pass (`flattenThread`) runs inside a `remember(comments, state.expandedIds)`, so
  it re-runs only when the source list or the expanded-set changes. Expanding/collapsing one id
  is a small incremental change; the `LazyColumn` diff keeps scroll state stable.
- Connectors are drawn with `Modifier.drawBehind` — no extra composables, no layout measurements.
- If you have *very* deep trees (hundreds of levels), the width added by `indentPerLevel * depth`
  will eventually push content off-screen. That's a product decision, not a library limit; cap
  depth in your UI or stop indenting past some level by post-processing `ThreadComment.replies`.

---

## 7. API at a glance

Public surface lives in `com.example.chatthread`:

| Symbol | Purpose |
| --- | --- |
| `CollapsibleChatThread(...)` | Main composable. |
| `ThreadComment` | Data class for one comment; holds `replies: List<ThreadComment>`. |
| `ThreadTag` | Colored badge used on a comment. |
| `ThreadStyle` | All visual configuration. `ThreadStyle.Default` + `.copy(...)` for tweaks. |
| `ThreadState` | Holder for the set of expanded ids; has `toggle`, `expand`, `collapse`. |
| `rememberThreadState(initiallyExpanded)` | Creates a saveable `ThreadState`. |

Everything under `com.example.chatthread.internal` is an implementation detail and may change
without notice.
