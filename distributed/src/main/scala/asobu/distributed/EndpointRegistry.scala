package asobu.distributed

import javax.inject.{Inject, Provider, Singleton}

import akka.actor._
import akka.cluster.Cluster

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

class EndpointRegistry extends Actor with ActorLogging {
  def receive: Receive = ???
}

object EndpointRegistry {

}

@Singleton
class EndpointRegistryProvider @Inject() (system: ActorSystem) extends Provider[ActorRef] {
  val promise = Promise[ActorRef]
  Cluster(system) registerOnMemberUp {
    promise.success(system.actorOf(Props[EndpointRegistry]))
  }

  def get(): ActorRef = Await.result(promise.future, 30.seconds)
}
