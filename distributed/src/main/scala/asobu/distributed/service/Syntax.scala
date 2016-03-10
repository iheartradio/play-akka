package asobu.distributed.service

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import Action.DistributedResult
import asobu.distributed.{RequestExtractorDefinition, EndpointDefinition}

import scala.concurrent.{ExecutionContext, Future}

trait Syntax {
  self: Controller ⇒
  def actionName(name: String) = getClass.getName.stripSuffix("$").replace('$', '.') + "." + name

  def handle[T](name0: String, extrs: Extractors[T])(bk: T ⇒ Future[DistributedResult])(
    implicit
    rc: EndpointsRegistryClient, ec: ExecutionContext, sys: ActorSystem
  ): Future[EndpointDefinition] = {
    val action = new Action {
      val name = actionName(name0)
      type TMessage = T
      val extractors = extrs
      def backend(t: T) = bk(t)
    }
    addAction(action)
  }

  def fields = RequestExtractorDefinition.compose

  def using[T](actor: ActorRef)(implicit at: Timeout, ec: ExecutionContext): (T ⇒ Future[DistributedResult]) = { (t: T) ⇒
    import akka.pattern.ask
    (actor ? t).collect {
      case dr: DistributedResult ⇒ dr
    }
  }
  def from[T] = Extractors.build[T]

}
