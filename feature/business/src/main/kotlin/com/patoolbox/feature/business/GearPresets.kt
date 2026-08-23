package com.patoolbox.feature.business

import com.patoolbox.core.model.GearCategory
import com.patoolbox.core.model.GearItem

/**
 * 「よくある機材」から選んで足すための下書き。
 *
 * **台帳を自動で埋めるのではない。** 機材台帳は数量・シリアル・状態を持つ
 * 実在の持ち物の記録なので、持ってもいない機材を勝手に1件登録すると
 * 台帳そのものが嘘になる。ここはあくまで「よく現場にある型番」を
 * 押しやすい形で並べておき、選んだら中身を確認・入力できる編集ダイアログを開く
 * ところまでで止める（実際に台帳へ足すかどうかは、そこで利用者が決める）。
 *
 * 選定基準は「現場でほぼ必ず名前が挙がる型番」。1社だけに寄らないよう
 * カテゴリごとに定番を1〜2点だけ置いてある。
 */
object GearPresets {
    val ALL: List<GearItem> = listOf(
        GearItem(category = GearCategory.SPEAKER, name = "メインスピーカー", maker = "JBL", modelName = "SRX835P"),
        GearItem(category = GearCategory.SPEAKER, name = "モニタースピーカー", maker = "Yamaha", modelName = "DXR12mkII"),
        GearItem(category = GearCategory.AMP, name = "ギターアンプ", maker = "Marshall", modelName = "JCM800 2203"),
        GearItem(category = GearCategory.AMP, name = "ベースアンプ", maker = "Ampeg", modelName = "SVT-4PRO"),
        GearItem(category = GearCategory.MIXER, name = "デジタル卓", maker = "Yamaha", modelName = "CL5"),
        GearItem(category = GearCategory.MIXER, name = "アナログ卓", maker = "Allen & Heath", modelName = "GL2800"),
        GearItem(category = GearCategory.MIC, name = "ボーカルマイク", maker = "SHURE", modelName = "SM58"),
        GearItem(category = GearCategory.MIC, name = "楽器用マイク", maker = "SHURE", modelName = "SM57"),
        GearItem(category = GearCategory.MIC, name = "コンデンサーマイク", maker = "AKG", modelName = "C414 XLII"),
        GearItem(category = GearCategory.DI, name = "パッシブDI", maker = "Radial", modelName = "ProDI"),
        GearItem(category = GearCategory.WIRELESS, name = "ワイヤレスマイク", maker = "SHURE", modelName = "QLXD24/SM58"),
        GearItem(category = GearCategory.WIRELESS, name = "イヤモニ送信機", maker = "Sennheiser", modelName = "EW IEM G4"),
        GearItem(category = GearCategory.OUTBOARD, name = "グラフィックEQ", maker = "dbx", modelName = "231s"),
        GearItem(category = GearCategory.CABLE, name = "マイクケーブル 10m", maker = "CANARE", modelName = "L-4E6S"),
        GearItem(category = GearCategory.CABLE, name = "スピコンケーブル 20m", maker = "CANARE", modelName = "4S6"),
        GearItem(category = GearCategory.STAND, name = "マイクスタンド", maker = "K&M", modelName = "210/9"),
        GearItem(category = GearCategory.STAND, name = "スピーカースタンド", maker = "K&M", modelName = "21435"),
        GearItem(category = GearCategory.POWER, name = "電源タップ", maker = "コンセントプラス", modelName = "20A 分電"),
    )
}
