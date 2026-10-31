package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LectraUiState
import com.example.ui.LectraViewModel
import androidx.compose.foundation.layout.Column
import com.example.data.model.LectureClassEntity
import com.example.data.model.StudyTaskEntity
import com.example.ui.components.AddCourseDialog
import com.example.ui.components.AddEditLectureClassDialog
import com.example.ui.components.AddFlashcardDialog
import com.example.ui.components.AddQuizDialog
import com.example.ui.components.AddStudySessionDialog
import com.example.ui.components.AddStudyTaskDialog
import com.example.ui.components.LectraBottomBar
import com.example.ui.components.LectraTopBar
import com.example.ui.components.MiniTimerPill
import com.example.ui.navigation.LectraDestination
import com.example.ui.navigation.SubScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.CourseListScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.FlashcardStudyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.PomodoroTimerScreen
import com.example.ui.screens.QuizStudyScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.SettingsScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.splash.SplashScreen
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.ui.screens.ResearchScreen
import com.example.ui.theme.LectraTheme
import com.example.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("LECTRA_CRASH", "Fatal unhandled exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            android.util.Log.w("LECTRA_AUTH", "FirebaseApp auto-init skipped: ${e.message}")
        }

        enableEdgeToEdge()
        setContent {
            val mainViewModel: LectraViewModel = viewModel()
            val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()

            LectraTheme(themeMode = mainUiState.themeMode) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("lectra_prefs", android.content.Context.MODE_PRIVATE) }
                var isOnboardingCompleted by remember {
                    mutableStateOf(prefs.getBoolean("onboarding_completed", false))
                }
                var isSemesterReconfiguring by remember { mutableStateOf(false) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else if (!isOnboardingCompleted) {
                    WelcomeScreen(
                        onGetStarted = {
                            prefs.edit()
                                .putBoolean("onboarding_completed", true)
                                .putBoolean("semester_setup_complete", true)
                                .apply()
                            isOnboardingCompleted = true
                        }
                    )
                } else if (isSemesterReconfiguring) {
                    com.example.ui.screens.SemesterSetupScreen(
                        viewModel = mainViewModel,
                        onSetupComplete = {
                            isSemesterReconfiguring = false
                        }
                    )
                } else {
                    LectraApp(
                        viewModel = mainViewModel,
                        onReconfigureSemester = {
                            isSemesterReconfiguring = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LectraApp(
    viewModel: LectraViewModel = viewModel(),
    onReconfigureSemester: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showAddClassDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<LectureClassEntity?>(null) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var showAddFlashcardDialog by remember { mutableStateOf(false) }
    var showAddQuizDialog by remember { mutableStateOf(false) }

    // Quick PDF Picker
    val generalPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val targetCourseId = uiState.selectedCourseId
                ?: uiState.courses.firstOrNull()?.id
            if (targetCourseId != null) {
                viewModel.handlePdfImport(it, targetCourseId)
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    // Time update receiver for system changes
    val timeUpdateReceiver = remember {
        object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                viewModel.updateDeviceTime()
            }
        }
    }
    
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val intentFilter = android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_TIME_CHANGED)
            addAction(android.content.Intent.ACTION_TIMEZONE_CHANGED)
            addAction(android.content.Intent.ACTION_DATE_CHANGED)
        }
        context.registerReceiver(timeUpdateReceiver, intentFilter)
        
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.updateDeviceTime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            context.unregisterReceiver(timeUpdateReceiver)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle user notices / Snackbars
    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    // Global Back Handler
    BackHandler(enabled = uiState.currentSubScreen != SubScreen.NONE || uiState.currentDestination != LectraDestination.HOME) {
        viewModel.closeSubScreen()
    }

    val isFullStudyScreen = uiState.currentSubScreen == SubScreen.FLASHCARD_STUDY || 
            uiState.currentSubScreen == SubScreen.QUIZ_STUDY ||
            uiState.currentSubScreen == SubScreen.POMODORO_TIMER ||
            uiState.currentSubScreen == SubScreen.GLOBAL_SEARCH ||
            uiState.currentSubScreen == SubScreen.RESEARCH ||
            uiState.currentSubScreen == SubScreen.PDF_VIEWER ||
            uiState.currentSubScreen == SubScreen.DOCUMENT_VIEWER

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isFullStudyScreen && uiState.currentSubScreen != SubScreen.COURSE_DETAIL && uiState.currentDestination != LectraDestination.HOME && uiState.currentDestination != LectraDestination.SEARCH) {
                LectraTopBar(
                    title = "LECTRA",
                    subtitle = when (uiState.currentDestination) {
                        LectraDestination.HOME -> "Campus Study Companion"
                        LectraDestination.COURSES -> "My University Courses"
                        LectraDestination.PLANNER -> "Study Timetable & Deadlines"
                        LectraDestination.REVISION -> "Active Recall & Quizzes"
                        LectraDestination.SEARCH -> "Google Search"
                        LectraDestination.SETTINGS -> "Profile & Preferences"
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openResearch() },
                            modifier = Modifier.testTag("top_bar_research_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = "Research",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { viewModel.openGlobalSearch() },
                            modifier = Modifier.testTag("top_bar_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullStudyScreen && uiState.currentSubScreen != SubScreen.COURSE_DETAIL) {
                Column {
                    MiniTimerPill(
                        visible = uiState.isTimerRunning || (uiState.timerRemainingSeconds < uiState.timerTotalSeconds),
                        courses = uiState.courses,
                        selectedCourseId = uiState.selectedTimerCourseId,
                        timerMode = uiState.timerMode,
                        remainingSeconds = uiState.timerRemainingSeconds,
                        isRunning = uiState.isTimerRunning,
                        onExpand = { viewModel.openPomodoroTimer() },
                        onTogglePlayPause = {
                            if (uiState.isTimerRunning) viewModel.pausePomodoroTimer() else viewModel.startPomodoroTimer()
                        }
                    )
                    LectraBottomBar(
                        currentDestination = uiState.currentDestination,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentSubScreen) {
                SubScreen.COURSE_DETAIL -> {
                    val course = uiState.courses.find { it.id == uiState.selectedCourseId }
                    if (course != null) {
                        val courseMaterials = uiState.materials.filter { it.courseId == course.id }
                        val courseTasks = uiState.tasks.filter { it.courseId == course.id }
                        CourseDetailScreen(
                            course = course,
                            units = uiState.courseUnits,
                            topics = uiState.courseTopics,
                            materials = courseMaterials,
                            tasks = courseTasks,
                            themeMode = uiState.themeMode,
                            onBackClick = { viewModel.closeSubScreen() },
                            onAddUnit = { num, title, desc -> viewModel.addUnit(course.id, num, title, desc) },
                            onAddTopic = { unitId, courseId, title, notes -> viewModel.addTopic(unitId, courseId, title, notes) },
                            onToggleTopicMastered = { viewModel.toggleTopicMastered(it) },
                            onPdfSelected = { uri, cId, uId, tId -> viewModel.handlePdfImport(uri, cId, uId, tId) },
                            onDeleteMaterial = { viewModel.deleteMaterial(it) },
                            onStartFlashcards = { viewModel.startFlashcardStudy(it) },
                            onStartQuiz = { viewModel.startQuizStudy(it) },
                            onStartPomodoro = { viewModel.openPomodoroTimer(it) },
                            onOpenMaterial = { viewModel.openPdfMaterial(it, context) },
                            onDeleteCourse = { viewModel.deleteCourse(it) },
                            onToggleTask = { viewModel.toggleTaskCompletion(it) }
                        )
                    } else {
                        viewModel.closeSubScreen()
                    }
                }

                SubScreen.FLASHCARD_STUDY -> {
                    FlashcardStudyScreen(
                        flashcards = viewModel.currentActiveFlashcards(),
                        currentIndex = uiState.flashcardCurrentIndex,
                        isFlipped = uiState.isFlashcardFlipped,
                        onFlip = { viewModel.flipFlashcard() },
                        onRate = { viewModel.rateFlashcard(it) },
                        onBackClick = { viewModel.closeSubScreen() }
                    )
                }

                SubScreen.QUIZ_STUDY -> {
                    QuizStudyScreen(
                        quizzes = viewModel.currentActiveQuizzes(),
                        currentIndex = uiState.quizCurrentIndex,
                        selectedOption = uiState.quizSelectedOption,
                        isAnswerRevealed = uiState.isQuizAnswerRevealed,
                        score = uiState.quizScore,
                        isFinished = uiState.isQuizFinished,
                        onSelectOption = { viewModel.selectQuizOption(it) },
                        onConfirmAnswer = { viewModel.confirmQuizAnswer() },
                        onNextQuestion = { viewModel.nextQuizQuestion() },
                        onResetQuiz = { viewModel.resetQuizSession() },
                        onBackClick = { viewModel.closeSubScreen() }
                    )
                }

                SubScreen.POMODORO_TIMER -> {
                    PomodoroTimerScreen(
                        uiState = uiState,
                        onBackClick = { viewModel.closeSubScreen() },
                        onSelectCourse = { viewModel.selectTimerCourse(it) },
                        onSetGoal = { viewModel.setTimerSessionGoal(it) },
                        onSetMode = { mode, duration -> viewModel.setTimerMode(mode, duration) },
                        onStartTimer = { viewModel.startPomodoroTimer() },
                        onPauseTimer = { viewModel.pausePomodoroTimer() },
                        onStopAndLog = { viewModel.stopAndLogPomodoroSession() },
                        onResetTimer = { viewModel.resetPomodoroTimer() },
                        onSkipMode = { viewModel.skipToNextPomodoroMode() }
                    )
                }

                SubScreen.GLOBAL_SEARCH -> {
                    com.example.ui.screens.GlobalSearchScreen(
                        uiState = uiState,
                        onBackClick = { viewModel.closeSubScreen() },
                        onQueryChanged = { viewModel.setSearchQuery(it) },
                        onOpenCourse = { courseId ->
                            viewModel.closeSubScreen()
                            viewModel.openCourseDetail(courseId)
                        },
                        onOpenResearch = { query ->
                            viewModel.openResearch(query)
                        }
                    )
                }

                SubScreen.RESEARCH -> {
                    ResearchScreen(
                        uiState = uiState,
                        onBackClick = { viewModel.closeSubScreen() },
                        onQueryChanged = { viewModel.setResearchQuery(it) },
                        onExecuteSearch = { query ->
                            viewModel.executeGoogleSearch(context, query)
                        }
                    )
                }

                SubScreen.PDF_VIEWER, SubScreen.DOCUMENT_VIEWER -> {
                    val material = uiState.activeReadingMaterial
                    if (material != null) {
                        com.example.ui.screens.document.DocumentViewerDispatcher(
                            material = material,
                            annotations = uiState.activePdfAnnotations,
                            isDarkTheme = uiState.themeMode == com.example.ui.theme.ThemeMode.DARK,
                            onBackClick = { viewModel.closeSubScreen() },
                            onPageChanged = { pageNum, totalPages ->
                                viewModel.updatePdfReadingProgress(material.id, pageNum, totalPages)
                            },
                            onAddAnnotation = { pageIndex, type, colorHex, content, dataJson, relX, relY ->
                                viewModel.addPdfAnnotation(material.id, pageIndex, type, colorHex, content, dataJson, relX, relY)
                            },
                            onDeleteAnnotation = { annotationId ->
                                viewModel.deletePdfAnnotation(annotationId)
                            },
                            onToggleBookmark = { matId, pageIndex ->
                                viewModel.togglePdfBookmark(matId, pageIndex)
                            }
                        )
                    } else {
                        viewModel.closeSubScreen()
                    }
                }

                SubScreen.NONE -> {
                    when (uiState.currentDestination) {
                        LectraDestination.HOME -> {
                            HomeScreen(
                                uiState = uiState,
                                onNavigate = { viewModel.navigateTo(it) },
                                onOpenCourse = { viewModel.openCourseDetail(it) },
                                onToggleSession = { viewModel.toggleSessionCompletion(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                onStartFlashcardStudy = { viewModel.startFlashcardStudy(it) },
                                onStartQuiz = { viewModel.startQuizStudy(it) },
                                onQuickAddCourse = { showAddCourseDialog = true },
                                onQuickAddSession = { showAddSessionDialog = true },
                                onQuickAddTask = { showAddTaskDialog = true },
                                onRequestPdfPicker = {
                                    generalPdfPickerLauncher.launch(com.example.data.document.DocumentType.SUPPORTED_MIME_TYPES)
                                },
                                onOpenPomodoro = { viewModel.openPomodoroTimer(it) },
                                onOpenResearch = { viewModel.openResearch(it) },
                                onToggleTheme = { viewModel.toggleTheme() },
                                onOpenMaterial = { viewModel.openPdfMaterial(it, context) }
                            )
                        }

                        LectraDestination.COURSES -> {
                            CourseListScreen(
                                courses = uiState.courses,
                                materials = uiState.materials,
                                units = uiState.units,
                                tasks = uiState.tasks,
                                onCourseClick = { viewModel.openCourseDetail(it) },
                                onAddCourseClick = { showAddCourseDialog = true },
                                onDeleteCourse = { viewModel.deleteCourse(it) },
                                themeMode = uiState.themeMode
                            )
                        }

                        LectraDestination.PLANNER -> {
                            PlannerScreen(
                                classes = uiState.classes,
                                courses = uiState.courses,
                                sessions = uiState.sessions,
                                tasks = uiState.tasks,
                                onAddClassClick = { showAddClassDialog = true },
                                onEditClass = { classToEdit = it },
                                onDeleteClass = { viewModel.deleteLectureClass(it) },
                                onToggleSession = { viewModel.toggleSessionCompletion(it) },
                                onDeleteSession = { viewModel.deleteSession(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
                                onEditTask = { taskToEdit = it },
                                onAddSessionClick = { showAddSessionDialog = true },
                                onAddTaskClick = { showAddTaskDialog = true }
                            )
                        }

                        LectraDestination.REVISION -> {
                            RevisionScreen(
                                courses = uiState.courses,
                                flashcards = uiState.flashcards,
                                quizzes = uiState.quizzes,
                                aiStatus = uiState.aiStatus,
                                onStartFlashcards = { viewModel.startFlashcardStudy(it) },
                                onStartQuiz = { viewModel.startQuizStudy(it) },
                                onAddFlashcardClick = { showAddFlashcardDialog = true },
                                onAddQuizClick = { showAddQuizDialog = true },
                                onResetQuizProgress = { viewModel.resetQuizSession() }
                            )
                        }

                        LectraDestination.SEARCH -> {
                            ResearchScreen(
                                uiState = uiState,
                                onBackClick = { viewModel.navigateTo(LectraDestination.HOME) },
                                onQueryChanged = { viewModel.setResearchQuery(it) },
                                onExecuteSearch = { query ->
                                    viewModel.executeGoogleSearch(context, query)
                                }
                            )
                        }

                        LectraDestination.SETTINGS -> {
                            SettingsScreen(
                                uiState = uiState,
                                backupManager = viewModel.backupManager,
                                onThemeModeChanged = { viewModel.setThemeMode(it) },
                                onResetDemoData = { viewModel.resetToDemoData() },
                                onReconfigureSemester = onReconfigureSemester
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddCourseDialog) {
        AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { code, title, lecturer, color, hours ->
                viewModel.addCourse(code, title, lecturer, color, hours)
            },
            onConfirmWithUnits = { code, title, lecturer, color, hours, units ->
                viewModel.addCourseWithUnits(code, title, lecturer, color, hours, units)
            }
        )
    }

    if (showAddClassDialog || classToEdit != null) {
        AddEditLectureClassDialog(
            courses = uiState.courses,
            existingClass = classToEdit,
            defaultDayOfWeek = uiState.currentDayOfWeek,
            onDismiss = {
                showAddClassDialog = false
                classToEdit = null
            },
            onConfirm = { courseId, code, name, topic, day, startH, startM, endH, endM, venue, colorHex ->
                if (classToEdit == null) {
                    viewModel.addLectureClass(
                        courseId = courseId,
                        courseCode = code,
                        courseName = name,
                        topicName = topic,
                        dayOfWeek = day,
                        startHour = startH,
                        startMinute = startM,
                        endHour = endH,
                        endMinute = endM,
                        venue = venue,
                        colorHex = colorHex
                    )
                } else {
                    viewModel.updateLectureClass(
                        classToEdit!!.copy(
                            courseId = courseId,
                            courseCode = code,
                            courseName = name,
                            topicOrLectureName = topic,
                            dayOfWeek = day,
                            startHour = startH,
                            startMinute = startM,
                            endHour = endH,
                            endMinute = endM,
                            venue = venue,
                            colorHex = colorHex
                        )
                    )
                }
                showAddClassDialog = false
                classToEdit = null
            },
            onDelete = {
                classToEdit?.let { viewModel.deleteLectureClass(it) }
                classToEdit = null
            }
        )
    }

    if (showAddSessionDialog) {
        AddStudySessionDialog(
            courses = uiState.courses,
            onDismiss = { showAddSessionDialog = false },
            onConfirm = { course, topic, day, hour, min, dur, goal ->
                viewModel.addStudySession(course.id, course.code, course.title, topic, day, hour, min, dur, goal)
            }
        )
    }

    if (showAddTaskDialog || taskToEdit != null) {
        AddStudyTaskDialog(
            courses = uiState.courses,
            existingTask = taskToEdit,
            onDismiss = {
                showAddTaskDialog = false
                taskToEdit = null
            },
            onConfirm = { courseId, code, title, days, priority ->
                viewModel.addTask(
                    courseId = courseId,
                    courseCode = code,
                    title = title,
                    dueDate = System.currentTimeMillis() + days * 86400000L,
                    priority = priority
                )
                showAddTaskDialog = false
            },
            onUpdate = { updatedTask ->
                viewModel.updateTask(updatedTask)
                taskToEdit = null
            }
        )
    }

    if (showAddFlashcardDialog) {
        AddFlashcardDialog(
            courses = uiState.courses,
            defaultCourseId = uiState.selectedCourseId,
            onDismiss = { showAddFlashcardDialog = false },
            onConfirm = { courseId, prompt, answer, hint ->
                viewModel.addFlashcard(courseId, prompt, answer, hint)
            }
        )
    }

    if (showAddQuizDialog) {
        AddQuizDialog(
            courses = uiState.courses,
            onDismiss = { showAddQuizDialog = false },
            onConfirm = { courseId, q, optA, optB, optC, optD, correctIdx, exp ->
                viewModel.addQuizQuestion(courseId, q, optA, optB, optC, optD, correctIdx, exp)
            }
        )
    }

    if (uiState.isImportingPdf) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            androidx.compose.material3.Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color(0xFF7565F6),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.pdfImportProgress ?: "Importing PDF Document...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
