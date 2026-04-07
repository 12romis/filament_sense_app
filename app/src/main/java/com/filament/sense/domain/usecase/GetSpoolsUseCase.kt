package com.filament.sense.domain.usecase

import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSpoolsUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    operator fun invoke(): Flow<List<SpoolSlot>> = spoolRepository.spools
}