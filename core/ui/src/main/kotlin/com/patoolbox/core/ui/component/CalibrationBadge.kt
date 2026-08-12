package com.patoolbox.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.model.AudioInputType
import com.patoolbox.core.model.CalibrationConfidence
import com.patoolbox.core.model.CalibrationProfile
import com.patoolbox.core.ui.R

/**
 * 校正状態のバッジ。計測画面には常時出す。
 *
 * スマホの内蔵マイクは無校正だと数dB〜十数dBずれるので、
 * 「いまどの状態で測っているか」を隠さないことを設計の前提にしている。
 */
@Composable
fun CalibrationBadge(
    profile: CalibrationProfile,
    modifier: Modifier = Modifier,
) {
    val (textRes, container, content) = when (profile.confidence) {
        CalibrationConfidence.UNCALIBRATED -> Triple(
            R.string.calibration_uncalibrated,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )

        CalibrationConfidence.FAIR -> Triple(
            R.string.calibration_manual,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )

        CalibrationConfidence.GOOD -> Triple(
            if (profile.inputType == AudioInputType.USB) {
                R.string.calibration_external
            } else {
                R.string.calibration_calibrator
            },
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    PaChip(
        text = stringResource(textRes),
        containerColor = container,
        contentColor = content,
        modifier = modifier,
    )
}

/** 入力ソースが測定向きでないときに出す注意バッジ。 */
@Composable
fun InputSourceBadge(
    sourceLabel: String,
    isMeasurementGrade: Boolean,
    modifier: Modifier = Modifier,
) {
    PaChip(
        text = sourceLabel,
        containerColor = Color.Transparent,
        contentColor = if (isMeasurementGrade) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
        borderColor = if (isMeasurementGrade) {
            MaterialTheme.colorScheme.outlineVariant
        } else {
            MaterialTheme.colorScheme.error
        },
        modifier = modifier,
    )
}
