package asobu.distributed

import akka.actor.ActorRef
import asobu.distributed.gateway.Endpoint.Prefix
import asobu.distributed.gateway.RoutesCompilerExtra._
import asobu.distributed.service.Extractors.RemoteExtractor
import asobu.distributed.service.RemoteExtractorDef
import asobu.dsl.{Extractor, ExtractResult}
import play.api.mvc.{AnyContent, Request}
import play.core.routing.RouteParams
import play.routes.compiler.Route
import shapeless.{HNil, HList}

/**
 * Endpoint definition by the remote handler
 */
trait EndpointDefinition {
  /**
   * type of the Message
   */
  type T
  val routeInfo: Route
  val prefix: Prefix
  def handlerActor: ActorRef
  def clusterRole: Option[String] //also indicate if this handlerActor is in a cluster

  val defaultPrefix: String = {
    if (prefix.value.endsWith("/")) "" else "/"
  }

  val documentation: (String, String, String) = {
    val localPath = if (routeInfo.path.parts.isEmpty) ""
    else defaultPrefix + encodeStringConstant(routeInfo.path.toString)
    val pathInfo = prefix + localPath
    (routeInfo.toString, pathInfo, routeInfo.call.toString)
  }

  val id: String = documentation.toString()

  def remoteExtractor: RemoteExtractor[T]

}

object EndpointDefinition {
  type Aux[T0] = EndpointDefinition { type T = T0 }
}

case class EndPointDefImpl[LExtracted <: HList, LParam <: HList, LExtra <: HList](
    prefix: Prefix,
    routeInfo: Route,
    remoteExtractorDef: RemoteExtractorDef[LExtracted, LParam, LExtra],
    handlerActor: ActorRef,
    clusterRole: Option[String] = None
) extends EndpointDefinition {

  type T = LExtracted

  def remoteExtractor: RemoteExtractor[T] = remoteExtractorDef.extractor
}

/**
 * Endpoint that takes no input at all, just match a route path
 */
case class NullaryEndpointDefinition(
    prefix: Prefix,
    routeInfo: Route,
    handlerActor: ActorRef,
    clusterRole: Option[String] = None
) extends EndpointDefinition {

  type T = HNil
  def extract(routeParams: RouteParams, request: Request[AnyContent]): ExtractResult[T] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    ExtractResult.pure(HNil)
  }

  def remoteExtractor = Extractor.empty[(RouteParams, Request[AnyContent])]
}
