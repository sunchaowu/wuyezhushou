package com.guanjiazhushou.assistant.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.guanjiazhushou.assistant.data.reminder.ReminderDeepLink
import com.guanjiazhushou.assistant.domain.model.LegalDocumentType
import com.guanjiazhushou.assistant.domain.model.OnboardingStep
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.domain.model.RoomListFilter
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.presentation.building.BuildingFormScreen
import com.guanjiazhushou.assistant.presentation.building.BuildingGridScreen
import com.guanjiazhushou.assistant.presentation.building.BuildingListScreen
import com.guanjiazhushou.assistant.presentation.common.RoomListScreen
import com.guanjiazhushou.assistant.presentation.communication.RoomCommunicationScreen
import com.guanjiazhushou.assistant.presentation.event.EventListScreen
import com.guanjiazhushou.assistant.presentation.legal.LegalDocumentScreen
import com.guanjiazhushou.assistant.presentation.legal.PrivacyConsentLoading
import com.guanjiazhushou.assistant.presentation.legal.PrivacyConsentScreen
import com.guanjiazhushou.assistant.presentation.legal.PrivacyConsentStatus
import com.guanjiazhushou.assistant.presentation.legal.PrivacyConsentViewModel
import com.guanjiazhushou.assistant.presentation.onboarding.OnboardingSpotlightOverlay
import com.guanjiazhushou.assistant.presentation.onboarding.OnboardingViewModel
import com.guanjiazhushou.assistant.presentation.profile.AboutScreen
import com.guanjiazhushou.assistant.presentation.profile.BackupScreen
import com.guanjiazhushou.assistant.presentation.profile.GroupScreen
import com.guanjiazhushou.assistant.presentation.profile.ProfileScreen
import com.guanjiazhushou.assistant.presentation.profile.SettingsScreen
import com.guanjiazhushou.assistant.presentation.profile.ThemeScreen
import com.guanjiazhushou.assistant.presentation.profile.ThemeViewModel
import com.guanjiazhushou.assistant.presentation.profile.VersionDetailsScreen
import com.guanjiazhushou.assistant.presentation.search.SearchScreen
import com.guanjiazhushou.assistant.presentation.statistics.RoomListViewModel
import com.guanjiazhushou.assistant.presentation.statistics.StatisticsScreen
import com.guanjiazhushou.assistant.ui.theme.PropertyManagerTheme
import com.guanjiazhushou.assistant.ui.theme.paletteFor
import com.guanjiazhushou.assistant.ui.theme.rememberThemedNavigationBarStyle

