# ASCII Wall

Android-приложение с живыми ASCII-обоями. Рисует анимированные и статические сцены из символов прямо на рабочем столе и экране блокировки.

*Скриншотов пока нет, поэтому ниже описание.*

## Возможности

- 78 сцен (анимированные и статические), разбитые по категориям
- Цветовые темы
- Выбор сцены из каталога и применение в качестве обоев
- Страница «О проекте»

## Сборка

Нужны JDK 17+ и Android SDK.

```
./gradlew assembleDebug      # отладочная сборка
./gradlew assembleRelease    # релизная сборка
```

## Подпись релиза

Параметры подписи читаются из файла `keystore.properties` в корне проекта (он в `.gitignore` и в репозиторий не попадает). Образец: `keystore.properties.example`.

```
storeFile=asciiwall.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Если файла нет, используются переменные окружения `ASCIIWALL_STORE_FILE`, `ASCIIWALL_STORE_PASSWORD`, `ASCIIWALL_KEY_ALIAS`, `ASCIIWALL_KEY_PASSWORD`. Если нет и их, релиз собирается без подписи.

## Автор и лицензия

Автор: [vipa-ch-cloud](https://github.com/vipa-ch-cloud).

Интерфейс портирован из [tg-ws-proxy-android](https://github.com/amurcanov/tg-ws-proxy-android) (GPLv3), поэтому проект распространяется под лицензией GNU GPL v3. См. файл [LICENSE](LICENSE).

---

## English

ASCII Wall is an Android live wallpaper app that renders animated and static ASCII scenes. It has 78 scenes grouped by category and several color themes, plus an About page. No screenshots yet.

Build: `./gradlew assembleRelease`. Release signing is read from `keystore.properties` (git-ignored, see `keystore.properties.example`), then from `ASCIIWALL_*` environment variables; without either the release APK is built unsigned.

Author: [vipa-ch-cloud](https://github.com/vipa-ch-cloud). The UI is ported from tg-ws-proxy-android (GPLv3); this project is licensed under GPLv3.
