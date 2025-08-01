# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MangaViewAndroid is a Korean manga/webtoon/novel viewer application that connects to a specific manga hosting service. The app supports downloading content, multiple viewer types, and includes a novel text viewer alongside traditional image-based manga viewers.

## Build Commands

- **Build Debug APK**: `./gradlew assembleDebug`
- **Build Release APK**: `./gradlew assembleRelease`
- **Clean Build**: `./gradlew clean`
- **Install Debug**: `./gradlew installDebug`

The build system automatically generates version codes based on date (format: YYMMdd + 2112000000) and creates APK files named `mangaViewer_[versionCode].apk`.

## Architecture Overview

### Core Content Types and Viewer Routing

The app handles three content types via `MTitle.java` constants:
- `base_comic = 1` - Traditional manga (images)
- `base_webtoon = 2` - Webtoons (vertical scroll images) 
- `base_novel = 3` - Text-based novels

**Viewer Selection Logic** (`Utils.viewerIntent()`):
- Novels → `ViewerActivity4` (text viewer with manga-like UI)
- Comics/Webtoons → User preference determines viewer:
  - `ViewerActivity` (scroll viewer, type 0)
  - `ViewerActivity2` (touch viewer, type 1) 
  - `ViewerActivity3` (webtoon viewer, type 2)

### Key Navigation Flow

1. **Search** → Content List → Episode List → Viewer
2. **Main Tabs**: Main/Search/Recent/Favorites/Downloaded
3. **Episode Activity** acts as content hub, routing to appropriate viewers via `Utils.viewerIntent()`

### Critical Architectural Components

**Content Management**:
- `Manga.java` - Represents episodes/chapters with metadata
- `Title.java` - Represents series/works with episode lists
- `NovelPage.java` - Handles novel text content parsing and rendering
- `Search.java` - Handles search functionality across all content types

**Network Layer**:
- `CustomHttpClient.java` - Handles HTTP requests with custom headers
- Built-in captcha handling and retry logic
- Supports both online streaming and offline downloaded content

**UI Framework**:
- Fragment-based main navigation (`MainMain.java`, `MainSearch.java`)
- CoordinatorLayout + AppBarLayout for viewer UIs with toolbar hiding
- RecyclerView adapters for content lists (`TitleAdapter`, `EpisodeAdapter`)

### Novel Viewer Implementation

`ViewerActivity4` provides a text-based reading experience with:
- Long-press UI toggle (matches manga viewer behavior)
- Font size adjustment
- Text selection and clipboard copying
- Full-screen immersive mode
- Status bar aware layout positioning

**Novel Content Flow**:
1. Search with base_novel type → Episode list
2. Episode activity routes to ViewerActivity4 via Utils.viewerIntent()
3. NovelPage.fetch() retrieves and parses text content
4. Text displayed in ScrollView with manga viewer UI patterns

### Debugging and Testing

- `DebugActivity.java` provides development tools
- Novel viewer testing: Use "소설 뷰어 테스트" button (loads test content with ID 999999)
- Test content bypasses network calls for reliable debugging

### Important Files for Viewer Extensions

When adding new viewer types or content formats:
- Update `Utils.viewerIntent()` routing logic
- Add constants to `MTitle.java` if new content types needed
- Register new activities in `AndroidManifest.xml`
- Follow existing UI patterns (AppBarLayout + toolbar hiding)

### Content Parsing

The app expects specific HTML structure from the target service:
- Manga: Image URLs extracted from page responses
- Novels: Text content extracted via CSS selectors (`.view-content`, `.content`, etc.)
- Episode lists: Parsed from structured HTML responses

### Preferences and Settings

`Preference.java` manages user settings including:
- Viewer type selection (affects Utils.viewerIntent routing)
- Theme settings (dark/light)
- Download preferences
- Base URL configuration