@Composable
fun AppNavigation(
    reminderDeepLink: ReminderDeepLink? = null,
    onReminderDeepLinkConsumed: () -> Unit = {},
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeId by themeViewModel.themeId.collectAsStateWithLifecycle()

    PropertyManagerTheme(themeId = themeId, palette = paletteFor(themeId)) {
        val privacyConsentViewModel: PrivacyConsentViewModel = hiltViewModel()
        val privacyConsentState by privacyConsentViewModel.uiState.collectAsStateWithLifecycle()
        var consentLegalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }

        LaunchedEffect(Unit) {
            privacyConsentViewModel.ensureInitialized()
        }

        when (privacyConsentState.status) {
            PrivacyConsentStatus.LOADING -> {
                PrivacyConsentLoading()
                return@PropertyManagerTheme
            }
            PrivacyConsentStatus.REQUIRED -> {
                if (consentLegalDocument != null) {
                    LegalDocumentScreen(
                        documentType = consentLegalDocument!!,
                        onBack = { consentLegalDocument = null },
                    )
                } else {
                    PrivacyConsentScreen(
                        onOpenDocument = { consentLegalDocument = it },
                        onAccept = privacyConsentViewModel::acceptPolicy,
                    )
                }
                return@PropertyManagerTheme
            }
            PrivacyConsentStatus.ACCEPTED -> Unit
        }

        val navController = rememberNavController()
        val onboardingViewModel: OnboardingViewModel = hiltViewModel()
        val onboardingStep by onboardingViewModel.currentStep.collectAsStateWithLifecycle()
        val onboardingAnchors by onboardingViewModel.anchors.collectAsStateWithLifecycle()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = BottomTab.entries.any { tab ->
            currentRoute?.startsWith(tab.route) == true ||
                currentRoute in bottomTabLeafRoutes(tab)
        }

        LaunchedEffect(Unit) {
            onboardingViewModel.ensureInitialized()
        }

        LaunchedEffect(reminderDeepLink) {
            val link = reminderDeepLink ?: return@LaunchedEffect
            navController.navigate(BottomTab.BUILDING.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            when (link.targetType) {
                ReminderTargetType.EVENT -> {
                    navController.navigate(BottomTab.PROFILE.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    navController.navigate(Routes.EVENT_LIST) {
                        launchSingleTop = true
                    }
                }
                ReminderTargetType.PAYMENT_FOLLOWUP -> {
                    if (link.buildingId > 0) {
                        navController.navigate(Routes.buildingGrid(link.buildingId, link.roomId)) {
                            launchSingleTop = true
                        }
                    }
                    navController.navigate(Routes.roomCommunication(link.roomId)) {
                        launchSingleTop = true
                    }
                }
            }
            onReminderDeepLinkConsumed()
        }

        val activeOnboardingStep = onboardingStep?.takeIf { step ->
            when (step) {
                OnboardingStep.CREATE_BUILDING,
                OnboardingStep.MODIFY_DELETE,
                -> currentRoute == Routes.BUILDING_LIST
                OnboardingStep.FILL_FORM -> currentRoute == Routes.BUILDING_FORM
                OnboardingStep.STATISTICS -> showBottomBar
            }
        }

        Box {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        AppBottomBar(
                            navController = navController,
                            currentRoute = currentRoute,
                            onboardingViewModel = onboardingViewModel,
                        )
                    }
                },
                // Avoid double top inset: child screens own TopAppBar + status bar handling.
                contentWindowInsets = if (showBottomBar) {
                    ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Bottom)
                } else {
                    WindowInsets(0, 0, 0, 0)
                },
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = Routes.BUILDING_GRAPH,
                    modifier = if (showBottomBar) {
                        Modifier.padding(padding)
                    } else {
                        Modifier
                    },
                ) {
                    buildingGraph(navController, onboardingViewModel)
                    statisticsGraph(navController)
                    profileGraph(navController)
                }
            }

            activeOnboardingStep?.let { step ->
                OnboardingSpotlightOverlay(
                    step = step,
                    targetBounds = onboardingAnchors[step],
                    onNext = {
                        when (step) {
                            OnboardingStep.CREATE_BUILDING -> {
                                onboardingViewModel.onCreateBuildingClicked()
                                navController.navigate(Routes.BUILDING_FORM)
                            }
                            else -> onboardingViewModel.advanceFromCurrentStep()
                        }
                    },
                    onSkip = onboardingViewModel::skipOnboarding,
                )
            }
        }
    }
}

private fun bottomTabLeafRoutes(tab: BottomTab): Set<String> = when (tab) {
    BottomTab.BUILDING -> setOf(Routes.BUILDING_LIST)
    BottomTab.STATISTICS -> setOf(Routes.STATISTICS)
    BottomTab.PROFILE -> setOf(Routes.PROFILE)
}

@Composable
private fun AppBottomBar(
    navController: NavHostController,
    currentRoute: String?,
    onboardingViewModel: OnboardingViewModel,
) {
    val navStyle = rememberThemedNavigationBarStyle()
    NavigationBar(containerColor = navStyle.containerColor) {
        BottomTab.entries.forEach { tab ->
            val selected = currentRoute?.let { route ->
                route.startsWith(tab.route) || route in bottomTabLeafRoutes(tab)
            } == true
            NavigationBarItem(
                selected = selected,
                colors = navStyle.itemColors,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                modifier = if (tab == BottomTab.STATISTICS) {
                    Modifier.onGloballyPositioned { coordinates ->
                        val position = coordinates.positionInRoot()
                        val size = coordinates.size
                        onboardingViewModel.registerAnchor(
                            step = OnboardingStep.STATISTICS,
                            bounds = Rect(
                                left = position.x,
                                top = position.y,
                                right = position.x + size.width,
                                bottom = position.y + size.height,
                            ),
                        )
                    }
                } else {
                    Modifier
                },
            )
        }
    }
}

private val BottomTab.icon: ImageVector
    get() = when (this) {
        BottomTab.BUILDING -> Icons.Default.Apartment
        BottomTab.STATISTICS -> Icons.Default.BarChart
        BottomTab.PROFILE -> Icons.Default.Person
    }

