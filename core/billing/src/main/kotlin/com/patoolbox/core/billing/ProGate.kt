package com.patoolbox.core.billing

import com.patoolbox.core.model.ProStatus
import kotlinx.coroutines.flow.Flow

/**
 * Pro の有効判定への唯一の入口。
 *
 * 各 feature はここだけを見て、Play Billing の存在を知らない。
 * Phase 5 で実装を Play Billing 版に差し替えても feature 側は変更不要。
 */
interface ProGate {
    val proStatus: Flow<ProStatus>
}
