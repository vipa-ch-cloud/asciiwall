package dev.asciiwall.app.core

import org.json.JSONArray
import org.json.JSONObject

/** Тематика сцены. Название — строка cat_<id>. */
enum class SceneCategory(val id: String) {
    NATURE("nature"), SPACE("space"), FRACTAL("fractal"), TECH("tech"),
    GAMES("games"), ART("art"), ANIMALS("animals"), ARCH("arch");

    /** Имя строкового ресурса: cat_<id>. */
    val resName: String get() = "cat_$id"
}

/**
 * Тип сцены. API для UI:
 *  - [isStatic] — статичная картинка (true) или анимация (false);
 *  - [category] — тематика ([SceneCategory]);
 *  - [id] / [resName] — "matrix" / "scene_matrix" (строка с названием в strings.xml);
 *  - [kindResName] — "cat_static" или "cat_animated" (строки для вкладок).
 */
enum class SceneType(val isStatic: Boolean, val category: SceneCategory) {
    // --- анимации ---
    MATRIX(false, SceneCategory.TECH), FIRE(false, SceneCategory.NATURE), PLASMA(false, SceneCategory.ART),
    WAVES(false, SceneCategory.NATURE), STARS(false, SceneCategory.SPACE), CLOCK(false, SceneCategory.TECH),
    DONUT(false, SceneCategory.TECH), LIFE(false, SceneCategory.GAMES), SNOW(false, SceneCategory.NATURE),
    RAIN(false, SceneCategory.NATURE), BREATH(false, SceneCategory.ART),
    MATRIX_UP(false, SceneCategory.TECH), MATRIX_STORM(false, SceneCategory.TECH), MATRIX_SLOW(false, SceneCategory.TECH),
    LAVA(false, SceneCategory.ART), OCEAN(false, SceneCategory.NATURE), VORTEX(false, SceneCategory.ART),
    HYPNO(false, SceneCategory.ART), AURORA(false, SceneCategory.NATURE), METEORS(false, SceneCategory.SPACE),
    GALAXY(false, SceneCategory.SPACE), WARP(false, SceneCategory.SPACE), TUNNEL(false, SceneCategory.ART),
    CLOCK_ANALOG(false, SceneCategory.TECH), CLOCK_BINARY(false, SceneCategory.TECH),
    SNAKE(false, SceneCategory.GAMES), PONG(false, SceneCategory.GAMES), MAZE(false, SceneCategory.GAMES),
    MANDEL_ZOOM(false, SceneCategory.FRACTAL), JULIA_ANIM(false, SceneCategory.FRACTAL),
    LISSAJOUS(false, SceneCategory.ART), SPIROGRAPH(false, SceneCategory.ART), PENDULUM(false, SceneCategory.ART),
    FIRE_TORCH(false, SceneCategory.NATURE), FIRE_EMBERS(false, SceneCategory.NATURE), STORM(false, SceneCategory.NATURE),
    LEAVES(false, SceneCategory.NATURE), FIREFLIES(false, SceneCategory.NATURE), DNA(false, SceneCategory.TECH),
    BRAINFUCK(false, SceneCategory.TECH), AQUARIUM(false, SceneCategory.ANIMALS), ECG(false, SceneCategory.TECH),
    EQUALIZER(false, SceneCategory.TECH), RIPPLES(false, SceneCategory.NATURE), CUBE(false, SceneCategory.TECH),
    GLOBE(false, SceneCategory.SPACE), RADAR(false, SceneCategory.TECH), FIREWORKS(false, SceneCategory.ART),
    CLOUDS(false, SceneCategory.NATURE),
    // --- статичные рисунки ---
    MOUNTAINS(true, SceneCategory.NATURE), LAKE(true, SceneCategory.NATURE), PINES(true, SceneCategory.NATURE),
    SNOWFLAKE(true, SceneCategory.NATURE), TOPOMAP(true, SceneCategory.NATURE),
    CASTLE(true, SceneCategory.ARCH), CITY(true, SceneCategory.ARCH), PYRAMIDS(true, SceneCategory.ARCH),
    CAT(true, SceneCategory.ANIMALS), OWL(true, SceneCategory.ANIMALS), PENGUIN(true, SceneCategory.ANIMALS),
    BUTTERFLY(true, SceneCategory.ANIMALS), WEB(true, SceneCategory.ANIMALS),
    EARTH(true, SceneCategory.SPACE), SATURN(true, SceneCategory.SPACE), MOON(true, SceneCategory.SPACE),
    MANDELBROT(true, SceneCategory.FRACTAL), JULIA(true, SceneCategory.FRACTAL),
    SIERPINSKI(true, SceneCategory.FRACTAL), CARPET(true, SceneCategory.FRACTAL),
    HILBERT(true, SceneCategory.FRACTAL), TREE(true, SceneCategory.FRACTAL),
    MOSAIC(true, SceneCategory.ART), TRUCHET(true, SceneCategory.ART), SUNFLOWER(true, SceneCategory.ART),
    MANDALA(true, SceneCategory.ART), ROSE(true, SceneCategory.ART), SYNTHWAVE(true, SceneCategory.ART),
    DROID(true, SceneCategory.TECH);

