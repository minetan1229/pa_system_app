package com.patoolbox.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.patoolbox.core.designsystem.component.PaCard
import com.patoolbox.core.designsystem.theme.LocalPaDimens
import com.patoolbox.core.ui.R

/**
 * 本番中に見つけたハウリング。
 *
 * 周波数だけでなく音名と 1/3 オクターブ帯域を持つのは、
 * 打つ手が「卓のどのつまみを触るか」だから。
 * 「1.02kHz」より「1k の帯域」の方が、グライコの前では速い。
 *
 * [com.patoolbox.core.dsp.FeedbackDetector] の検出結果から作る。ここに dsp への依存を
 * 持たせないのは、core:ui が core:dsp に依存しなくて済むようにするため
 * （画面はこの4値だけあれば描けるので、変換は呼び出し側の ViewModel が行う）。
 */
data class FeedbackAlert(
    val frequencyHz: Double,
    val noteName: String,
    val bandLabel: String,
    /** 周囲より何dB突出しているか。値そのものより「どれだけ危ないか」の目安 */
    val prominenceDb: Double,
)

/**
 * ハウリングの表示。本番タイマー・本番万能コントローラーで共有する。
 *
 * 鳴っている間は面ごと赤くする。本番中は数字を読む余裕が無いので、
 * まず色の変化で気づけること、次に周波数が読めること、の順で作ってある。
 *
 * 収まった後も「直前に出た」を残すのは、対処が済んだかどうかを
 * 曲間に確かめたいため。消すのは手動。
 */
@Composable
fun FeedbackAlertPanel(
    current: FeedbackAlert?,
    last: FeedbackAlert?,
    onClearLast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = LocalPaDimens.current

    if (current != null) {
        PaCard(
            modifier = modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            borderColor = MaterialTheme.colorScheme.error,
            contentPadding = dimens.spaceMd,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.feedback_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = stringResource(R.string.feedback_value, formatHz(current.frequencyHz)),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                text = stringResource(
                    R.string.feedback_detail,
                    current.noteName,
                    current.bandLabel,
                    "%.0f".format(current.prominenceDb),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = stringResource(R.string.feedback_action),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        return
    }

    if (last != null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.feedback_last,
                    formatHz(last.frequencyHz),
                    last.noteName,
                    last.bandLabel,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClearLast) {
                Text(stringResource(R.string.feedback_clear))
            }
        }
        return
    }

    Text(
        text = stringResource(R.string.feedback_idle),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
