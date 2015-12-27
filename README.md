# Средство просмотра данных с [портала открытых данных Пермского края](http://opendata.permkrai.ru/opendata/)

## Задача
Продемонстрировать возможность загрузки списка показателей, значений показателей и реестровых данных, разбора ответа и отображения его на пользовательском интерфейсе.

![Отображение реестровых данных](/DirectionsBrowser.png?raw=true)
![Отображение показателей](CubesBrowser.png?raw=true)

## Идея реализации
1. Пользователь на интерфейсе вводит идентификатор реестровых данных. Данные загружаются в формате CSV. В случае, если полученные записи имеют привязку к координатам, информация о записях отображается на карте.
2. Список показателей загружается в формате XML. Пользователь на интерфейсе выбирает показатель. Загруженные в формате CSV данные отображаются в виде таблицы.

## Особенности реализации
Пользовательский интерфейс - [JavaFX](https://ru.wikipedia.org/wiki/JavaFX) и [Swing](http://docs.oracle.com/javase/tutorial/uiswing/). Карта отображается в [JavaFX WebView](https://docs.oracle.com/javafx/2/webview/jfxpub-webview.htm) при помощи [Leaflet](http://leafletjs.com/).

## Запуск
Запуск через [sbt run <class name>](http://www.scala-sbt.org/0.13/tutorial/Running.html) или в [IntelliJ IDEA](https://www.jetbrains.com/idea/help/creating-and-running-your-scala-application.html):
* `perm.tryfuture.opendata.DirectionsBrowser`
* `perm.tryfuture.opendata.CubesBrowser`