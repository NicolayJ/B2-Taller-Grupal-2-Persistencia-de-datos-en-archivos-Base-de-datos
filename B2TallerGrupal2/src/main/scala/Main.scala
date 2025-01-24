import cats.effect.{IO, IOApp}
import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._
import java.io.File
import models.Estudiante
import dao.EstudianteDAO

import doobie._
import doobie.implicits._
import cats.effect.{IO, IOApp}
import cats.implicits._
import models._
import config.Database


// Extiende de IOApp.Simple para manejar efectos IO y recursos de forma segura
object Main extends IOApp.Simple {
  val path2DataFile2 = "src/main/resources/data/estudiantes.csv"

  val dataSource = new File(path2DataFile2)
    .readCsv[List, Estudiante](rfc.withHeader.withCellSeparator(','))

  val estudiantes = dataSource.collect {
    case Right(estudiante) => estudiante
  }

  // Secuencia de operaciones IO usando for-comprehension
  def run: IO[Unit] = for {
    result <- EstudianteDAO.insertAll(estudiantes)  // Inserta datos y extrae resultado con <-
    _ <- IO.println(s"Registros insertados: ${result.size}")  // Imprime cantidad
    allEstudiantes <- Database.transactor.use(xa => EstudianteDAO.obtenerTodos.transact(xa))
    _ <- IO.println("Lista de estudiantes:")
    _ <- allEstudiantes.traverse(e => IO.println(e))
  } yield ()  // Completa la operación


}