# Keep LSPosed entry and hooker classes
-keep class com.volkeyhook.xposed.** { *; }
-keep class io.github.libxposed.api.** { *; }

# Keep hooker annotations
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keep class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
}
