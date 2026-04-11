package com.filament.sense.domain.usecase

import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetThresholdsUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    operator fun invoke(): StateFlow<Triple<Int, Int, Int>> = spoolRepository.thresholds
}
