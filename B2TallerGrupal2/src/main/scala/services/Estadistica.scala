package services

import models.Temperatura

object Estadistica{
  def promedio(temperaturas:List[Temperatura]): Double = {
    temperaturas.map(_.temp_tarde).sum / temperaturas.length
  }

  def maxima_temperatura(temperaturas:List[Temperatura]): Double = {
    temperaturas.map(_.temp_tarde).max
  }

  def desviacion_estandar(temperaturas:List[Temperatura]): Double = {
    val x = temperaturas.map(_.temp_tarde)
    val N = temperaturas.length
    val promedio = x.sum / N
    val sumaCuadrados = x.map(temperatura => math.pow(temperatura - promedio, 2)).sum

    math.sqrt(sumaCuadrados/N)
  }
}
