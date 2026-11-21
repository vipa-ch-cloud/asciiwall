package dev.asciiwall.app.ui

import dev.asciiwall.app.R
import dev.asciiwall.app.core.SceneCategory
import dev.asciiwall.app.core.SceneType

// Явные ссылки R.string.*: getIdentifier() не виден shrinkResources и терял названия в release-сборке.

fun sceneLabelRes(s: SceneType): Int = when (s) {
    SceneType.MATRIX -> R.string.scene_matrix
    SceneType.FIRE -> R.string.scene_fire
    SceneType.PLASMA -> R.string.scene_plasma
    SceneType.WAVES -> R.string.scene_waves
    SceneType.STARS -> R.string.scene_stars
    SceneType.CLOCK -> R.string.scene_clock
    SceneType.DONUT -> R.string.scene_donut
    SceneType.LIFE -> R.string.scene_life
    SceneType.SNOW -> R.string.scene_snow
    SceneType.RAIN -> R.string.scene_rain
    SceneType.BREATH -> R.string.scene_breath
    SceneType.MATRIX_UP -> R.string.scene_matrix_up
    SceneType.MATRIX_STORM -> R.string.scene_matrix_storm
    SceneType.MATRIX_SLOW -> R.string.scene_matrix_slow
    SceneType.LAVA -> R.string.scene_lava
    SceneType.OCEAN -> R.string.scene_ocean
    SceneType.VORTEX -> R.string.scene_vortex
    SceneType.HYPNO -> R.string.scene_hypno
    SceneType.AURORA -> R.string.scene_aurora
    SceneType.METEORS -> R.string.scene_meteors
    SceneType.GALAXY -> R.string.scene_galaxy
    SceneType.WARP -> R.string.scene_warp
    SceneType.TUNNEL -> R.string.scene_tunnel
    SceneType.CLOCK_ANALOG -> R.string.scene_clock_analog
    SceneType.CLOCK_BINARY -> R.string.scene_clock_binary
    SceneType.SNAKE -> R.string.scene_snake
    SceneType.PONG -> R.string.scene_pong
    SceneType.MAZE -> R.string.scene_maze
    SceneType.MANDEL_ZOOM -> R.string.scene_mandel_zoom
    SceneType.JULIA_ANIM -> R.string.scene_julia_anim
    SceneType.LISSAJOUS -> R.string.scene_lissajous
    SceneType.SPIROGRAPH -> R.string.scene_spirograph
    SceneType.PENDULUM -> R.string.scene_pendulum
    SceneType.FIRE_TORCH -> R.string.scene_fire_torch
    SceneType.FIRE_EMBERS -> R.string.scene_fire_embers
    SceneType.STORM -> R.string.scene_storm
    SceneType.LEAVES -> R.string.scene_leaves
    SceneType.FIREFLIES -> R.string.scene_fireflies
    SceneType.DNA -> R.string.scene_dna
    SceneType.BRAINFUCK -> R.string.scene_brainfuck
    SceneType.AQUARIUM -> R.string.scene_aquarium
    SceneType.ECG -> R.string.scene_ecg
    SceneType.EQUALIZER -> R.string.scene_equalizer
    SceneType.RIPPLES -> R.string.scene_ripples
    SceneType.CUBE -> R.string.scene_cube
    SceneType.GLOBE -> R.string.scene_globe
    SceneType.RADAR -> R.string.scene_radar
    SceneType.FIREWORKS -> R.string.scene_fireworks
    SceneType.CLOUDS -> R.string.scene_clouds
    SceneType.MOUNTAINS -> R.string.scene_mountains
    SceneType.LAKE -> R.string.scene_lake
    SceneType.PINES -> R.string.scene_pines
    SceneType.SNOWFLAKE -> R.string.scene_snowflake
    SceneType.TOPOMAP -> R.string.scene_topomap
    SceneType.CASTLE -> R.string.scene_castle
    SceneType.CITY -> R.string.scene_city
    SceneType.PYRAMIDS -> R.string.scene_pyramids
    SceneType.CAT -> R.string.scene_cat
    SceneType.OWL -> R.string.scene_owl
    SceneType.PENGUIN -> R.string.scene_penguin
    SceneType.BUTTERFLY -> R.string.scene_butterfly
    SceneType.WEB -> R.string.scene_web
    SceneType.EARTH -> R.string.scene_earth
    SceneType.SATURN -> R.string.scene_saturn
    SceneType.MOON -> R.string.scene_moon
    SceneType.MANDELBROT -> R.string.scene_mandelbrot
    SceneType.JULIA -> R.string.scene_julia
    SceneType.SIERPINSKI -> R.string.scene_sierpinski
    SceneType.CARPET -> R.string.scene_carpet
    SceneType.HILBERT -> R.string.scene_hilbert
    SceneType.TREE -> R.string.scene_tree
    SceneType.MOSAIC -> R.string.scene_mosaic
    SceneType.TRUCHET -> R.string.scene_truchet
    SceneType.SUNFLOWER -> R.string.scene_sunflower
    SceneType.MANDALA -> R.string.scene_mandala
    SceneType.ROSE -> R.string.scene_rose
    SceneType.SYNTHWAVE -> R.string.scene_synthwave
    SceneType.DROID -> R.string.scene_droid
}

fun categoryLabelRes(c: SceneCategory): Int = when (c) {
    SceneCategory.NATURE -> R.string.cat_nature
    SceneCategory.SPACE -> R.string.cat_space
    SceneCategory.FRACTAL -> R.string.cat_fractal
    SceneCategory.TECH -> R.string.cat_tech
    SceneCategory.GAMES -> R.string.cat_games
    SceneCategory.ART -> R.string.cat_art
    SceneCategory.ANIMALS -> R.string.cat_animals
    SceneCategory.ARCH -> R.string.cat_arch
}

/** Строка названия встроенного пресета или null, если для id пресета нет. */
fun presetLabelRes(id: String): Int? = when (id) {
    "matrix" -> R.string.preset_matrix
    "fire" -> R.string.preset_fire
    "plasma" -> R.string.preset_plasma
    "waves" -> R.string.preset_waves
    "stars" -> R.string.preset_stars
    "clock" -> R.string.preset_clock
    "donut" -> R.string.preset_donut
    "life" -> R.string.preset_life
    "snow" -> R.string.preset_snow
    "rain" -> R.string.preset_rain
    "breath" -> R.string.preset_breath
    else -> null
}
