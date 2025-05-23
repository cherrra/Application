📱 Мобильное приложение автосервиса

Клиентская часть (Android, Java, MVVM)

🧩 Описание проекта

Мобильное приложение разработано в рамках дипломного проекта и предназначено для взаимодействия клиентов с автосервисом. Оно позволяет пользователям удобно оформлять заявки на ремонт и обслуживание автомобилей, отслеживать статус работ, просматривать услуги и управлять своими данными.

Проект реализован на чистом Java с использованием архитектуры MVVM (Model-View-ViewModel). Это обеспечивает масштабируемость, читаемость и удобство поддержки кода.

🎯 Цель проекта

Целью разработки является решение актуальных проблем, связанных с техническим обслуживанием автомобилей, путём создания удобного и функционального мобильного приложения, предоставляющего следующие возможности:

• Быстрое оформление заявок на ремонт

• Отслеживание статуса выполнения работ

• Просмотр перечня доступных услуг

• Управление профилем и автомобилями пользователя

• Получение уведомлений об изменениях

🏗️ Технологии и библиотеки

Проект использует ряд популярных Android-библиотек:

• AndroidX: AppCompat, ConstraintLayout, RecyclerView, CardView

• Material Components: UI-компоненты от Google

• MVVM: вручную реализованная архитектура без использования ViewModel из Android Jetpack

• Retrofit 2 + Gson — основной инструмент для работы с REST API

• OkHttp + Logging Interceptor — HTTP-клиент с логированием запросов

• Picasso, Glide — для загрузки изображений

• EncryptedSharedPrefs (Security Crypto) — безопасное хранение токена и пользовательских данных

• ThreeTenABP — работа с датами и временем 

📁 Структура проекта

data/model/ — модели данных 

data/repository/ — обработка данных

ui/view/ — экраны приложения 

ui/viewmodels/ — реализация MVVM: взаимодействие с API

network/ — взаимодействие с сервером через Retrofit и OkHttp

utils/ — утилиты, вспомогательные классы 

res/ — ресурсы приложения (layout, drawable, strings и т.п.)

🔒 Безопасность

• Токен доступа хранится с использованием EncryptedSharedPreferences

• API-запросы защищены через авторизацию с JWT

• Используется HTTPS-соединение через OkHttp + Interceptor
