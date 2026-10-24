# LECTRA — Smart University Study Companion

A native Android application built with Kotlin and Jetpack Compose to help university students organize lecture materials, track course units and topics, schedule study sessions, and master exam material with active-recall flashcards and practice quizzes.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and design choices have been confirmed and incorporated into the blueprint:

- **Confirmed Aesthetic**: **Warm Amber and Deep Espresso Campus Style** — rich warm leather/amber accents (`#D97706`, `#B45309`), deep espresso & cocoa slate surfaces (`#1C1410`, `#2B1D16` in dark mode; `#FFFDF9`, `#FAF5EE` in light mode), parchment card tones, and high-contrast editorial typography.
- **Confirmed Data Seeding**: First-launch auto-seeding with realistic university courses (e.g. *CS 301: Distributed Systems & Cloud Architecture*, *BIO 210: Molecular Genetics & Cellular Biology*), complete with units, topics, scheduled study sessions, and flashcard/quiz decks so the app feels immediately alive and actionable.
- **Confirmed Revision Focus**: Comprehensive Revision Hub supporting **both** spaced-repetition flashcard drills (with Again, Hard, Good, Easy recall scoring) and interactive multiple-choice practice quizzes with instant feedback, explanations, and score tracking.
- **Local Persistence Strategy**: Local-first Room database storing structured relational data (Courses, Units, Topics, Document References via Android Storage Access Framework URI permissions, Study Sessions, Flashcards, and Quiz Results). No large binary PDFs stored directly in SQLite.
- **AI Integration Architecture**: Clean abstraction layer (`SummarizationService`, `FlashcardGeneratorService`) with Gemini API ready interfaces that function gracefully in offline/unauthenticated mode without crashing or blocking core study capabilities.
- **CI/CD & GitHub Readiness**: Standard Android Gradle structure, complete wrapper files, `.gitignore`, and a verified GitHub Actions workflow (`.github/workflows/build.yml`) to compile and artifact the debug APK.

---

### 1. Overview & Core Concept

**LECTRA** addresses the chronic cognitive overload experienced by university students who juggle dense lecture slide decks, syllabus timelines, and fragmented study notes.

- **Primary Persona**: College and graduate students managing 4–6 rigorous academic courses per term who struggle with unstructured PDF slides, last-minute cramming, and missed assignment deadlines.
- **Core Value Proposition**:
  1. **Course & Lecture Matrix**: Turn chaotic PDF slide drops into structured Units and Topics with permanent document SAF bookmarks and reading statuses.
  2. **Smart Timetable & Planner**: Prevent overlapping study sessions, track upcoming assignment deadlines, and log completed revision time.
  3. **Active Recall Hub**: Transition passive slide reading into active recall through self-scored flashcards and topical practice quizzes.
  4. **AI-Ready Foundation**: Modular service contract to plug in Gemini-powered slide summarization and auto-flashcard generation upon user secret activation.

---

### 2. User Experience & Visual Design

#### Aesthetic Direction: Warm Amber & Deep Espresso Campus Style
The visual identity evokes a modern university library study lounge—warm parchment, brass and amber accents, espresso card borders, and crisp, focused typography.

- **Primary Accent**: Warm Amber (`#D97706`), Warm Honey (`#F59E0B`), Terracotta Gold (`#C2410C`)
- **Background & Canvas**:
  - Dark Mode: Deep Espresso Slate (`#18120F`), Roasted Cocoa Surface (`#241A14`), Warm Muted Charcoal (`#2F241E`)
  - Light Mode: Warm Ivory/Parchment (`#FDFBF7`), Pale Oatmeal (`#F5EFE6`), Warm Timber Border (`#E6DACB`)
- **Text & Hierarchy**:
  - Display & Headlines: Bold, collegiate serif-weighted feeling with crisp tracking.
  - Body & Metadata: Highly legible geometric sans with generous line-height for dense academic reading.

#### Key Navigation Destinations (M3 Bottom Navigation Bar with Safe Insets)
1. **Home (Dashboard)**:
   - Quick greeting with campus term progress & weekly study streak badge.
   - "Today's Agenda" study session cards with one-tap "Mark Complete" / "Start Session".
   - "Recent Lecture Materials" carousel with document tags and topic links.
   - Quick action bar: "Import PDF", "Add Course", "New Study Session", "Quick Quiz".
   - Study time analytics (Hours studied this week, flashcards reviewed).
2. **My Courses**:
   - Course cards showing course code, title, color tag, lecturer, and total units/materials.
   - Hierarchical Course Detail: Units -> Topics -> Lecture PDFs & Notes.
   - Document SAF import: Pick PDF via system file picker, persist URI permissions, track import date, file size, and reading progress.
   - Add/Edit/Delete Course and Topic dialogs.
