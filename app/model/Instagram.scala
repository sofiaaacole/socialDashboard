package model

import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsDefined, JsString}
import play.api.libs.ws._

import scala.concurrent.Future

object Instagram {

  val logger: Logger = Logger("instagram")

  def instagramAuth(clientID: String, clientSecret: String, grantType: String, redirectURI: String, code: String): Future[String] = {

    val url = "https://api.instagram.com/oauth/access_token"

    (for {
      response <- WS.url(url).post(Map[String, Seq[String]](
        "client_id" -> Seq(clientID),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq(grantType),
        "redirect_uri" -> Seq(redirectURI),
        "code" -> Seq(code)
      ))
    } yield {
        response.json \ "access_token" match {
          case JsDefined(JsString(token)) => Some(token)
          case _ => None
        }
      }).map {
      case Some(at) => createSubscription(clientID, clientSecret, at)
      case None => "ERROR"
    }

  }

  def createSubscription(clientID: String, clientSecret: String, accessToken: String): String = {
    val url = "https://api.instagram.com/v1/subscriptions/"
    val callback = "http://allsocialdashboard.herokuapp.com/callback"

    val response: Future[WSResponse] =
      WS.url(url).post(Map[String, Seq[String]](
        "client_id"     -> Seq(clientID),
        "client_secret" -> Seq(clientSecret),
        "object"        -> Seq("tag"),
        "aspect"        -> Seq("media"),
        "object_id"     -> Seq("nofilter"),
        "verify_token"  -> Seq(accessToken),
        "callback_url"  -> Seq(callback)
      ))

    logger.info("Call to create subscription " + response)

    response.map(_.json \ "data" \ "object") match {
      case x => x.toString
    }
  }
}