package xitrum.validation

import scala.xml.Elem
import xitrum.Action

trait Validator extends Serializable {
  def render(action: Action, elem: Elem, paramName: String): Elem
  def validate(action: Action, paramName: String): Boolean

  //----------------------------------------------------------------------------

  def ::(other: Validator) = new Validators(List(other, this))

  def ::(elem: Elem)(implicit action: Action): Elem = {
    val validators = new Validators(List(this))
    elem :: validators
  }
}