3. **Study Planner**:
   - Day & week timeline view with non-overlapping visual session blocks.
   - Upcoming assignment/exam deadline countdown tags.
   - Floating Action Button to schedule sessions with course picker, topic selection, start/end time validation, and reminder settings.
4. **Revision Hub**:
   - Dual-tab interface: **Flashcards** & **Practice Quizzes**.
   - Interactive Flashcard Study Deck: Smooth flip animation, 4-tier recall grading (*Again*, *Hard*, *Good*, *Easy*), session completion summary.
   - Interactive Practice Quiz: Multiple choice questions with instant answer reveal, explanation cards, and final score summary.
   - Manual deck/card creation and AI-Ready "Generate from Lecture" preview card.
5. **Profile & Settings**:
   - Academic semester configuration (Term dates, target study hours).
   - Storage stats (Total courses, imported PDFs, total flashcards).
   - AI Engine Status indicator (Gemini configuration status, privacy explanation disclosure).
   - Sample data reset & export options.

---

### 3. Key Product Decisions & Trade-Offs

- **Room Relational Database with Foreign Keys**:
  - *Chosen Approach*: Clean Room database (`LectraDatabase`) containing entities for `CourseEntity`, `UnitEntity`, `TopicEntity`, `LectureMaterialEntity`, `StudySessionEntity`, `FlashcardEntity`, and `QuizQuestionEntity`.
  - *Why*: Instant offline access, zero network delay, strong relational integrity (cascading deletes when a course or unit is removed), and fully compliant with Android best practices.
- **Android Storage Access Framework (SAF) URI Persistence**:
  - *Chosen Approach*: Store Android content URIs (`takePersistableUriPermission`) and file metadata in SQLite rather than copying gigabytes of raw PDF byte arrays into the database.
  - *Why*: Avoids database size bloat, prevents out-of-memory errors on large slide decks, and respects user device storage.
- **Offline-First AI Bridge**:
  - *Chosen Approach*: `AiService` interface with a `LocalFallbackAiService` and `GeminiAiService` implementation. If no API key is configured in AI Studio secrets, the app gracefully highlights manual creation while showing transparent AI preparation cards without crashes.
- **Pre-loaded Academic Demo Dataset**:
  - *Chosen Approach*: Seamless database pre-population during first launch so the user explores a fully populated study ecosystem immediately.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│                      LECTRA PRESENTATION LAYER                  │
│                                                                 │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────┐  │
│  │  HomeScreen  │ │CourseScreens │ │PlannerScreen │ │Revision│  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └───┬────┘  │
│         │                │                │             │       │
│  ┌──────▼────────────────▼────────────────▼─────────────▼────┐  │
│  │                    LectraViewModel                        │  │
│  │   (StateFlow<LectraUiState>, CoroutineScope, SAF Handler) │  │
│  └──────────────────────────────┬────────────────────────────┘  │
└─────────────────────────────────┼───────────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────────┐
│                     LECTRA REPOSITORY LAYER                     │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    LectraRepository                        │ │
│  │   (Flow-based queries, CRUD operations, SAF resolver)      │ │
│  └──────────────┬───────────────────────────────┬─────────────┘ │
└─────────────────┼───────────────────────────────┼───────────────┘
                  │                               │
┌─────────────────▼──────────────┐ ┌──────────────▼───────────────┐
│         LOCAL PERSISTENCE      │ │          AI SERVICES         │
│  ┌───────────────────────────┐ │ │  ┌─────────────────────────┐ │
│  │      LectraDatabase       │ │ │  │  AiSummarizerService    │ │
│  │ (Room 2.6.1 + KSP Engine) │ │ │  │  - Gemini REST / Logic   │ │
│  │  - CourseDao              │ │ │  │  - Fallback / Mock-free │ │
│  │  - LectureMaterialDao     │ │ │  │  - Explicit consent     │ │
│  │  - StudySessionDao        │ │ │  └─────────────────────────┘ │
│  │  - RevisionDao            │ │                                │
│  └───────────────────────────┘ │                                │
└────────────────────────────────┘ └──────────────────────────────┘
```

#### GitHub Actions Workflow (`.github/workflows/build.yml`)
- Automates checkout, JDK 17 setup, Android SDK caching, and executing `gradle assembleDebug`.
- Uploads `app/build/outputs/apk/debug/app-debug.apk` as a downloadable artifact.
- Includes step-by-step instructions in the repository `README.md`.
