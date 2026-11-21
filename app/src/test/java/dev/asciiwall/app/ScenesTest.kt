package dev.asciiwall.app

import dev.asciiwall.app.core.*
import org.junit.Assert.*
import org.junit.Test

class ScenesTest {
    private val sizes = listOf(184 to 92, 60 to 130, 20 to 10, 1 to 1, 300 to 40)

    @Test fun allPresetsRunAllSizes() {
        assertTrue(Presets.all.size >= 8)
        for (p in Presets.all) for ((c, r) in sizes) {
            val s = Scene.create(p.scene)
            s.resize(c, r)
            val out = FloatArray(c * r)
            var t = 0f
            var maxV = 0f
            for (f in 0 until 200) {
                t += 1f / 30f
                s.update(t, 1f / 30f, p.density, out)
                for (v in out) {
                    assertFalse("${p.id} NaN", v.isNaN())
                    assertTrue("${p.id} range $v", v >= -0.0001f && v <= 1.0001f)
                    if (v > maxV) maxV = v
                }
            }
            if (c * r >= 200) assertTrue("${p.id} ${c}x$r draws nothing", maxV > 0.1f)
        }
    }

    @Test fun densityExtremes() {
        for (p in Presets.all) for (d in listOf(0.2f, 2f)) {
            val s = Scene.create(p.scene); s.resize(80, 40)
            val out = FloatArray(80 * 40)
            for (f in 0 until 60) s.update(f / 30f, 1f / 30f, d, out)
        }
    }

    @Test fun jsonRoundTrip() {
        val list = Presets.all.map { it.copy(builtin = false) }
        val back = Config.listFromJson(Config.listToJson(list))
        assertEquals(list.size, back.size)
        for (i in list.indices) assertEquals(list[i].sanitized(), back[i])
        assertEquals(1, Config.listFromJson("{\"scene\":\"FIRE\",\"name\":\"x\"}").size)
    }

    @Test fun everySceneTypeRuns() {
        for (type in SceneType.entries) for ((c, r) in sizes) {
            val s = Scene.create(type)
            s.resize(c, r)
            val out = FloatArray(c * r)
            var maxV = 0f
            for (f in 0 until 120) {
                s.update(f / 30f + 0.5f, 1f / 30f, 1f, out)
                for (v in out) {
                    assertFalse("$type NaN", v.isNaN())
                    assertTrue("$type range $v", v >= -0.0001f && v <= 1.0001f)
                    if (v > maxV) maxV = v
                }
            }
            if (c * r >= 200) assertTrue("$type ${c}x$r draws nothing", maxV > 0.1f)
        }
    }

    @Test fun categoryApiAndPresetsCoverAllScenes() {
        for (type in SceneType.entries) {
            assertTrue(type.resName, type.resName == "scene_" + type.id)
            assertTrue(Presets.all.any { it.scene == type })
            assertTrue(SceneType.inCategory(type.category).contains(type))
        }
        assertEquals(SceneType.entries.size, SceneType.staticScenes.size + SceneType.animatedScenes.size)
        assertTrue(SceneType.staticScenes.size >= 20 && SceneType.animatedScenes.size >= 40)
        assertTrue(SceneType.staticScenes.all { it.isStatic })
    }

    @Test fun stringsExistInBothLocales() {
        val root = java.io.File("src/main/res")
        for (dir in listOf("values", "values-en")) {
            val text = java.io.File(root, "$dir/strings.xml").readText()
            for (type in SceneType.entries) assertTrue("$dir ${type.resName}", text.contains("name=\"${type.resName}\""))
            for (cat in SceneCategory.entries) assertTrue("$dir ${cat.resName}", text.contains("name=\"${cat.resName}\""))
            assertTrue(text.contains("name=\"cat_static\"") && text.contains("name=\"cat_animated\""))
        }
    }
}
