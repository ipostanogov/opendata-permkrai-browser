package perm.tryfuture.opendata

import java.net.URL
import java.nio.charset.Charset
import java.util
import javafx.application.Application
import javafx.embed.swing.SwingNode
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.Scene
import javafx.scene.control.{Hyperlink, ScrollPane}
import javafx.scene.layout.VBox
import javafx.stage.{WindowEvent, Stage}
import javax.swing.{JScrollPane, JTable, SwingUtilities}

import org.apache.commons.csv.{CSVFormat, CSVParser}

import scala.collection.JavaConversions._
import scala.xml.XML

object CubesBrowser {
  def main(args: Array[String]) {
    Application.launch(classOf[CubesBrowser], args: _*)
  }
}

class CubesBrowser extends Application {
  case class FactorsData(factors: Map[String, String], factorCubePairs: Map[String, String])

  val factorsData = {
    val str = scala.io.Source.fromURL("http://opendata.permkrai.ru/LoadDataManager/api/DataPub.asmx/GetXMLFactors").mkString.replace("&lt;", "<").replace("&gt;", ">").replace( """<?xml version="1.0" encoding="utf-8"?>""", "")
    val xmlFactorsAll = XML.loadString(str)
    // Структуру XML см. в http://opendata.permkrai.ru/opendata/ForDevelopers/
    val xmlFactorCubes = xmlFactorsAll.flatMap(_ \ "FactorCubes")
    val factors = xmlFactorCubes.flatMap(_ \ "Factors" \ "Factor").flatMap { factor =>
      for {
        id <- factor.attribute("factor_id").flatMap(_.headOption)
        name <- factor.attribute("name").flatMap(_.headOption)
      } yield {
        id.toString() -> name.toString()
      }
    }.toMap
    // Для каждого показателя сохраняется только один куб (досточно для демонстрации)
    val factorCubePairs = xmlFactorCubes.flatMap(_ \ "FactorCubePairs" \ "FactorCubePair").flatMap { factorCubePair =>
      for {
        id <- factorCubePair.attribute("factor_id").flatMap(_.headOption)
        name <- factorCubePair.attribute("dim_data_key").flatMap(_.headOption)
      } yield {
        id.toString() -> name.toString()
      }
    }.toMap
    FactorsData(factors, factorCubePairs)
  }

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("Список показателей портала открытых данных Пермского края")
    val vBox = new VBox()
    factorsData.factors.foreach { case (id, name) =>
      val link = new Hyperlink(name) {
        setOnAction(new EventHandler[ActionEvent] {
          override def handle(event: ActionEvent): Unit = {
            val stage = new Stage()
            stage.setTitle(name)
            val url = new URL(s"http://opendata.permkrai.ru/LoadDataManager/api/getcsv.ashx?factor=$id&cube=${factorsData.factorCubePairs(id)}")
            val reader = CSVParser.parse(url, Charset.forName("windows-1251"), CSVFormat.EXCEL.withHeader().withDelimiter(';'))
            val header = new util.Vector(reader.getHeaderMap.keys.toVector)
            val values = new util.Vector(reader.getRecords.map(r => new util.Vector(header.map(r.get))))
            val swingNode = new SwingNode
            SwingUtilities.invokeLater(new Runnable {
              override def run(): Unit = {
                swingNode.setContent(new JScrollPane(new JTable(values, header)))
              }
            })
            stage.setScene(new Scene(new VBox(swingNode)))
            stage.show()
          }
        })
      }
      vBox.getChildren.add(link)
    }
    primaryStage.setScene(new Scene(new ScrollPane(vBox)))
    primaryStage.setMaximized(true)
    primaryStage.setOnCloseRequest(new EventHandler[WindowEvent] {
      override def handle(event: WindowEvent): Unit = System.exit(0)
    })
    primaryStage.show()
  }
}