package com.filament.sense.domain.usecase

import com.filament.sense.domain.model.Measurement
import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBucketedMeasurementsUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    /** Повертає усереднені точки по 8-год кошиках за останні 30 днів. */
    operator fun invoke(spoolId: Int): Flow<List<Measurement>> {
        val sinceMs = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        return spoolRepository.getBucketedMeasurements(spoolId, sinceMs)
    }
}
