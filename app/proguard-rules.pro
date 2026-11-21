# Wallpaper service and config model are referenced from the manifest / via reflection-like paths
-keep class dev.asciiwall.app.wall.AsciiWallpaperService { *; }
-keep class dev.asciiwall.app.core.Config { *; }
-keep class dev.asciiwall.app.core.Config$* { *; }
-keepclassmembers enum dev.asciiwall.app.core.** { *; }