    /** Короткий id: имя в нижнем регистре, как в пресетах. */
    val id: String get() = name.lowercase()

    /** Имя строки с названием: scene_<id>. */
    val resName: String get() = "scene_$id"

    /** Имя строки вкладки: cat_static или cat_animated. */
    val kindResName: String get() = if (isStatic) "cat_static" else "cat_animated"

    companion object {
        val staticScenes: List<SceneType> = entries.filter { it.isStatic }
        val animatedScenes: List<SceneType> = entries.filter { !it.isStatic }
        fun inCategory(c: SceneCategory): List<SceneType> = entries.filter { it.category == c }
    }
}

enum class Palette { MONO, GREEN, AMBER, NEON, FIRE, ICE, DYNAMIC }

const val DEFAULT_RAMP = " .:-=+*#%@"

/** Полное описание одних обоев. Чистый Kotlin, без Android-зависимостей (кроме org.json). */
data class Config(
    val id: String,
    val name: String,
    val builtin: Boolean,
    val scene: SceneType,
    val ramp: String = DEFAULT_RAMP,
    val palette: Palette = Palette.GREEN,
    val speed: Float = 1f,
    val density: Float = 1f,
    val fontDp: Float = 9f,
    val brightness: Float = 1f,
    val glow: Boolean = false,
    val fps: Int = 30,
    val shuffle: Boolean = false,
    val parallax: Boolean = false,
) {
    fun sanitized(): Config {
        val r = if (ramp.length >= 2) ramp.take(32) else DEFAULT_RAMP
        return copy(
            ramp = r,
            speed = speed.coerceIn(0.1f, 3f),
            density = density.coerceIn(0.2f, 2f),
            fontDp = fontDp.coerceIn(5f, 28f),
            brightness = brightness.coerceIn(0.1f, 1f),
            fps = fps.coerceIn(5, 60),
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("name", name); put("scene", scene.name)
        put("ramp", ramp); put("palette", palette.name)
        put("speed", speed.toDouble()); put("density", density.toDouble())
        put("fontDp", fontDp.toDouble()); put("brightness", brightness.toDouble())
        put("glow", glow); put("fps", fps); put("shuffle", shuffle); put("parallax", parallax)
    }

    companion object {
        fun fromJson(o: JSONObject, builtin: Boolean = false): Config {
            val scene = runCatching { SceneType.valueOf(o.optString("scene")) }.getOrDefault(SceneType.PLASMA)
            val pal = runCatching { Palette.valueOf(o.optString("palette")) }.getOrDefault(Palette.GREEN)
            return Config(
                id = o.optString("id").ifEmpty { newId() },
                name = o.optString("name").ifEmpty { "Config" },
                builtin = builtin,
                scene = scene,
                ramp = o.optString("ramp", DEFAULT_RAMP),
                palette = pal,
                speed = o.optDouble("speed", 1.0).toFloat(),
                density = o.optDouble("density", 1.0).toFloat(),
                fontDp = o.optDouble("fontDp", 9.0).toFloat(),
                brightness = o.optDouble("brightness", 1.0).toFloat(),
                glow = o.optBoolean("glow", false),
                fps = o.optInt("fps", 30),
                shuffle = o.optBoolean("shuffle", false),
                parallax = o.optBoolean("parallax", false),
            ).sanitized()
        }

        fun newId(): String = "c" + java.util.UUID.randomUUID().toString().take(8)

        fun listToJson(list: List<Config>): String = JSONObject().apply {
            put("asciiwall", 1)
            put("configs", JSONArray().also { a -> list.forEach { a.put(it.toJson()) } })
        }.toString(2)

        /** Принимает {"configs":[...]}, массив или одиночный объект. */
        fun listFromJson(text: String): List<Config> {
            val t = text.trim()
            val out = ArrayList<Config>()
            if (t.startsWith("[")) {
                val a = JSONArray(t)
                for (i in 0 until a.length()) out.add(fromJson(a.getJSONObject(i)))
            } else {
                val o = JSONObject(t)
                val a = o.optJSONArray("configs")
                if (a != null) for (i in 0 until a.length()) out.add(fromJson(a.getJSONObject(i)))
                else if (o.has("scene")) out.add(fromJson(o))
            }
            return out
        }
    }
}

object Presets {
    private fun p(
        id: String, scene: SceneType, ramp: String, pal: Palette,
        speed: Float = 1f, density: Float = 1f, font: Float = 9f, bright: Float = 1f,
        glow: Boolean = false, shuffle: Boolean = false, fps: Int = 30, parallax: Boolean = false,
    ) = Config(id, id, true, scene, ramp, pal, speed, density, font, bright, glow, fps, shuffle, parallax)

    val all: List<Config> = listOf(
        p("matrix", SceneType.MATRIX, " 01234789:<>=ZXC*+", Palette.GREEN, 1f, 1f, 10f, 1f, glow = true, shuffle = true),
        p("fire", SceneType.FIRE, " .,:;=+*#%@", Palette.FIRE, 1f, 1f, 9f, 1f, glow = true),
        p("plasma", SceneType.PLASMA, " .:-=+*#%@", Palette.NEON, 0.8f, 1f, 9f, 0.85f),
        p("waves", SceneType.WAVES, " .-~=*#@", Palette.NEON, 0.9f, 1f, 9f, 0.9f, parallax = true),
        p("stars", SceneType.STARS, " .'`+*@", Palette.MONO, 1f, 1f, 8f, 1f, glow = true),
        p("clock", SceneType.CLOCK, " .:-=+*#%@", Palette.AMBER, 1f, 1f, 8f, 0.95f, glow = true, fps = 20),
        p("donut", SceneType.DONUT, " .,-~:;=!*#\$@", Palette.GREEN, 1f, 1f, 8f, 1f, glow = true),
        p("life", SceneType.LIFE, " .oO0@", Palette.DYNAMIC, 1f, 1f, 9f, 1f, fps = 20),
        p("snow", SceneType.SNOW, " .:*+", Palette.MONO, 0.8f, 1f, 10f, 0.9f, parallax = true),
        p("rain", SceneType.RAIN, " ./|!", Palette.ICE, 1f, 1f, 10f, 0.9f),
        p("breath", SceneType.BREATH, " .:-=+*#%@", Palette.DYNAMIC, 0.7f, 1f, 11f, 0.9f, fps = 20),
        p("matrix_up", SceneType.MATRIX_UP, " 01234789:<>=ZXC*+", Palette.GREEN, 1f, 1f, 10f, 1f, glow = true, shuffle = true),
        p("matrix_storm", SceneType.MATRIX_STORM, " 01234789:<>=ZXC*+", Palette.GREEN, 1f, 1f, 10f, 1f, glow = true, shuffle = true),
        p("matrix_slow", SceneType.MATRIX_SLOW, " 01ABCDEF:<>=*+", Palette.GREEN, 1f, 1f, 10f, 1f, glow = true, shuffle = true),
        p("lava", SceneType.LAVA, " .,:;=+*#%&@", Palette.FIRE, 0.8f, 1f, 9f, 1f, glow = true),
        p("ocean", SceneType.OCEAN, " .-~=*#@", Palette.ICE, 0.9f, 1f, 9f, 1f),
        p("vortex", SceneType.VORTEX, " .:-=+*#%@", Palette.NEON, 0.9f, 1f, 9f, 1f),
        p("hypno", SceneType.HYPNO, " .oO0@", Palette.DYNAMIC, 0.8f, 1f, 9f, 1f),
        p("aurora", SceneType.AURORA, " .:-=+*#%@", Palette.NEON, 0.7f, 1f, 9f, 1f, glow = true),
        p("meteors", SceneType.METEORS, " .'`+*@", Palette.MONO, 1f, 1f, 8f, 1f, glow = true),
        p("galaxy", SceneType.GALAXY, " .'`+*@", Palette.ICE, 0.8f, 1f, 8f, 1f, glow = true),
        p("warp", SceneType.WARP, " .'`+*@", Palette.ICE, 1f, 1f, 8f, 1f, glow = true),
        p("tunnel", SceneType.TUNNEL, " .:-=+*#%@", Palette.NEON, 0.9f, 1f, 9f, 1f),
        p("clock_analog", SceneType.CLOCK_ANALOG, " .:-=+*#%@", Palette.AMBER, 1f, 1f, 8f, 1f, glow = true, fps = 20),
        p("clock_binary", SceneType.CLOCK_BINARY, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 8f, 1f, glow = true, fps = 20),
        p("snake", SceneType.SNAKE, " .o0@", Palette.GREEN, 1f, 1f, 9f, 1f, fps = 20),
        p("pong", SceneType.PONG, " .:*#@", Palette.MONO, 1f, 1f, 9f, 1f, fps = 30),
        p("maze", SceneType.MAZE, " .:*#%@", Palette.GREEN, 1f, 1f, 9f, 1f, fps = 20),
        p("mandel_zoom", SceneType.MANDEL_ZOOM, " .:-=+*#%@", Palette.NEON, 1f, 1f, 8f, 1f, fps = 15),
        p("julia_anim", SceneType.JULIA_ANIM, " .:-=+*#%@", Palette.FIRE, 1f, 1f, 8f, 1f, fps = 15),
        p("lissajous", SceneType.LISSAJOUS, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 9f, 1f, glow = true),
        p("spirograph", SceneType.SPIROGRAPH, " .:-=+*#%@", Palette.NEON, 1f, 1f, 9f, 1f, glow = true),
        p("pendulum", SceneType.PENDULUM, " .:-=+*#%@", Palette.AMBER, 1f, 1f, 9f, 1f, glow = true),
        p("fire_torch", SceneType.FIRE_TORCH, " .,:;=+*#%@", Palette.FIRE, 1f, 1f, 9f, 1f, glow = true),
        p("fire_embers", SceneType.FIRE_EMBERS, " .,:;=+*#%@", Palette.FIRE, 1f, 1f, 9f, 1f, glow = true),
        p("storm", SceneType.STORM, " ./|!", Palette.ICE, 1f, 1f, 10f, 1f),
        p("leaves", SceneType.LEAVES, " .:*+o@", Palette.AMBER, 0.8f, 1f, 10f, 1f, parallax = true),
        p("fireflies", SceneType.FIREFLIES, " .:*+@", Palette.AMBER, 1f, 1f, 9f, 1f, glow = true),
        p("dna", SceneType.DNA, " .:-=+*#%@", Palette.NEON, 1f, 1f, 9f, 1f, glow = true),
        p("brainfuck", SceneType.BRAINFUCK, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 9f, 1f, glow = true),
        p("aquarium", SceneType.AQUARIUM, " .:-=+*#%@", Palette.ICE, 1f, 1f, 9f, 1f),
        p("ecg", SceneType.ECG, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 9f, 1f, glow = true),
        p("equalizer", SceneType.EQUALIZER, " .:-=+*#%@", Palette.NEON, 1f, 1f, 9f, 1f),
        p("ripples", SceneType.RIPPLES, " .-~=*#@", Palette.ICE, 1f, 1f, 9f, 1f),
        p("cube", SceneType.CUBE, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 9f, 1f, glow = true),
        p("globe", SceneType.GLOBE, " .:-=+*#%@", Palette.ICE, 1f, 1f, 8f, 1f, fps = 20),
        p("radar", SceneType.RADAR, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 9f, 1f, glow = true),
        p("fireworks", SceneType.FIREWORKS, " .:*+#@", Palette.DYNAMIC, 1f, 1f, 9f, 1f, glow = true),
        p("clouds", SceneType.CLOUDS, " .:-=+*#%@", Palette.ICE, 0.6f, 1f, 9f, 1f, fps = 20),
        p("mountains", SceneType.MOUNTAINS, " .,:;=+*#%&@", Palette.ICE, 1f, 1f, 7f, 1f, fps = 5),
        p("lake", SceneType.LAKE, " .,:;=+*#%&@", Palette.NEON, 1f, 1f, 7f, 1f, fps = 5),
        p("pines", SceneType.PINES, " .,:;=+*#%&@", Palette.GREEN, 1f, 1f, 7f, 1f, fps = 5),
        p("snowflake", SceneType.SNOWFLAKE, " .:-=+*#%@", Palette.ICE, 1f, 1f, 7f, 1f, fps = 5),
        p("topomap", SceneType.TOPOMAP, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 7f, 1f, fps = 5),
        p("castle", SceneType.CASTLE, " .,:;=+*#%&@", Palette.MONO, 1f, 1f, 7f, 1f, fps = 5),
        p("city", SceneType.CITY, " .,:;=+*#%&@", Palette.AMBER, 1f, 1f, 7f, 1f, fps = 5),
        p("pyramids", SceneType.PYRAMIDS, " .,:;=+*#%&@", Palette.AMBER, 1f, 1f, 7f, 1f, fps = 5),
        p("cat", SceneType.CAT, " .,:;=+*#%&@", Palette.MONO, 1f, 1f, 7f, 1f, fps = 5),
        p("owl", SceneType.OWL, " .,:;=+*#%&@", Palette.AMBER, 1f, 1f, 7f, 1f, fps = 5),
        p("penguin", SceneType.PENGUIN, " .,:;=+*#%&@", Palette.ICE, 1f, 1f, 7f, 1f, fps = 5),
        p("butterfly", SceneType.BUTTERFLY, " .,:;=+*#%&@", Palette.NEON, 1f, 1f, 7f, 1f, fps = 5),
        p("web", SceneType.WEB, " .:-=+*#%@", Palette.MONO, 1f, 1f, 7f, 1f, fps = 5),
        p("earth", SceneType.EARTH, " .,:;=+*#%&@", Palette.ICE, 1f, 1f, 7f, 1f, fps = 5),
        p("saturn", SceneType.SATURN, " .,:;=+*#%&@", Palette.AMBER, 1f, 1f, 7f, 1f, fps = 5),
        p("moon", SceneType.MOON, " .,:;=+*#%&@", Palette.MONO, 1f, 1f, 7f, 1f, fps = 5),
        p("mandelbrot", SceneType.MANDELBROT, " .,:;=+*#%&@", Palette.NEON, 1f, 1f, 7f, 1f, fps = 5),
        p("julia", SceneType.JULIA, " .,:;=+*#%&@", Palette.FIRE, 1f, 1f, 7f, 1f, fps = 5),
        p("sierpinski", SceneType.SIERPINSKI, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 7f, 1f, fps = 5),
        p("carpet", SceneType.CARPET, " .:-=+*#%@", Palette.AMBER, 1f, 1f, 7f, 1f, fps = 5),
        p("hilbert", SceneType.HILBERT, " .:-=+*#%@", Palette.NEON, 1f, 1f, 7f, 1f, fps = 5),
        p("tree", SceneType.TREE, " .:-=+*#%@", Palette.GREEN, 1f, 1f, 7f, 1f, fps = 5),
        p("mosaic", SceneType.MOSAIC, " .,:;=+*#%&@", Palette.DYNAMIC, 1f, 1f, 7f, 1f, fps = 5),
        p("truchet", SceneType.TRUCHET, " .:-=+*#%@", Palette.NEON, 1f, 1f, 7f, 1f, fps = 5),
        p("sunflower", SceneType.SUNFLOWER, " .:-=+*#%@", Palette.AMBER, 1f, 1f, 7f, 1f, fps = 5),
        p("mandala", SceneType.MANDALA, " .,:;=+*#%&@", Palette.DYNAMIC, 1f, 1f, 7f, 1f, fps = 5),
        p("rose", SceneType.ROSE, " .:-=+*#%@", Palette.FIRE, 1f, 1f, 7f, 1f, fps = 5),
        p("synthwave", SceneType.SYNTHWAVE, " .,:;=+*#%&@", Palette.NEON, 1f, 1f, 7f, 1f, fps = 5),
        p("droid", SceneType.DROID, " .,:;=+*#%&@", Palette.GREEN, 1f, 1f, 7f, 1f, fps = 5),
    )

    fun byId(id: String): Config? = all.firstOrNull { it.id == id }
}
