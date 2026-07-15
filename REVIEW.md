# Arcedex — Comprehensive Code Review & Audit Report

> **Date:** 2026-07-15 | **Repo:** [FriendlyButFire/Arcedex](https://github.com/FriendlyButFire/Arcedex) | **Branch:** master
> **Reviewer:** Arena AI Agent

---

## Phase 1 — Stack & Architecture Inventory

### Build Stack
- **Language:** Kotlin 100% (no Java source files)
- **Gradle & Toolchain:** Gradle 9.6.1 via wrapper, AGP 9.2.1, Kotlin 2.4.0, KSP 2.3.9
- **DSL & Dependency Management:** Groovy `build.gradle` + Version Catalog `gradle/libs.versions.toml`
- **SDK Targets:** `compileSdk = 37`, `targetSdk = 37`, `minSdk = 23` (Android 16 / Baklava compatible)
- **JVM Toolchain:** Java 17 (`kotlin { jvmToolchain(17) }`)
- **Modules:** Single `:app` module
- **UI:** Jetpack Compose BOM `2026.06.01`, Material3, Activity Compose
- **Storage:** Room 2.8.4 (KSP code generation)
- **Threading:** Coroutines 1.11.0 + Flow (`viewModelScope`, `Dispatchers.Default`, `SupervisorJob` in Application)

### Architecture Map
- **Pattern:** MVVM (ViewModel exposing StateFlows to Compose UI)
- **DI:** Manual dependency injection — lazy `database` + `repository` in `ArcedexApplication`, custom `PokeResearchViewModelFactory`
- **Navigation:** Single Activity Scaffold with status & navigation bar padding
- **State Management:** `PokeResearchViewModel` exposes reactive `StateFlow` streams (`researchTasks`, `filteredPokedex`, `researchProgress`, `pokesort`, `inSearchMode`, `searchedText`, `userPoints`, `pokemonToResearchTasks`, `hideFilter`, `selectedArea`, `selectedCategory`, `selectedCategoryType`)
- **Data Layer:**
  - Entity: `PokeResearch` (Room entity with unique composite index on `(name, task)`)
  - DAO: `PokeResearchDao` providing suspend functions, transactional bulk inserts (`insertAll`), and `Flow<List<PokeResearch>>`
  - Static Data: `PokeResearchData` (~19k LOC static task definitions), `Pokedex`, `PokeMovesData`, `PokeAreaData`, `PokeTranslateData`
- **Domain & Utilities:** `Pokemon`, `ResearchProgress`, `PokeMove`, `PokeTranslate`, enums (`PokeSort`, `HisuiArea`, `HideFilter`, `TaskCategory`), `Utility.kt` (rank math, category parsing, O(1) translation lookup), `BackupUtil.kt` (JSON + gzip + Base64 progress serialization)

---

## Phase 2 — Detailed Audit Across 11 Key Areas

### 1. Lint & Static Analysis
- **Status:** Initial run produced 41 errors and 255 warnings (296 total issues).
- **Actions Taken:**
  - ✅ **41 MissingTranslation Errors:** Resolved by supplying all missing Japanese strings in `values-ja/strings.xml`.
  - ✅ **246 IconLocation Warnings:** Resolved by moving densityless bitmap PNGs from `drawable/` to `drawable-nodpi/`.
  - ✅ **3 UnusedResources Warnings:** Removed dead color `dark_burgundy`, obsolete `ic_launcher_foreground.xml` duplicate, and unused string `red_quote`.
  - ✅ **2 MonochromeLauncherIcon Warnings:** Added `<monochrome>` vector drawable for adaptive launcher icons.
  - ✅ **1 PluralsCandidate Warning:** Converted `backup_restored_message` to `<plurals>` in English and Japanese.
- **Current Metric:** 0 errors, 3 non-blocking version info warnings.

---

### 2. Build & Config
- **Actions Taken:**
  - ✅ **Dependency Cleanup:** Replaced `api` with `implementation` for coroutines dependencies and eliminated deprecated `kotlin-stdlib-jdk7`.
  - ✅ **AGP 9.x Packaging DSL:** Migrated legacy `packagingOptions` to `packaging.resources`.
  - ✅ **R8 Code & Resource Shrinking:** Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `app/build.gradle` for `release` builds. Added `.debug` applicationIdSuffix for `debug` builds.
  - ✅ **ProGuard Rules:** Configured `app/proguard-rules.pro` to keep Room entities, models, and DAOs.
  - ✅ **Version Bumps:** Updated `versionCode` to 12 and `versionName` to `1.2.1`.
- **Deferred / Won't Do:**
  - ❌ *Product Flavors:* Single-APK app; flavor setup adds unnecessary build complexity.

---

### 3. Project / Module Structure
- **Actions Taken:**
  - ✅ **Monolith Decomposition:** Split 1200+ LOC `MainActivity.kt` into clean UI packages:
    - `ui/screens/ArcedexApp.kt`
    - `ui/components/TopBar.kt`
    - `ui/components/BottomBar.kt`
    - `ui/components/PokedexList.kt`
    - `ui/components/PokemonRow.kt`
    - `ui/components/TaskRow.kt`
    - `ui/components/BackupDialog.kt`
  - ✅ **Logic Decoupling:** Extracted business logic helpers (`taskCategoryTypeOf`) from composable UI into `Utility.kt`.
- **Deferred / Won't Do:**
  - ❌ *Multi-Module Breakdown (`:core`, `:feature`):* Over-engineering for a compact offline app; single module keeps build times fast and setup minimal.

---

### 4. Code Structure & Consistency
- **Actions Taken:**
  - ✅ **Immutable Encapsulation:** Updated `pokemonToResearchTasks` to expose immutable `Map<String, List<PokeResearch>>`.
  - ✅ **Recomposition Side-Effect Guard:** Wrapped `setLanguage(language)` inside `LaunchedEffect(language)` in `ArcedexApp`.
  - ✅ **O(1) Lookup Optimization:** Added `jpTranslationMap` lazy map in `PokeTranslateData` (replacing O(N) linear scans across 600+ entries) and `pokemonByName` in `Pokedex`.
- **Deferred / Won't Do:**
  - ❌ *MVI Architecture Rewrite:* MVVM with Flow is idiomatic, lightweight, and effective for this application.

---

### 5. Performance
- **Actions Taken:**
  - ✅ **Background Thread Filtering:** Added `filteredPokedex: StateFlow<List<Pokemon>>` in `PokeResearchViewModel` combining filters and executing on `Dispatchers.Default` via `flowOn(Dispatchers.Default)`.
  - ✅ **Background Search & Progress Calc:** `searchPokedex()`, `searchClear()`, `calcProgress()`, and `setSort()` process on `Dispatchers.Default`.
  - ✅ **LazyColumn Stability:** Added `key = { it.name }` to `LazyColumn` items in `Pokedex`.
  - ✅ **StrictMode Audit:** Configured StrictMode thread and VM violation monitoring in `BuildConfig.DEBUG`.
- **Deferred / Won't Do:**
  - ⏭️ *Coil Async Image Migration:* Local vector and nodpi drawables render instantly; Coil library deferred until dynamic image loading is required.

---

### 6. Concurrency & Lifecycle
- **Actions Taken:**
  - ✅ **Coroutines Scoping:** Scoped background processing explicitly using `viewModelScope` and `Dispatchers.Default`.
  - ✅ **Lifecycle-Aware State Collection:** Used `collectAsStateWithLifecycle()` across Compose UI components.
- **Deferred / Won't Do:**
  - ⏭️ *Card Expansion State Hoisting:* Simple UI expansion toggles currently use `remember`; fine for local UI interaction.

---

### 7. Data Layer & Data Structures
- **Actions Taken:**
  - ✅ **Database Composite Index:** Added `@Index(value = ["name", "task"], unique = true)` on `PokeResearch` entity.
  - ✅ **Bulk Insertion:** Implemented `@Insert suspend fun insertAll(List)` on `PokeResearchDao` and updated database population callback to execute in a single bulk transaction.
  - ✅ **Suspend DAO Methods:** Updated `getCount()` to `suspend fun getCount(): Int`.
- **Deferred / Won't Do:**
  - ❌ *Database Encryption (SQLCipher):* Unnecessary overhead for non-PII local companion data.

---

### 8. Security & Privacy
- **Actions Taken:**
  - ✅ **Backup Exclusion Rules:** Explicitly set `allowBackup="false"` in `AndroidManifest.xml` and supplied `xml/backup_rules.xml` and `xml/data_extraction_rules.xml` to prevent automatic unencrypted DB cloud backups.
  - ✅ **Standard JVM Serialization:** Updated `BackupUtil.kt` to use standard `java.util.Base64` for safe copy-paste progress string serialization.

---

### 9. Correctness & Test Coverage
- **Actions Taken:**
  - ✅ **Unit Test Suite:** Implemented pure JVM unit tests in `app/src/test/java/jzam/arcedex/`:
    - `UtilityTest.kt`: Validates Pokemon ID formatting, research rank calculations, total points formulas, task category regex matching for all 16 categories, defeat type parsing, move type lookups, and Japanese translations.
    - `BackupUtilTest.kt`: Tests progress export, parsing, non-zero progress filtering, and validation error throwing.
    - `PokeResearchViewModelTest.kt`: Tests ViewModel state initialization, 3-state filter cycling, and region/category selection.
  - ✅ **Input Validation:** Trimmed search input whitespace and treated blank searches as clear commands.

---

### 10. Accessibility & Resources
- **Actions Taken:**
  - ✅ **String Externalization:** 100% of UI strings externalized to `values/strings.xml` and `values-ja/strings.xml`.
  - ✅ **Resource Deduplication:** Deleted 4 duplicate raster `double_points.png` files across `hdpi`, `mdpi`, `xhdpi`, `xxhdpi`, retaining modern vector `drawable-anydpi-v24/double_points.xml`.

---

### 11. Project Hygiene
- **Actions Taken:**
  - ✅ **Developer Documentation:** Added comprehensive `Development Setup` instructions in `README.md` detailing JDK 17, Android SDK 37, Gradle commands, and testing procedures.
  - ✅ **Version Control Ignores:** Expanded `.gitignore` to cover `/app/build`, `**/build/`, `.kotlin/`, `.android/`, keystores, etc.
  - ✅ **Open Source Licensing:** Added `LICENSE` file (MIT License).

---

## Prioritized Checklist & Status Summary

| Item | Description | Priority | Status | Rationale |
| :--- | :--- | :---: | :---: | :--- |
| **F1.3** | Fix Lint MissingTranslation, IconLocation, UnusedResources, Monochrome, Plurals | P0 | ✅ Done | Resolved 296 lint findings down to 0 errors |
| **F2.5** | Clean up dependencies (`api` $\to$ `implementation`, drop legacy stdlib artifact) | P0 | ✅ Done | Avoids transitive dependency leakage |
| **F4.1** | Wrap `setLanguage` in `LaunchedEffect` | P0 | ✅ Done | Prevents infinite recomposition side-effects |
| **F7.1-7.3** | DB Hygiene: composite unique index, bulk `insertAll`, suspend `getCount()` | P0 | ✅ Done | Eliminates cold-start DB lock & duplicate rows |
| **F8.1** | Disable automatic unencrypted cloud backups with rules | P0 | ✅ Done | Enhances data privacy and backup predictability |
| **F5.2** | Add stable key `{ it.name }` to `LazyColumn items` | P0 | ✅ Done | Prevents full list recomposition on item expansion |
| **F5.6** | Enable `StrictMode` in debug builds | P0 | ✅ Done | Detects disk/network I/O on UI thread during development |
| **F9.2** | Validate & trim search input | P0 | ✅ Done | Fixes blank search edge case matching all tasks |
| **F4.2/4.6** | Background state flow filtering (`filteredPokedex`) & $O(1)$ HashMap lookups | P1 | ✅ Done | Offloads filtering from UI thread to `Dispatchers.Default` |
| **F1.2/3.2** | Decompose monolithic `MainActivity.kt` into `ui/screens` and `ui/components` | P1 | ✅ Done | Improves code maintainability and package separation |
| **F9.1** | Add unit test suite (`UtilityTest`, `BackupUtilTest`, `ViewModelTest`) | P1 | ✅ Done | Locks in critical math, parsing, and serialization behavior |
| **F2.4** | Enable R8 minification & resource shrinking | P1 | ✅ Done | Reduces release APK footprint |
| **F5.3/10.7**| Clean up redundant raster drawables (`double_points.png`) | P1 | ✅ Done | Eliminates duplicate assets in favor of vector XML |
| **F11.1-11.5**| Add setup docs, MIT `LICENSE`, expanded `.gitignore`, bump version to 1.2.1 | P1 | ✅ Done | Improves repository hygiene and dev experience |
| **F1.1** | Add ktlint / detekt Gradle plugins | P2 | ⏭️ Deferred | Non-blocking static formatting checks |
| **F5.3** | Migrate local drawables to Coil `AsyncImage` | P2 | ⏭️ Deferred | Existing vector/nodpi assets load efficiently |
| **F6.6** | Hoist card expansion state to ViewModel / `rememberSaveable` | P2 | ⏭️ Deferred | Standard rotation reset is acceptable for current scope |
| **--** | Multi-module split (`:core`, `:feature`) | P3 | ❌ Won't Do | Over-engineering for a single-screen offline app |
| **--** | MVI / Redux architecture rewrite | P3 | ❌ Won't Do | MVVM with Flow is clean, effective, and standard |
| **--** | SQLCipher Database Encryption | P3 | ❌ Won't Do | Adds native binary size for non-sensitive public data |
