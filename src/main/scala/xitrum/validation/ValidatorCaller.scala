package xitrum.validation

import xitrum.Action

object ValidatorCaller {
  def call(action: Action): Boolean = {
    for ((paramName, validators) <- action.validators) {
      for (v <- validators.validators) {
        if (!v.validate(action, paramName)) return false
      }
    }

    true
  }

}
