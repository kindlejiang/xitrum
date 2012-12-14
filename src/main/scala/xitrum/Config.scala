package xitrum

import java.io.File
import java.nio.charset.Charset

import com.hazelcast.client.{ClientConfig, ClientConfigBuilder, HazelcastClient}
import com.hazelcast.core.{Hazelcast, HazelcastInstance}

import xitrum.scope.session.SessionStore
import xitrum.util.Loader

//----------------------------------------------------------------------------

case class BasicAuthConfig(realm: String, username: String, password: String)

case class PortConfig(http: Option[Int], https: Option[Int], flashSocketPolicy: Option[Int])

case class KeyStoreConfig(path: String, password: String, certificatePassword: String)

case class ReverseProxyConfig(ips: List[String], baseUrl: String)

case class SessionConfig(store: String, cookieName: String, secureKey: String)

case class RequestConfig(maxSizeInMB: Int, charset: String, filteredParams: List[String])

case class ResponseConfig(maxSizeInKBOfCachedStaticFiles: Int, maxNumberOfCachedStaticFiles: Int, clientMustRevalidateStaticFiles: Boolean)

case class Config(
  basicAuth:     Option[BasicAuthConfig],
  port:          PortConfig,
  keystore:      KeyStoreConfig,
  reverseProxy:  Option[ReverseProxyConfig],
  scalate:       String,
  hazelcastMode: String,
  session:       SessionConfig,
  request:       RequestConfig,
  response:      ResponseConfig)

case class HazelcastJavaClientConfig(groupName: String, groupPassword: String, addresses: List[String])

//----------------------------------------------------------------------------

/** See config/xitrum.properties */
object Config extends Logger {
  private val HAZELCAST_MODE_CLUSTER_MEMBER = "clusterMember"
  private val HAZELCAST_MODE_LITE_MEMBER    = "liteMember"
  private val HAZELCAST_MODE_JAVA_CLIENT    = "javaClient"

  /**
   * Static textual files are always compressed
   * Dynamic textual responses are only compressed if they are big
   * http://code.google.com/speed/page-speed/docs/payload.html#GzipCompression
   *
   * Google recommends > 150B-1KB
   */
  val BIG_TEXTUAL_RESPONSE_SIZE_IN_KB = 1

  /**
   * In case of CPU bound, the pool size should be equal the number of cores
   * http://grizzly.java.net/nonav/docs/docbkx2.0/html/bestpractices.html
   */
  val EXECUTIORS_PER_CORE = 64

  private val DEFAULT_SECURE_KEY = "ajconghoaofuxahoi92chunghiaujivietnamlasdoclapjfltudoil98hanhphucup8"

  /**
   * Path to the root directory of the current project.
   * If you're familiar with Rails, this is the same as Rails.root.
   * See https://github.com/ngocdaothanh/xitrum/issues/47
   */
  val root = {
    val res = getClass.getClassLoader.getResource("xitrum.properties")
    if (res != null)
      res.getFile.replace(File.separator + "config" + File.separator + "xitrum.properties", "")
    else
      System.getProperty("user.dir")  // Fallback to current working directory
  }

  //----------------------------------------------------------------------------

  /** See bin/runner.sh */
  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  /** Loaded from xitrum.json */
  val config = {
    var ret: Config = null
    try {
      ret = Loader.jsonFromClasspath[Config]("xitrum.json")
    } catch {
      case e: Exception =>
        exitOnError("Could not load config/xitrum.json. For an example, see https://github.com/ngocdaothanh/xitrum-new/blob/master/config/xitrum.json", e)
    }
    ret
  }

  def warnOnDefaultSecureKey() {
    if (config.session.secureKey == DEFAULT_SECURE_KEY)
      logger.warn("For security, change secureKey in config/xitrum.json to your own!")
  }

  //----------------------------------------------------------------------------

  val baseUrl = config.reverseProxy.map(_.baseUrl).getOrElse("")

  /**
   * @param path with leading "/"
   *
   * Avoids returning path with double "//" prefix. Something like
   * //xitrum/postback/zOIc0v...
   * will cause the browser to send request to http://xitrum/postback/zOIc0v...
   */
  def withBaseUrl(path: String) = {
    if (Config.baseUrl.isEmpty) {
      path
    } else {
      if (path.isEmpty) Config.baseUrl else Config.baseUrl + "/" + path
    }
  }

  val requestCharset = Charset.forName(config.request.charset)

  val sessionStore  = {
    val className = config.session.store
    Class.forName(className).newInstance().asInstanceOf[SessionStore]
  }

  //----------------------------------------------------------------------------

  /**
   * Use lazy to avoid starting Hazelcast if it is not used
   * (starting Hazelcast takes several seconds, sometimes we want to work in
   * sbt console mode and don't like this overhead)
   */
  lazy val hazelcastInstance: HazelcastInstance = {
    // http://www.hazelcast.com/docs/2.4/manual/multi_html/ch12s07.html
    System.setProperty("hazelcast.logging.type", "slf4j")

    // http://www.hazelcast.com/docs/2.4/manual/multi_html/ch15.html
    // http://www.hazelcast.com/docs/2.4/manual/multi_html/ch07s03.html
    if (config.hazelcastMode == HAZELCAST_MODE_LITE_MEMBER)
      System.setProperty("hazelcast.lite.member", "true")

    if (config.hazelcastMode == HAZELCAST_MODE_LITE_MEMBER || config.hazelcastMode == HAZELCAST_MODE_CLUSTER_MEMBER) {
      val path = Config.root + File.separator + "config" + File.separator + "hazelcast_cluster_or_lite_member.xml"
      System.setProperty("hazelcast.config", path)

      // null: load from "hazelcast.config" system property above
      // http://www.hazelcast.com/docs/2.4/manual/multi_html/ch12.html
      Hazelcast.newHazelcastInstance(null)
    } else {
      // https://github.com/hazelcast/hazelcast/issues/93
      val clientConfig = new ClientConfigBuilder("hazelcast_java_client.properties").build()
      HazelcastClient.newHazelcastClient(clientConfig)
    }
  }

  /**
   * Shutdowns Hazelcast and call System.exit(-1).
   * Once Hazelcast is started, calling System.exit(-1) does not stop
   * the current process!
   */
  def exitOnError(msg: String, e: Throwable) {
    logger.error(msg, e)
    Hazelcast.shutdownAll()
    System.exit(-1)
  }
}
