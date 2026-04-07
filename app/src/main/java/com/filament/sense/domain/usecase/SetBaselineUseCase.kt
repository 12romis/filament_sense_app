package com.filament.sense.domain.usecase

import com.filament.sense.domain.repository.SpoolRepository
import javax.inject.Inject

class SetBaselineUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    suspend operator fun invoke(index: Int) = spoolRepository.saveBaseline(index)
}