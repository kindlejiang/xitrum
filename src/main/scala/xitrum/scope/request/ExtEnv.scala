package xitrum.scope.request

import scala.collection.mutable.{Map => MMap}

import xitrum.Config
import xitrum.Action
import xitrum.scope.session.CSRF
import xitrum.validation.Validators

trait ExtEnv extends RequestEnv with ParamAccess with CSRF {
  this: Action =>

  // The below are not always accessed by framework/application, thus set to lazy

  lazy val at = new At

  lazy val validators = MMap[String, Validators]()

  // Avoid encoding, decoding when cookies/session is not touched by the application
  private var sessionTouched = false
  private var cookiesTouched = false

  lazy val session = {
    sessionTouched = true
    Config.sessionStore.restore(this)
  }

  lazy val cookies = {
    cookiesTouched = true
    new Cookies(request)
  }

  def sessiono[T](key: String): Option[T] = session.get(key).map(_.asInstanceOf[T])

  def prepareWhenRespond {
    if (sessionTouched) Config.sessionStore.store(session, this)
    if (cookiesTouched) cookies.setCookiesWhenRespond(this)
  }

  def resetSession {
    session.clear
    cookies.clear  // This will clear the session cookie
  }
}
