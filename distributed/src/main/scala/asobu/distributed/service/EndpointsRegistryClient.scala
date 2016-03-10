package asobu.distributed.service

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import asobu.distributed.EndpointsRegistryUpdater.Add
import asobu.distributed._

import scala.concurrent.Future

trait EndpointsRegistryClient {
  def add(endpointDefinition: EndpointDefinition): Future[Unit]
}

class EndpointsRegistryClientImp(registry: EndpointsRegistry)(implicit system: ActorSystem, ao: Timeout) extends EndpointsRegistryClient {
  import system.dispatcher

  val clientActor = system.actorOf(EndpointsRegistryUpdater.props(registry), "asobu-endpoint-registry-client")

  def add(endpointDefinition: EndpointDefinition): Future[Unit] =
    (clientActor ? Add(endpointDefinition)).map(_ ⇒ ())

}

