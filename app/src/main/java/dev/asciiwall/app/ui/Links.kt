package dev.asciiwall.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.asciiwall.app.R

/**
 * ВСЕ внешние ссылки и списки страницы «О проекте» живут здесь.
 * Пустая строка = «ссылки пока нет»: кнопка неактивна, открытие даёт сообщение.
 */
object AppLinks {
    const val AUTHOR = "https://github.com/vipa-ch-cloud"
    const val AUTHOR_NAME = "vipa-ch-cloud"
    const val REPO = "https://github.com/vipa-ch-cloud/asciiwall"

    /** Поддержка автора. ПЛЕЙСХОЛДЕРЫ: реальные ссылки/реквизиты подставляет автор (в проектах их нет). */
    const val DONATE_URL = "" // TODO: ссылка на донат (DonationAlerts / CloudTips / ...)
    const val BOOSTY_URL = "" // TODO: ссылка Boosty

    /** Источник стиля интерфейса (GPLv3). */
    const val STYLE_SOURCE = "https://github.com/amurcanov/tg-ws-proxy-android"
}

/** Участник проекта. [url] пустой, если ссылки нет. */
data class Person(val name: String, val roleRes: Int?, val url: String = "")

object People {
    val developers = listOf(
        Person(AppLinks.AUTHOR_NAME, R.string.about_role_author, AppLinks.AUTHOR),
        Person("Claude Code (Anthropic)", R.string.about_role_assistant),
    )

    /** Пока пусто: допишите сюда Person("имя", null, "ссылка") для тех, кто поддержал проект. */
    val supporters: List<Person> = emptyList()
}

/** Проект автора для списка «Другие проекты». */
data class Project(val name: String, val descRes: Int, val url: String)

object OtherProjects {
    private const val BASE = "https://github.com/vipa-ch-cloud/"
    val list = listOf(
        Project("TgAuto", R.string.about_proj_tgauto, BASE + "TgAuto"),
        Project("BigIcons", R.string.about_proj_bigicons, BASE + "bigicons"),
    )
}

/** Открывает ссылку; при пустой ссылке или отсутствии браузера показывает сообщение вместо падения. */
fun openUrl(ctx: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(ctx, R.string.about_link_missing, Toast.LENGTH_SHORT).show()
        return
    }
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.about_no_browser, Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(ctx, R.string.about_no_browser, Toast.LENGTH_SHORT).show()
    }
}
