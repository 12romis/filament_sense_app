package com.filament.sense.domain.usecase

import com.filament.sense.domain.repository.SpoolRepository
import javax.inject.Inject

class DeleteSpoolUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    suspend operator fun invoke(id: Int) = spoolRepository.deleteSpool(id)
}
