package perm.tryfuture.opendata

import java.awt.Color
import java.net.{URL, URLDecoder}
import java.nio.charset.Charset
import javafx.application.{Application, Platform}
import javafx.event.{ActionEvent, EventHandler}
import javafx.geometry.Insets
import javafx.scene.{Group, Scene}
import javafx.scene.control._
import javafx.scene.layout.VBox
import javafx.scene.text.TextFlow
import javafx.scene.web.WebView
import javafx.stage.{WindowEvent, Stage}

import org.apache.commons.csv.{CSVFormat, CSVParser}

import scala.collection.JavaConversions._

import scala.concurrent.Future
import scala.util.{Random, Failure, Success, Try}

object DirectionsBrowser {
  def main(args: Array[String]) {
    Application.launch(classOf[DirectionsBrowser], args: _*)
  }
}

class DirectionsBrowser extends Application {
  val browser = new WebView
  val webEngine = browser.getEngine

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("Просмотр открытых данных Пермского края")
    primaryStage.setOnCloseRequest(new EventHandler[WindowEvent] {
      override def handle(event: WindowEvent): Unit = System.exit(0)
    })
    val vbox = new VBox(8) {
      setPadding(new Insets(10, 10, 10, 10))
    }
    val addButton = new Button("Добавить")
    val directionTextField = new TextField {
      setText("349436")
      setPromptText("ID набора данных")
      setPrefColumnCount(10)
    }
    val flow = new TextFlow(directionTextField, addButton)
    vbox.getChildren.add(flow)
    val scrollPane = new ScrollPane(vbox)
    primaryStage.setScene(new Scene(scrollPane, 450, 200))
    addButton.setOnAction(new EventHandler[ActionEvent] {
      override def handle(event: ActionEvent): Unit = {
        // Проверка целочисленности введённого идентификатора
        Try(directionTextField.getText.toInt) match {
          case Success(id: Int) if id > 0 =>
            val newDirectionLabel = new Label(id.toString)
            val removeButton: Button = new Button("Удалить") {
              setDisable(true)
            }
            val flow = new TextFlow(newDirectionLabel, removeButton)
            vbox.getChildren.add(flow)
            removeButton.setOnAction(new EventHandler[ActionEvent] {
              override def handle(event: ActionEvent): Unit = vbox.getChildren.removeAll(flow)
            })
            loadNewDirection(id, newDirectionLabel)
            directionTextField.setText("350257")
          case _ =>
        }
      }
    })
    showMap()
    primaryStage.show()
  }

  def loadNewDirection(id: Int, newDirectionLabel: Labeled): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      // Формирование строки для загрузки
      val url = new URL(s"http://opendata.permkrai.ru/opendata/LoadData/SerializedHandler.ashx?direction=$id&format=CSV&action=exportfile&startpos=0&endpos=1000")
      val httpHeader = url.openConnection().getHeaderField("Content-Disposition").mkString
      // Извлечение имени файла из HTTP-заголовка
      val filename = URLDecoder.decode(httpHeader.split('=').last, "UTF-8").replace("UTF-8''", "").replace(".csv", "")
      Platform.runLater(new Runnable {
        override def run(): Unit = {
          // Подмена целочисленного идентификатора на удобочитаемый
          newDirectionLabel.setText(s"(${newDirectionLabel.getText}) $filename")
        }
      })
      // Загрузка файла по ссылке и последующий его разбор
      val reader = CSVParser.parse(url, Charset.forName("windows-1251"), CSVFormat.EXCEL.withHeader().withDelimiter(';'))
      // Выделение столбцов, значения в которых будут отображены на пользовательском интерфейсе
      val csvHeadersInfo = (reader.getHeaderMap -- Seq("Широта", "Долгота")).keys.toList.sorted
      reader.getRecords.toList.map { csvRecord =>
        for {
          latitude <- Try(csvRecord.get("Широта").toDouble)
          longitude <- Try(csvRecord.get("Долгота").toDouble)
        } yield {
          val displayText = csvHeadersInfo.map { header =>
            header + ": " + csvRecord.get(header)
          }.mkString("<br />")
          // Формирование очередного маркера для карты в случае, если у записи означены столбцы "Широта" и "Долгота"
          MarkerData(latitude, longitude, displayText)
        }
      }.collect { case Success(x) => x }
    } onComplete {
      case Success(markers) =>
        // Для каждого набора данных свой цвет маркера
        val color = Color.getHSBColor(Random.nextFloat(), (Random.nextInt(2000) + 7000) / 10000.0f, 0.9f)
        val colorStr = "#%02x%02x%02x".format(color.getRed, color.getGreen, color.getBlue)
        markers.foreach { case MarkerData(latitude, longitude, text) =>
          Platform.runLater(new Runnable {
            override def run(): Unit = {
              // Добавить маркер на карту
              webEngine.executeScript(
                s"""
                   |L.circle([$latitude, $longitude], 150, {
                   |			color: '$colorStr',
                   |			fillOpacity: 0.5
                   |		}).addTo(map).bindPopup("$text");
                   |
               """.stripMargin
              )
            }
          })
        }
      case Failure(e) =>
        println(e)
    }
  }

  def showMap(): Unit = {
    browser.setPrefWidth(620)
    browser.setPrefHeight(420)
    // См. http://leafletjs.com/examples/quick-start.html
    webEngine.loadContent(
      """
        |<!DOCTYPE html>
        |<html>
        |<head>
        |	<title>Leaflet Quick Start Guide Example</title>
        |	<meta charset="utf-8" />
        |
        |	<meta name="viewport" content="width=device-width, initial-scale=1.0">
        |
        |	<link rel="stylesheet" href="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.css" />
        |</head>
        |<body>
        |	<div id="map" style="width: 600px; height: 400px"></div>
        |
        |	<script src="http://cdn.leafletjs.com/leaflet/v0.7.7/leaflet.js"></script>
        |	<script>
        |
        |		var map = L.map('map').setView([58.0104600, 56.2501700], 12);
        |
        |		L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token=pk.eyJ1IjoibWFwYm94IiwiYSI6IjZjNmRjNzk3ZmE2MTcwOTEwMGY0MzU3YjUzOWFmNWZhIn0.Y8bhBaUMqFiPrDRW9hieoQ', {
        |			maxZoom: 18,
        |			attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
        |				'<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
        |				'Imagery © <a href="http://mapbox.com">Mapbox</a>',
        |			id: 'mapbox.streets'
        |		}).addTo(map);
        |	</script>
        |</body>
        |</html>
        |
      """.stripMargin)
    val stage = new Stage()
    stage.setTitle("Карта")
    stage.setScene(new Scene(new Group(browser), 610, 410))
    stage.setOnCloseRequest(new EventHandler[WindowEvent] {
      override def handle(event: WindowEvent): Unit = System.exit(0)
    })
    stage.show()
  }
}

case class MarkerData(latitude: Double, longitude: Double, text: String)