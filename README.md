# LECTRA — Smart University Study Companion

**LECTRA** is a native Android application built with Kotlin and Jetpack Compose designed specifically for university and graduate students. It transforms dense lecture PDF slide decks into organized syllabi, coordinates weekly revision timetables to avoid scheduling conflicts, and reinforces conceptual retention through active-recall flashcards and topical practice quizzes.

---

## 🏛️ Campus Warm Amber & Deep Espresso Design

LECTRA features a bespoke university library atmosphere:
- **Palette**: Warm Amber (`#D97706`), Warm Honey (`#F59E0B`), Terracotta Cinnamon (`#B45309`), Sage Forest (`#2D5A46`), and Deep Espresso Slate (`#150F0C`).
- **Parchment Surface Tones**: High-contrast, glare-reducing reading cards tailored for prolonged academic focus.
- **Edge-to-Edge Fluidity**: Full window inset compliance (`safeDrawing`, `navigationBars`), M3 tonal elevations, and 48dp accessible touch targets.

---

## 🚀 Key Functional Modules

### 1. Home Dashboard (Lecture Timetable Priority)
- **Today's Live Classes in Chronological Order**: Displays all classes scheduled for the current day with course code, full title, exact start and end times, and venue location.
- **Embedded Class Assignments**: Displays assignments directly inside their corresponding class cards, highlighted with priority badges, one-tap completion toggles, and actual deadlines.
- **Real-Time Dynamic Expiration**: Classes automatically transition out of upcoming/active status once their scheduled end time passes.
- **Automatic Day Transition & Next Scheduled Classes**: Dynamically determines the current day and time from the device. When today's classes conclude (or on days without classes), it scans forward to find the next actual scheduled lecture day rather than fabricating a fake timetable.
- **Academic Progress & Revision Hub**: Located directly below the timetable:
  - Today's study progress (sessions completed vs planned, minutes spent).
  - Course syllabus progress (mastered topics percentage bars per course).
  - Notes requiring review (unmastered topics with key takeaways).
  - Unread lecture materials (PDF slide decks with unread page counts).
  - Pending practice questions (unanswered topical quiz questions).
  - Upcoming assessments (pending assignments sorted by deadline).
  - Weak revision areas (flashcards rated "Again" or "Hard" with targeted drill).

### 2. My Courses & Syllabus Matrix
- **Course Management**: Create, edit, and organize courses with course codes, lecturers, color badges, and target weekly study hours.
- **Hierarchical Units & Topics**: Subdivide courses into syllabus units and specific topics with mastery tracking.
- **SAF Document Management**: Uses Android's Storage Access Framework (`ActivityResultContracts.OpenDocument`) to link PDFs locally without uploading private university documents to third parties.

### 3. Study Planner & Timetable
- **Weekly Schedule Grid**: 7-day selector with active session dots and progress counters.
- **Overlap Conflict Prevention**: Automatic detection prevents accidental overlapping study blocks on the same day.
- **Task & Deadline Tracker**: Priority-ranked assignment deadlines (`HIGH`, `MEDIUM`, `LOW`).

### 4. Revision Hub (Active Recall)
- **Spaced Repetition Flashcards**: Interactive 3D flip cards with 4-tier recall grading (*Again*, *Hard*, *Good*, *Easy*) calculating spaced intervals.
- **Topical Practice Quizzes**: Multiple-choice drills with instant answer reveal, explanation cards, and comprehensive score breakdown.
- **AI-Ready Engine**: Prepared abstraction layer for Gemini-powered slide summarization and automatic card generation upon key activation.

### 5. Profile & Settings
- **Storage Inventory**: Real-time count of local SQLite database entities and document references.
- **Privacy Disclosures**: Transparent confirmation of zero-leak local processing.
- **Academic Demo Dataset**: One-tap reset to pre-populate realistic university demo courses (*CS 301*, *BIO 210*, *MATH 240*).

---

## 🛠️ Project Structure

```
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI workflow (JDK 17 + Gradle assembleDebug)
├── app/
│   ├── build.gradle.kts            # App module build configuration & Room KSP
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Permissions & launcher activity
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt # Compose container, navigation, and dialogs
│   │   │   │   ├── data/
│   │   │   │   │   ├── ai/         # AiStudyService abstraction
│   │   │   │   │   ├── local/      # Room database, DAOs, and database seeder
│   │   │   │   │   ├── model/      # Room entity data classes
│   │   │   │   │   └── repository/ # Repository layer & SAF URI resolver
│   │   │   │   └── ui/
│   │   │   │       ├── LectraViewModel.kt
│   │   │   │       ├── components/ # TopBar, BottomBar, Dialogs
│   │   │   │       ├── navigation/ # Destination enums
│   │   │   │       ├── screens/    # Home, Courses, Planner, Revision, Settings
│   │   │   │       └── theme/      # Warm Amber & Espresso Theme, Color, Type
│   │   │   └── res/
│   │   │       ├── drawable/       # Adaptive vector launcher icon & background
│   │   │       └── mipmap-*/       # High-density launcher PNGs
│   │   └── test/                   # Local JVM unit tests
├── gradle/
│   ├── libs.versions.toml          # Gradle version catalog
│   └── wrapper/
├── build.gradle.kts                # Top-level build file
├── settings.gradle.kts             # Settings configuration ("LECTRA")
└── README.md
```

---

## 💻 Build & Run Instructions

### Prerequisites
- **JDK**: Version 17 (or compatible Java 11+)
- **Android SDK**: Compile SDK 36 (Android 15+), Min SDK 24 (Android 7.0+)
- **Gradle**: 8.x+ (managed via wrapper or system `gradle`)

### Local Command Line Build
```bash
# Verify compilation and build debug APK
gradle assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions CI
The included workflow `.github/workflows/build.yml` automatically triggers on `push` or `pull_request` to `main`:
1. Checks out the repository.
2. Configures Temurin JDK 17.
3. Sets up Gradle cache.
4. Executes `./gradlew assembleDebug`.
5. Uploads `app-debug.apk` as a downloadable artifact.

---

## 🔒 Privacy & AI Secrets
- All courses, notes, and study sessions are saved locally using **Android Jetpack Room**.
- To enable optional Gemini API features, configure `GEMINI_API_KEY` in the **AI Studio Secrets panel**. The app will safely fall back to local-only mode if no key is present.
