package com.patoolbox.feature.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.component.PaIllustration
import com.patoolbox.core.designsystem.component.PaNotice
import com.patoolbox.core.designsystem.component.PaPanel
import com.patoolbox.core.designsystem.component.PaScene
import com.patoolbox.core.designsystem.component.PaSectionHeader
import com.patoolbox.core.designsystem.component.PaTone
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.model.ToolId
import com.patoolbox.core.ui.component.PaToolScaffold

/**
 * 校正の読みもの。
 *
 * 校正画面（[CalibrationScreen]）は「合わせる操作」だけを置いてある。
 * 実際に詰まるのは操作ではなく、**手元に基準が無いときにどうするか**なので、
 * その判断をこちらに分けた。
 *
 * 一番長く書いてあるのが「端末1台のとき」なのは、それが一番多い状況で、
 * かつ正直に書くと「絶対値は作れない」という受け入れにくい結論になるため。
 * 代わりに何ができるのかを同じ画面に並べておかないと、
 * 読んだ人は結局それらしい数字を手で入れてしまう。
 */
@Composable
fun CalibrationGuideScreen(
    onBack: () -> Unit,
    onOpenCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    PaToolScaffold(
        tool = ToolId.SPL_METER,
        onBack = onBack,
        modifier = modifier,
        title = stringResource(R.string.guide_title),
        subtitle = stringResource(R.string.guide_role),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimens.gutter),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMd),
        ) {
            Column(modifier = Modifier.padding(top = dimens.spaceMd)) {
                PaPanel(
                    title = stringResource(R.string.guide_what_title),
                ) {
                    Body(stringResource(R.string.guide_what_body))
                }
            }

            PaSectionHeader(
                title = stringResource(R.string.guide_ways_title),
                subtitle = stringResource(R.string.guide_ways_subtitle),
                modifier = Modifier.padding(top = dimens.spaceSm),
            )

            PaCard(modifier = Modifier.fillMaxWidth()) {
                PaIllustration(
                    scene = PaScene.CALIBRATION,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                )
            }

            PaPanel(title = stringResource(R.string.guide_way_calibrator_title)) {
                Body(stringResource(R.string.guide_way_calibrator_body))
            }

            PaPanel(title = stringResource(R.string.guide_way_meter_title)) {
                Body(stringResource(R.string.guide_way_meter_body))
            }

            // 1台のときだけ帯で囲う。ここだけ結論が「合わせない」なので、
            // 他の2つと同じ見た目にすると読み飛ばされる
            PaNotice(
                title = stringResource(R.string.guide_way_alone_title),
                body = stringResource(R.string.guide_way_alone_body),
                tone = PaTone.WARNING,
            )

            PaCard(modifier = Modifier.fillMaxWidth()) {
                PaIllustration(
                    scene = PaScene.ONE_PHONE,
                    contentDescription = stringResource(R.string.guide_alone_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                )
            }

            PaSectionHeader(
                title = stringResource(R.string.guide_alone_steps_title),
                subtitle = stringResource(R.string.guide_alone_steps_subtitle),
                modifier = Modifier.padding(top = dimens.spaceSm),
            )

            AloneSteps.forEach { step ->
                PaPanel(title = stringResource(step.titleRes)) {
                    Body(stringResource(step.bodyRes))
                }
            }

            PaNotice(
                title = stringResource(R.string.guide_dontdo_title),
                body = stringResource(R.string.guide_dontdo_body),
                tone = PaTone.DANGER,
            )

            PaSectionHeader(
                title = stringResource(R.string.guide_usable_title),
                subtitle = stringResource(R.string.guide_usable_subtitle),
                modifier = Modifier.padding(top = dimens.spaceSm),
            )
            PaCard(modifier = Modifier.fillMaxWidth()) {
                Body(stringResource(R.string.guide_usable_body))
            }

            PaNotice(
                title = stringResource(R.string.guide_legal_title),
                body = stringResource(R.string.guide_legal_body),
                tone = PaTone.INFO,
            )

            Button(
                onClick = onOpenCalibration,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimens.spaceXl),
            ) {
                Text(stringResource(R.string.guide_open_calibration))
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * 1台だけのときの手順。
 *
 * 順番に意味がある。3（上限）を4（下限）より先にしているのは、
 * 上限を知らないまま本番を測ると、頭打ちした数字を記録に残してしまうため。
 */
private data class GuideStep(val titleRes: Int, val bodyRes: Int)

private val AloneSteps = listOf(
    GuideStep(R.string.guide_step1_title, R.string.guide_step1_body),
    GuideStep(R.string.guide_step2_title, R.string.guide_step2_body),
    GuideStep(R.string.guide_step3_title, R.string.guide_step3_body),
    GuideStep(R.string.guide_step4_title, R.string.guide_step4_body),
    GuideStep(R.string.guide_step5_title, R.string.guide_step5_body),
    GuideStep(R.string.guide_step6_title, R.string.guide_step6_body),
    GuideStep(R.string.guide_step7_title, R.string.guide_step7_body),
)