private fun androidx.navigation.NavGraphBuilder.buildingGraph(
    navController: NavHostController,
    onboardingViewModel: OnboardingViewModel,
) {
    navigation(startDestination = Routes.BUILDING_LIST, route = Routes.BUILDING_GRAPH) {
        composable(Routes.BUILDING_LIST) {
            BuildingListScreen(
                onCreateBuilding = {
                    onboardingViewModel.onCreateBuildingClicked()
                    navController.navigate(Routes.BUILDING_FORM)
                },
                onEditBuilding = { id -> navController.navigate(Routes.buildingFormEdit(id)) },
                onOpenGrid = { id -> navController.navigate(Routes.buildingGrid(id)) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onboardingViewModel = onboardingViewModel,
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenBuilding = { buildingId ->
                    navController.navigate(Routes.buildingGrid(buildingId)) {
                        popUpTo(Routes.BUILDING_LIST)
                        launchSingleTop = true
                    }
                },
                onOpenRoom = { buildingId, roomId ->
                    navController.navigate(Routes.buildingGrid(buildingId, roomId)) {
                        popUpTo(Routes.BUILDING_LIST)
                        launchSingleTop = true
                    }
                },
                onOpenCommunication = { roomId ->
                    navController.navigate(Routes.roomCommunication(roomId)) {
                        popUpTo(Routes.BUILDING_LIST)
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Routes.BUILDING_FORM) {
            BuildingFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    val stayOnList = onboardingViewModel.onBuildingSaved()
                    navController.popBackStack()
                    if (!stayOnList) {
                        navController.navigate(Routes.buildingGrid(id))
                    }
                },
                onboardingViewModel = onboardingViewModel,
            )
        }
        composable(
            route = Routes.BUILDING_FORM_EDIT,
            arguments = listOf(navArgument("buildingId") { type = NavType.LongType }),
        ) {
            BuildingFormScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    val stayOnList = onboardingViewModel.onBuildingSaved()
                    navController.popBackStack()
                    if (!stayOnList) {
                        navController.navigate(Routes.buildingGrid(id))
                    }
                },
                onboardingViewModel = onboardingViewModel,
            )
        }
        composable(
            route = Routes.BUILDING_GRID,
            arguments = listOf(
                navArgument("buildingId") { type = NavType.LongType },
                navArgument("roomId") {
                    type = NavType.LongType
                    defaultValue = Routes.NO_ROOM_HIGHLIGHT
                },
            ),
        ) {
            BuildingGridScreen(
                onBack = { navController.popBackStack() },
                onOpenCommunication = { roomId ->
                    navController.navigate(Routes.roomCommunication(roomId))
                },
            )
        }
        composable(
            route = Routes.ROOM_COMMUNICATION,
            arguments = listOf(navArgument("roomId") { type = NavType.LongType }),
        ) {
            RoomCommunicationScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.statisticsGraph(navController: NavHostController) {
    navigation(startDestination = Routes.STATISTICS, route = Routes.STATISTICS_GRAPH) {
        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onDrillDown = { filter, scope ->
                    navController.navigate(Routes.roomList(filter, scope))
                },
            )
        }
        composable(
            route = Routes.ROOM_LIST,
            arguments = listOf(
                navArgument("filterType") { type = NavType.StringType },
                navArgument("scopeType") { type = NavType.StringType },
            ),
        ) {
            val viewModel: RoomListViewModel = hiltViewModel()
            val rooms by viewModel.rooms.collectAsStateWithLifecycle()
            RoomListScreen(
                title = viewModel.title,
                rooms = rooms,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.profileGraph(navController: NavHostController) {
    navigation(startDestination = Routes.PROFILE, route = Routes.PROFILE_GRAPH) {
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateTheme = { navController.navigate(Routes.THEME) },
                onNavigateBackup = { navController.navigate(Routes.BACKUP) },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateGroup = { navController.navigate(Routes.GROUP) },
                onNavigateAbout = { navController.navigate(Routes.ABOUT) },
                onNavigateEvents = { navController.navigate(Routes.EVENT_LIST) },
            )
        }
        composable(Routes.EVENT_LIST) {
            EventListScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenDocument = { documentType ->
                    navController.navigate(Routes.legalDocument(documentType))
                },
                onOpenVersionDetails = { navController.navigate(Routes.VERSION_DETAILS) },
            )
        }
        composable(Routes.VERSION_DETAILS) {
            VersionDetailsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.LEGAL_DOCUMENT,
            arguments = listOf(
                navArgument("documentType") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val documentType = LegalDocumentType.fromRoute(
                checkNotNull(backStackEntry.arguments?.getString("documentType")),
            ) ?: LegalDocumentType.PRIVACY_POLICY
            LegalDocumentScreen(
                documentType = documentType,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.THEME) {
            ThemeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.GROUP) {
            GroupScreen(onBack = { navController.popBackStack() })
        }
    }
}
