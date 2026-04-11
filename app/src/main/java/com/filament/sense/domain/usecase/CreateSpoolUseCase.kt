package com.filament.sense.domain.usecase

import com.filament.sense.domain.repository.SpoolRepository
import javax.inject.Inject

class CreateSpoolUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    suspend operator fun invoke(name: String, colorArgb: Int, nominalWeight: Int, baselineWeight: Float) =
        spoolRepository.createSpool(name, colorArgb, nominalWeight, baselineWeight)
}
