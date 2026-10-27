# CourseListScreen (Warm Amber & Deep Espresso)

A dedicated Material 3 Composable screen that observes courses directly from the local Room database, styled with an academic Warm Amber and Deep Espresso palette.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following choices were confirmed during Phase 1:
> - **Navigation Integration**: Replace the existing Courses screen destination with `CourseListScreen`.
> - **Card Elements**: Display course code badge, course title, lecturer name, weekly study hours target, and quick actions (delete with confirmation, tap to open details).
> - **Palette Scope**: Apply the Warm Amber (`#F59E0B` / `#D97706`) and Deep Espresso (`#1A120B` / `#281B12`) palette specifically to the `CourseListScreen` container, cards, chips, and Floating Action Button.

---

### 1. Overview & Core Concept
- **What It Does**: Renders a reactive, clean list of enrolled academic courses queried from Room (`CourseDao`). Provides a Floating Action Button to add a new course and an interactive card layout with delete confirmation dialogs and navigation to course details.
- **Target Audience**: University students managing course syllabi, lecture materials, and weekly credit hours.
- **Key Value**: Delivers a rich, focused course management interface with high contrast, tactile feedback, and accessibility-compliant touch targets (min 48dp).

---

### 2. User Experience & Visual Design

#### Color System: Warm Amber & Deep Espresso
- **Background / Container**: Deep Espresso (`#1A120B` to `#23170F`) with subtle vertical gradient.
- **Card Surface**: Rich Roasted Espresso (`#2C1E14`) with a warm border stroke (`#4A3525`).
- **Primary Accent**: Warm Amber (`#F59E0B` / `#D97706`) for badges, FAB, and progress highlights.
- **Secondary Accent**: Golden Honey (`#FBBF24`) for icons and stat chips.
- **Typography**:
  - Primary text: Cream / Warm White (`#FDFBF7`)
  - Secondary text: Latte / Muted Tan (`#C4B5A5`)
- **Action State**: Crimson Rust (`#EF4444`) for destructive delete confirmation.

#### Layout & Interaction Flow
1. **Empty State**: When no courses are enrolled, displays a cozy warm espresso card with a book icon, encouraging students to tap the Amber FAB.
2. **Populated List**: A staggered `LazyColumn` of `CourseCard` items featuring:
   - Top accent stripe colored by course's chosen color or warm amber.
   - Course code badge with rounded pill shape.
   - Course title in bold typography.
   - Lecturer row with person icon.
   - Study time target chip and PDF materials counter.
   - Direct tap navigates to `CourseDetailScreen`.
   - Delete icon triggers an accessible Material 3 `AlertDialog`.
3. **Floating Action Button**: Positioned at bottom-end with Warm Amber container, Deep Espresso icon, and ripple feedback, triggering the course creation dialog.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Integration into Existing ViewModel State vs Standalone Flow**
  - *Chosen Approach*: `CourseListScreen` accepts reactive `List<CourseEntity>` and `List<LectureMaterialEntity>` from `LectraViewModel.uiState` (which already observes Room DAO `Flow`).
  - *Why*: Eliminates redundant DAO query dispatchers and ensures instant UI synchronization when courses are added or removed across dashboard, planner, and course detail screens.
- **Decision 2: Localized Warm Amber Palette vs Global Theme Overwrite**
  - *Chosen Approach*: Scope the Warm Amber & Deep Espresso color tokens to `CourseListScreen` components and card containers, while keeping existing app navigation (`LectraTheme`) consistent.
  - *Why*: Prevents breaking visual consistency on Home, Planner, and Revision screens while fulfilling the exact design mandate for the course screen.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      Room Database                          │
│                   (CourseDao / Entity)                      │
└─────────────────────────────┬───────────────────────────────┘
                              │ Flow<List<CourseEntity>>
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      LectraViewModel                        │
│                 (MutableStateFlow<UiState>)                 │
└─────────────────────────────┬───────────────────────────────┘
                              │ collectAsStateWithLifecycle()
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     CourseListScreen                        │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ TopBar / Screen Header (Warm Amber & Espresso Title)    │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ LazyColumn: CourseCard items                            │ │
│ │  - Code pill & Title                                    │ │
│ │  - Lecturer & Target Hours                              │ │
│ │  - Delete & Open Actions                                │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │ FloatingActionButton: Add Course Dialog trigger         │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### Verification Plan
- Build execution using `compile_applet`.
- Unit tests via `gradle :app:testDebugUnitTest` verifying `CourseEntity` rendering, empty state handling, and course list state emission.
