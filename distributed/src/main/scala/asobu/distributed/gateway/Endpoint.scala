package asobu.distributed.gateway

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{PoisonPill, ActorRef, ActorRefFactory}
import akka.util.Timeout
import asobu.distributed.service.Action.{DistributedResult, DistributedRequest}
import asobu.distributed.EndpointDefinition
import asobu.distributed.service.Extractors.RemoteExtractor
import asobu.dsl.ExtractResult
import play.api.mvc.Results._
import play.api.mvc.{Result, AnyContent, Request}
import play.core.routing
import play.core.routing.Route.ParamsExtractor
import play.core.routing.RouteParams
import play.routes.compiler.{DynamicPart, PathPart, StaticPart}
import cats.std.all._
import scala.concurrent.{ExecutionContext, Future, duration}, duration._

trait EndpointRoute {
  def unapply(requestHeader: Request[AnyContent]): Option[RouteParams]
}

trait EndpointHandler {
  def handle(routeParams: RouteParams, request: Request[AnyContent]): Future[Result]
}

case class Endpoint(definition: EndpointDefinition)(implicit arf: ActorRefFactory) extends EndpointRoute with EndpointHandler {

  type T = definition.T

  import definition._, definition.routeInfo._
  implicit val ak: Timeout = 10.minutes //todo: find the right place to configure this

  private val handlerRef: ActorRef = {
    definition.clusterRole.fold(handlerActor) { role ⇒
      val props = ClusterRouters.adaptive(handlerActor.path.toStringWithoutAddress, role)
      arf.actorOf(props, handlerActor.path.name + "-Router+" + ThreadLocalRandom.current().nextInt(1000)) //allows some redundancy in this router
    }
  }

  def shutdown() = handlerRef ! PoisonPill

  def unapply(request: Request[AnyContent]): Option[RouteParams] = routeExtractors.unapply(request)

  def handle(routeParams: RouteParams, request: Request[AnyContent]): Future[Result] = {
    import akka.pattern.ask
    import ExecutionContext.Implicits.global

    def handleMessageWithBackend(t: T): Future[Result] = {
      (handlerRef ? DistributedRequest(t, request.body)).collect {
        case r: DistributedResult ⇒ r.toResult
        case m                    ⇒ InternalServerError(s"Unsupported result from backend ${m.getClass}")
      }
    }
    val message = extractor.run((routeParams, request))
    message.fold[Future[Result]](
      Future.successful(_),
      (t: T) ⇒ handleMessageWithBackend(t)
    ).flatMap(identity)
    Future.successful(Ok)
  }

  lazy val extractor: RemoteExtractor[T] = definition.remoteExtractor

  private lazy val routeExtractors: ParamsExtractor = {
    val localParts = if (path.parts.nonEmpty) StaticPart(defaultPrefix) +: path.parts else Nil
    routing.Route(verb.value, routing.PathPattern(toCPart(StaticPart(prefix.value) +: localParts)))
  }

  implicit private def toCPart(parts: Seq[PathPart]): Seq[routing.PathPart] = parts map {
    case DynamicPart(n, c, e) ⇒ routing.DynamicPart(n, c, e)
    case StaticPart(v)        ⇒ routing.StaticPart(v)
  }

}

object Endpoint {
  case class Prefix(value: String) extends AnyVal

}
