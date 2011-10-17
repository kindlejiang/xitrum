package xitrum.validation

import scala.xml.Elem
import xitrum.Action

object RangeLength {
  def apply(min: Int, max: Int) = new RangeLength(min, max)
}

class RangeLength(min: Int, max: Int) extends Validator {
  def render(action: Action, elem: Elem, paramName: String): Elem = {
    import action._
    jsAddToView(js$name(paramName) + ".rules('add', {rangelength: [" + min + ", " + max + "]})")
    elem
  }

  def validate(action: Action, paramName: String): Boolean = {
    try {
      val value = action.param(paramName).trim.length
      min <= value && value <= max
    } catch {
      case _ => false
    }
  }
}
