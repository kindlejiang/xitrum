package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object Max {
  def apply(value: Double) = new Max(value)
}

class Max(value: Double) extends Validator {
  def render(action: Action, elem: Elem, paramName: String): Elem = {
    import action._
    jsAddToView(js$name(paramName) + ".rules('add', {max: " + value + "})")
    elem
  }

  def validate(action: Action, paramName: String): Boolean = {
    try {
      val value2 = action.param(paramName).trim.toDouble
      value2 <= value
    } catch {
      case _ => false
    }
  }
}
