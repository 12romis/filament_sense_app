package com.filament.sense.domain.usecase

import com.filament.sense.domain.repository.SpoolRepository
import javax.inject.Inject

class SetThresholdsUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    suspend operator fun invoke(warning: Int, critical: Int, empty: Int) =
        spoolRepository.setThresholds(warning, critical, empty)
}
