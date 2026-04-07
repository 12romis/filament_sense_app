package com.filament.sense.ui.screen.spools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun SpoolEditScreen(
    navController: NavController,
    viewModel: SpoolEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SpoolFormScreen(
        title = "Редагування котушки",
        state = state,
        onNameChange = viewModel::onNameChange,
        onColorChange = viewModel::onColorChange,
        onNominalWeightChange = viewModel::onNominalWeightChange,
        onBaselineWeightChange = viewModel::onBaselineWeightChange,
        onSave = {
            viewModel.save()
            navController.popBackStack()
        },
        onBack = { navController.popBackStack() },
    )
}
