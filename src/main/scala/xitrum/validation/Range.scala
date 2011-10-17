package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object Range {
  def apply(min: Double, max: Double) = new Range(min, max)
}

class Range(min: Double, max: Double) extends Validator {
  def render(action: Action, elem: Elem, paramName: String): Elem = {
    import action._
    jsAddToView(js$name(paramName) + ".rules('add', {range: [" + min + ", " + max + "]})")
    elem
  }

  def validate(action: Action, paramName: String): Boolean = {
    try {
      val value = action.param(paramName).trim.toInt
      min <= value && value <= max
    } catch {
      case _ => false
    }
  }
}
