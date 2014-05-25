package org.ferrit.core.util

/**
 * A simple bean counter.
 */
case class Counters(val counters: Map[String, Int] = Map.empty) {
  
  def get(key: String):Int = counters.getOrElse(key, 0)
  
  def increment(key: String):Counters = Counters(counters.updated(key, get(key) + 1))
  
  override def toString = counters.toString.replaceAll("""Map\(""", getClass.getSimpleName + "(")

}

object Counters {
  
  def apply() = new Counters(Map.empty)

}