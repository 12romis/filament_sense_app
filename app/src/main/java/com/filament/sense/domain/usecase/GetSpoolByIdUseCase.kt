package com.filament.sense.domain.usecase

import com.filament.sense.domain.model.SpoolSlot
import com.filament.sense.domain.repository.SpoolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSpoolByIdUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    operator fun invoke(id: Int): Flow<SpoolSlot?> = spoolRepository.getSpoolById(id)
}
