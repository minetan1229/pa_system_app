package com.patoolbox.core.ui

import androidx.annotation.StringRes
import com.patoolbox.core.model.ConsoleType

/**
 * 卓の種類の表示名。設定画面とホームの初回オンボーディングで同じ文言を使う
 * （feature 同士は依存しないので、共有する文言はここに置く）。
 */
@StringRes
fun ConsoleType.labelRes(): Int = when (this) {
    ConsoleType.UNSET -> R.string.console_unset
    ConsoleType.ANALOG -> R.string.console_analog
    ConsoleType.DIGITAL -> R.string.console_digital
}

/** @return 説明文のリソースID。指定なしには説明を付けない（「何かを選ばされている」ように見えるため） */
@StringRes
fun ConsoleType.descriptionResOrNull(): Int? = when (this) {
    ConsoleType.UNSET -> null
    ConsoleType.ANALOG -> R.string.console_analog_desc
    ConsoleType.DIGITAL -> R.string.console_digital_desc
}
