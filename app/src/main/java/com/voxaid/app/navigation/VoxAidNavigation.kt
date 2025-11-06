package com.voxaid.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voxaid.feature.main.category.CategoryScreen
import com.voxaid.feature.main.loading.LoadingScreen
import com.voxaid.feature.main.menu.MainMenuScreen

/**
 * Navigation routes for VoxAid app.
 *
 * Updated: Emergency mode excludes bandaging (not an emergency protocol)
 */
sealed class VoxAidRoute(val route: String) {
    data object Loading : VoxAidRoute("loading")
    data object MainMenu : VoxAidRoute("main_menu")
    data object Category : VoxAidRoute("category/{mode}") {
        fun createRoute(mode: String) = "category/$mode"
        const val ARG_MODE = "mode"
    }
    data object ProtocolVariant : VoxAidRoute("variant/{mode}/{protocol}") {
        fun createRoute(mode: String, protocol: String) = "variant/$mode/$protocol"
        const val ARG_MODE = "mode"
        const val ARG_PROTOCOL = "protocol"
    }
    data object Instruction : VoxAidRoute("instruction/{mode}/{variant}") {
        fun createRoute(mode: String, variant: String) = "instruction/$mode/$variant"
        const val ARG_MODE = "mode"
        const val ARG_VARIANT = "variant"
    }
    data object Emergency : VoxAidRoute("emergency/{protocol}") {
        fun createRoute(protocol: String) = "emergency/$protocol"
        const val ARG_PROTOCOL = "protocol"
    }
}

/**
 * Main navigation host for VoxAid.
 */
@Composable
fun VoxAidNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = VoxAidRoute.Loading.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Loading screen
        composable(VoxAidRoute.Loading.route) {
            LoadingScreen(
                onNavigateToMenu = {
                    navController.navigate(VoxAidRoute.MainMenu.route) {
                        popUpTo(VoxAidRoute.Loading.route) { inclusive = true }
                    }
                }
            )
        }

        // Main menu
        composable(VoxAidRoute.MainMenu.route) {
            MainMenuScreen(
                onModeSelected = { mode ->
                    navController.navigate(VoxAidRoute.Category.createRoute(mode))
                }
            )
        }

        // Category selection
        composable(
            route = VoxAidRoute.Category.route,
            arguments = listOf(
                navArgument(VoxAidRoute.Category.ARG_MODE) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString(VoxAidRoute.Category.ARG_MODE)
                ?: "instructional"

            CategoryScreen(
                mode = mode,
                onProtocolSelected = { protocol ->
                    // Prevent bandaging in emergency mode
                    if (mode == "emergency" && protocol == "bandaging") {
                        timber.log.Timber.w("Blocked emergency bandaging navigation")
                        return@CategoryScreen
                    }

                    when {
                        // Emergency mode: Direct to emergency screen
                        mode == "emergency" -> {
                            navController.navigate(
                                VoxAidRoute.Emergency.createRoute("emergency_$protocol")
                            )
                        }
                        // Instructional: CPR goes direct, others to variants
                        protocol == "cpr" -> {
                            navController.navigate(
                                VoxAidRoute.Instruction.createRoute(mode, "cpr_learning")
                            )
                        }
                        // Other protocols: Variant selection
                        else -> {
                            navController.navigate(
                                VoxAidRoute.ProtocolVariant.createRoute(mode, protocol)
                            )
                        }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Protocol variant selection (for Heimlich and Bandaging in instructional mode)
        composable(
            route = VoxAidRoute.ProtocolVariant.route,
            arguments = listOf(
                navArgument(VoxAidRoute.ProtocolVariant.ARG_MODE) {
                    type = NavType.StringType
                },
                navArgument(VoxAidRoute.ProtocolVariant.ARG_PROTOCOL) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString(VoxAidRoute.ProtocolVariant.ARG_MODE)
                ?: "instructional"
            val protocol =
                backStackEntry.arguments?.getString(VoxAidRoute.ProtocolVariant.ARG_PROTOCOL)
                    ?: "heimlich"

            // Double-check: prevent emergency bandaging
            if (mode == "emergency" && protocol == "bandaging") {
                timber.log.Timber.e("Emergency bandaging route blocked - invalid state")
                navController.popBackStack()
                return@composable
            }

            com.voxaid.feature.main.variant.ProtocolVariantScreen(
                protocolId = protocol,
                mode = mode,
                onVariantSelected = { variant ->
                    navController.navigate(
                        VoxAidRoute.Instruction.createRoute(mode, variant)
                    )
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToInstructional = { variant ->
                    navController.navigate(
                        VoxAidRoute.Instruction.createRoute("instructional", variant)
                    )
                }
            )
        }

        // Instruction screen (learning mode)
        composable(
            route = VoxAidRoute.Instruction.route,
            arguments = listOf(
                navArgument(VoxAidRoute.Instruction.ARG_MODE) {
                    type = NavType.StringType
                },
                navArgument(VoxAidRoute.Instruction.ARG_VARIANT) {
                    type = NavType.StringType
                }
            )
        ) {
            com.voxaid.feature.instruction.InstructionScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Emergency screen (emergency mode - CPR and Heimlich only)
        composable(
            route = VoxAidRoute.Emergency.route,
            arguments = listOf(
                navArgument(VoxAidRoute.Emergency.ARG_PROTOCOL) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val protocol = backStackEntry.arguments?.getString(VoxAidRoute.Emergency.ARG_PROTOCOL)
                ?: "emergency_cpr"

            // Validate: Only CPR and Heimlich allowed
            if (!protocol.contains("cpr") && !protocol.contains("heimlich")) {
                timber.log.Timber.e("Invalid emergency protocol: $protocol")
                navController.popBackStack()
                return@composable
            }

            com.voxaid.feature.instruction.emergency.EmergencyScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}