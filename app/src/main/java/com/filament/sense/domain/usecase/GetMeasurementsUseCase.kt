package com.filament.sense.domain.usecase

import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeasurementsUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    /** Повертає вимірювання для [spoolId] за останні [periodMs] мілісекунд. */
    operator fun invoke(spoolId: Int, periodMs: Long = 24 * 60 * 60 * 1000L): Flow<List<Measurement>> =
        spoolRepository.getMeasurements(spoolId, System.currentTimeMillis() - periodMs)
}