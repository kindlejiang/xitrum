package xitrum.validation

import scala.xml.Elem
import xitrum.Action

class Validators(var validators: List[Validator]) {
  def ::(validator: Validator): Validators = {
    validators = validator :: validators
    this
  }

  /**
   * {<input type="text" name="username" /> :: Required :: MaxLength(10)}
   *
   * Validators may add new or change other attributes. For example, Required
   * adds class="required".
   */
  def ::(elem: Elem)(implicit action: Action): Elem = {
    val paramName = (elem \ "@name").text
    validators.foldLeft(elem) { (acc, validator) =>
      validator.render(action, acc, paramName)
    }
  }
}
