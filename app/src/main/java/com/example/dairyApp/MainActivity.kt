package com.example.dairyApp

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dairyApp.diary.DiaryPageViewModel
import com.example.dairyApp.diary.EventViewModel
import com.example.dairyApp.features.CreateEventScreen
import com.example.dairyApp.features.DiaryEntryListScreen
import com.example.dairyApp.features.DiaryPageListScreen
import com.example.dairyApp.features.EventListScreen
import com.example.dairyApp.features.PhotoCaptionScreen
import com.example.dairyApp.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.EventList.route) {
        composable(Screen.EventList.route) {
            EventListScreen(
                eventViewModel = viewModel(
                    factory = EventViewModel.provideFactory(LocalContext.current.applicationContext as Application)
                ),
                onEventClick = { eventId ->
                    navController.navigate(Screen.DiaryPageList.createRoute(eventId))
                },
                onCreateEventClick = {
                    navController.navigate(Screen.CreateEvent.route)
                }
            )
        }

        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                navController = navController,
                eventViewModel = viewModel(
                    factory = EventViewModel.provideFactory(LocalContext.current.applicationContext as Application)
                )
            )
        }

        composable(
            route = Screen.DiaryPageList.route,
            arguments = listOf(navArgument(Screen.DiaryPageList.eventIdArg) { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString(Screen.DiaryPageList.eventIdArg)
            if (eventId != null) {
                DiaryPageListScreen(
                    navController = navController,
                    eventId = eventId,
                    navBackStackEntry = backStackEntry,
                    diaryPageViewModel = viewModel(
                        modelClass = DiaryPageViewModel::class.java,
                        viewModelStoreOwner = backStackEntry,
                        key = "diaryPageViewModel_with_eventId_$eventId",
                        factory = DiaryPageViewModel.provideFactory(
                            application = LocalContext.current.applicationContext as Application,
                            owner = backStackEntry,
                            eventId = eventId
                        )
                    )
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(
            route = Screen.DiaryEntryList.route,
            arguments = listOf(
                navArgument(Screen.DiaryEntryList.eventIdArg) { type = NavType.StringType },
                navArgument(Screen.DiaryEntryList.diaryPageNameArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString(Screen.DiaryEntryList.eventIdArg)
            val diaryPageName = backStackEntry.arguments?.getString(Screen.DiaryEntryList.diaryPageNameArg)
            if (eventId != null && diaryPageName != null) {
                DiaryEntryListScreen(
                    navController = navController,
                    eventId = eventId,
                    diaryPageName = diaryPageName,
                    navBackStackEntry = backStackEntry
                )
            } else {
                navController.popBackStack()
            }
        }

        composable(
            route = Screen.PhotoCaption.route,
            arguments = listOf(
                navArgument(Screen.PhotoCaption.eventIdArg) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString(Screen.PhotoCaption.eventIdArg)
            PhotoCaptionScreen(
                navController = navController,
                initialEventId = eventId
            )
        }
    }
}
