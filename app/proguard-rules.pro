# Navigation Compose の型安全ルートは kotlinx.serialization を使うので、
# @Serializable なクラスのシリアライザを残す。
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.patoolbox.**$$serializer { *; }
-keepclassmembers class com.patoolbox.** {
    *** Companion;
}
-keepclasseswithmembers class com.patoolbox.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room のエンティティは Kotlin のリフレクションを使わないので追加ルールは不要。
# Hilt / Dagger も生成コード側で完結する。
