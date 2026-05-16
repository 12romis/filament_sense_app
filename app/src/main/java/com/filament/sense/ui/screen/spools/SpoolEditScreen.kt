package com.filament.sense.ui.screen.spools

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch

@Composable
fun SpoolEditScreen(
    navController: NavController,
    viewModel: SpoolEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.navigateBack.collect { navController.popBackStack() }
    }

    SpoolFormScreen(
        title = "Редагування котушки",
        saveLabel = "Зберегти зміни",
        state = state,
        onNameChange = viewModel::onNameChange,
        onColorChange = viewModel::onColorChange,
        onNominalWeightChange = viewModel::onNominalWeightChange,
        onBaselineWeightChange = viewModel::onBaselineWeightChange,
        onSave = { viewModel.save() },
        onBack = { navController.popBackStack() },
        onDelete = {
            if (state.isActive) {
                scope.launch {
                    snackbarHostState.showSnackbar("Активна котушка не може бути видаленою")
                }
            } else {
                showDeleteDialog = true
            }
        },
        snackbarHostState = snackbarHostState,
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Видалити котушку?") },
            text = { Text("Ця дія є незворотньою. Котушку буде видалено назавжди.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                        navController.navigate("spools") {
                            popUpTo("spools") { inclusive = true }
                        }
                    },
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Скасувати")
                }
            },
        )
    }
}
