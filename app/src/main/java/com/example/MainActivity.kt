package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LectraUiState
import com.example.ui.LectraViewModel
import com.example.ui.components.AddCourseDialog
import com.example.ui.components.AddFlashcardDialog
import com.example.ui.components.AddQuizDialog
import com.example.ui.components.AddStudySessionDialog
import com.example.ui.components.AddStudyTaskDialog
import com.example.ui.components.LectraBottomBar
import com.example.ui.components.LectraTopBar
import com.example.ui.navigation.LectraDestination
import com.example.ui.navigation.SubScreen
import com.example.ui.screens.CourseDetailScreen
import com.example.ui.screens.CourseListScreen
import com.example.ui.screens.CoursesScreen
import com.example.ui.screens.FlashcardStudyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.QuizStudyScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.SettingsScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.ui.splash.SplashScreen
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddSessionDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
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
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.updateDeviceTime()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
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

    val isFullStudyScreen = uiState.currentSubScreen == SubScreen.FLASHCARD_STUDY || uiState.currentSubScreen == SubScreen.QUIZ_STUDY

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isFullStudyScreen && uiState.currentSubScreen != SubScreen.COURSE_DETAIL) {
                LectraTopBar(
                    title = "LECTRA",
                    subtitle = when (uiState.currentDestination) {
                        LectraDestination.HOME -> "Campus Study Companion"
                        LectraDestination.COURSES -> "My University Courses"
                        LectraDestination.PLANNER -> "Study Timetable & Deadlines"
                        LectraDestination.REVISION -> "Active Recall & Quizzes"
                        LectraDestination.SETTINGS -> "Profile & Preferences"
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullStudyScreen && uiState.currentSubScreen != SubScreen.COURSE_DETAIL) {
                LectraBottomBar(
                    currentDestination = uiState.currentDestination,
                    onNavigate = { viewModel.navigateTo(it) }
                )
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
                        CourseDetailScreen(
                            course = course,
                            units = uiState.courseUnits,
                            topics = uiState.courseTopics,
                            materials = courseMaterials,
                            onBackClick = { viewModel.closeSubScreen() },
                            onAddUnit = { num, title, desc -> viewModel.addUnit(course.id, num, title, desc) },
                            onAddTopic = { unitId, courseId, title, notes -> viewModel.addTopic(unitId, courseId, title, notes) },
                            onToggleTopicMastered = { viewModel.toggleTopicMastered(it) },
                            onPdfSelected = { uri, cId, uId, tId -> viewModel.handlePdfImport(uri, cId, uId, tId) },
                            onDeleteMaterial = { viewModel.deleteMaterial(it) },
                            onStartFlashcards = { viewModel.startFlashcardStudy(it) },
                            onStartQuiz = { viewModel.startQuizStudy(it) }
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
                                    generalPdfPickerLauncher.launch(arrayOf("application/pdf"))
                                }
                            )
                        }

                        LectraDestination.COURSES -> {
                            CourseListScreen(
                                courses = uiState.courses,
                                materials = uiState.materials,
                                onCourseClick = { viewModel.openCourseDetail(it) },
                                onAddCourseClick = { showAddCourseDialog = true },
                                onDeleteCourse = { viewModel.deleteCourse(it) }
                            )
                        }

                        LectraDestination.PLANNER -> {
                            PlannerScreen(
                                sessions = uiState.sessions,
                                tasks = uiState.tasks,
                                onToggleSession = { viewModel.toggleSessionCompletion(it) },
                                onDeleteSession = { viewModel.deleteSession(it) },
                                onToggleTask = { viewModel.toggleTaskCompletion(it) },
                                onDeleteTask = { viewModel.deleteTask(it) },
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

    if (showAddTaskDialog) {
        AddStudyTaskDialog(
            courses = uiState.courses,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { courseId, code, title, days, priority ->
                viewModel.addTask(
                    courseId = courseId,
                    courseCode = code,
                    title = title,
                    dueDate = System.currentTimeMillis() + days * 86400000L,
                    priority = priority
                )
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
}
