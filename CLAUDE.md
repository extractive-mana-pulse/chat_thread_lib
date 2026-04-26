# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-activity Jetpack Compose Android app that renders a nested, collapsible comment thread UI. Kotlin 2.0.21, AGP 8.11.1, Compose BOM 2024.09.00, Material3, JDK 11. `compileSdk` / `targetSdk` 36, `minSdk` 24. Package: `com.example.collapsiblechatthread`.

## Build & run

Use the Gradle wrapper from the project root:

```bash
./gradlew assembleDebug            # build debug APK
./gradlew installDebug             # install on connected device/emulator
./gradlew :app:compileDebugKotlin  # fast compile check (no APK)
./gradlew test                     # local JVM unit tests
./gradlew connectedAndroidTest     # instrumented tests (needs device)
./gradlew :app:testDebugUnitTest --tests "com.example.collapsiblechatthread.ExampleUnitTest.addition_isCorrect"  # single test
./gradlew lint                     # Android Lint
./gradlew clean
```

Dependency versions are centralized in `gradle/libs.versions.toml` — update there, not in `app/build.gradle.kts`.

## Architecture

The feature is intentionally laid out as a demo of hand-drawn thread connector lines between a post and its nested comments. There is no data layer, ViewModel, or navigation — everything lives in `@Composable` functions under `app/src/main/java/com/example/collapsiblechatthread/`.

### Composable tree

- `MainActivity.onCreate` sets content to `HomeScreen` wrapped in `CollapsibleChatThreadTheme`, applies edge-to-edge and a status bar gradient via `StatusBarProtection`.
- `HomeScreen` (in `MainActivity.kt`) is the state owner. It holds every `expanded`/`height` flag for the whole thread and a big `Row` of `AvatarAndVerticalLine` + `MessageContent`.
- `MessageContent` renders the top post and, when expanded, delegates to `ExpandedReplies`, which lays out three child comments in order: `Comment1` → `Comment2` → `Comment3`.
- `Comment1` has 4 nested `ReplyItemContent` replies with an animated expand/collapse. `Comment2` is a leaf. `Comment3` mirrors `Comment1` with 2 replies.
- Shared building blocks: `MessageHeader` (username + category badge), `ReplyIconAndText` (reply affordance), `ReplyItemContent` (single nested reply).

### Thread-line drawing (the non-obvious part)

Connector lines between avatars and "Show N Replies" rows are **not** Compose layout primitives. Each comment draws its own lines in a `Modifier.drawBehind { ... }` on the outer `Box`:

1. Children (avatars, reply rows, the collapse `minus_circle` icon, each nested reply's avatar) call `.onGloballyPositioned { coords -> xyzCenter = coords.positionInParent() }` to publish their positions as `Offset` state.
2. `drawBehind` reads those `Offset`s and renders rounded-L `Path`s (`moveTo` → vertical `lineTo` → `quadraticBezierTo` for the corner → horizontal `lineTo`) with `Stroke(cap = Round, join = Round)`. See `Comment1.kt:81-156` for the canonical pattern.
3. Line endpoints are derived by hand: avatar bottom = `avatarCenter.y + 28.dp + 6.dp` (avatar size + gap), horizontal run uses `avatarX + 24.dp`, corner radius is `12.dp`. These magic numbers are duplicated across `Comment1.kt`, `Comment2.kt`, `Comment3.kt`, and the expanded branch of `AvatarAndVerticalLine` in `MainActivity.kt` — keep them in sync when changing avatar size or spacing.

### Height propagation

The main-post vertical line in `AvatarAndVerticalLine` animates to match whichever subtree is expanded. To do that, `HomeScreen` collects heights bottom-up via callbacks:

- `messageContentHeight`, `messageContainerHeight` — post and Comment1 container heights
- `repliesHeight`, `onComment2Height`, `repliesHeightComment3`, `messageHeightComment3` — per-comment measured heights
- `onReplyIsExpanded`, `onReplyIsExpandedComment3` — expansion flags for Comment1 and Comment3

Every comment Composable takes a `(Dp) -> Unit` callback and reports its own height through `onGloballyPositioned { coordinates -> onHeightCalculated(with(density) { coordinates.size.height.toDp() }) }`. The state is then threaded back down into `AvatarAndVerticalLine`, which feeds it into `animateDpAsState` to drive the tail line's length. This is why `MessageContent`, `ExpandedReplies`, and `AvatarAndVerticalLine` have long, parallel parameter lists — adding a new comment node means adding a new height/expansion pair and wiring it through all three.

Internal per-comment expansion (e.g. `Comment1.isExpanded`) uses `rememberSaveable`; the parent is notified via `onReplyIsExpanded` / `onCollapsedStateChanged` so the main-post line length stays correct.

### Resources

Post/comment copy lives in `app/src/main/res/values/strings.xml` (keys like `main_post_*`, `comment1_*`, `comment1_reply1_*`). Line/icon drawables are `plus_circle.xml`, `minus_circle.xml`, `reply.xml`. Theme/colors under `ui/theme/` (`CollapsibleChatThreadTheme`, `Color.kt`, `Type.kt`).