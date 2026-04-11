package com.filament.sense.domain.usecase

import com.filament.sense.domain.repository.SpoolRepository
import javax.inject.Inject

class UpdateSpoolConfigUseCase @Inject constructor(
    private val spoolRepository: SpoolRepository,
) {
    suspend operator fun invoke(
        id: Int,
        name: String,
        colorArgb: Int,
        nominalWeight: Int,
        baselineWeight: Float,
    ) = spoolRepository.updateSpoolConfig(id, name, colorArgb, nominalWeight, baselineWeight)
}