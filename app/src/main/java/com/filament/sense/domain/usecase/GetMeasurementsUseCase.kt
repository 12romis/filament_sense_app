package com.filament.sense.domain.usecase

import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeasurementsUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    /** Повертає всі збережені вимірювання для [spoolId] (до 1000 записів). */
    operator fun invoke(spoolId: Int): Flow<List<Measurement>> =
        spoolRepository.getMeasurements(spoolId, 0L)
}