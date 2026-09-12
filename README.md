# Zhuki_Game
IP-313 Gromova Anastasia & Shirokov Kirill

## Требования

**Android Studio** — последняя стабильная версия

**Android SDK** — API 34 или 35

### 1. Клонируйте репозиторий

```bash
git clone https://github.com/gromova-aan/Zhuki_Game.git
```
### 2. Откройте проект в Android Studio
`File` -> `Open` -> выберите корневую папку `Zhuki_Game` (не вложенную `app`).

### 3. Создайте `local.properties`
В корне проекта создайте файл `local.properties` и укажите путь к SDK:

* Windows:

```bash
sdk.dir=C\:\\Users\\<Ваше_Имя>\\AppData\\Local\\Android\\Sdk
```

* macOS:

```bash
sdk.dir=/Users/<Ваше_Имя>/Library/Android/sdk
```

Путь можно посмотреть в Android Studio: `File` -> `Project Structure` -> `SDK Location`.

### 4. Синхронизируйте Gradle
   `File` -> `Sync Project with Gradle Files`

### 5. Установите недостающие SDK (при необходимости)
   `Tools` -> `SDK Manager` -> отметьте нужные версии -> `Apply`

### 6. Запустите приложение
   * Создайте эмулятор: `Tools` -> `Device Manager` -> `Create Virtual Device`
   * Нажмите `Run` или `Shift + F10`

### Структура проекта

`app/src/main/java/...`	Исходный код на Kotlin

`app/src/main/res/layout/`	XML-разметка экранов

`app/src/main/res/values/strings.xml`	Строковые ресурсы

`app/build.gradle.kts`	Конфигурация модуля приложения

`settings.gradle.kts`	Настройки проекта

### Технологии
**Kotlin** — основной язык

**Android SDK** — библиотеки и API платформы

**Gradle** — система сборки

**Android Emulator** — запуск и тестирование