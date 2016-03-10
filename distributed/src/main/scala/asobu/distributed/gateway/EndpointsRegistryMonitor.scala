package asobu.distributed.gateway

import akka.actor._
import akka.cluster.ddata.LWWMap
import akka.cluster.ddata.Replicator._
import asobu.distributed.{EndpointDefinition, EndpointsRegistry}
import scala.concurrent.{duration, Await, Promise}, duration._

class EndpointsRegistryMonitor(registry: EndpointsRegistry, endpointsRouter: ActorRef) extends Actor with ActorLogging {
  import registry._

  replicator ! Subscribe(DataKey, self)
  replicator ! Get(DataKey, ReadMajority(30.seconds)) //todo: hardcoded timeout here

  def receive = monitoring(Nil)

  def monitoring(endpoints: List[Endpoint]): Receive = {

    def updateEndpoints(data: LWWMap[EndpointDefinition]): Unit = {
      val newDefs: List[EndpointDefinition] = data.entries.values.toList

      val (toPurge, toKeep) = endpoints.partition { ep ⇒
        newDefs.exists { newDef ⇒ newDef.id == ep.definition.id && newDef != ep.definition }
      }

      val toAdd = newDefs.filterNot { newDef ⇒
        toKeep.exists(_.definition == newDef)
      }

      toPurge.foreach(_.shutdown())

      val updatedEndpoints = toKeep ++ toAdd.map(Endpoint(_))

      endpointsRouter ! EndpointsRouter.Update(updatedEndpoints)

      context become monitoring(updatedEndpoints)
    }

    {
      case c @ Changed(DataKey) ⇒
        log.info("Endpoints regenerated due to registry change")
        updateEndpoints(c.get(DataKey))

      case g @ GetSuccess(DataKey, _) ⇒
        log.info("Endpoints initialized")
      //        updateEndpoints(g.get(DataKey))

      case GetFailure(DataKey, _) ⇒
        log.error("Failed to get endpoint registry")

      case NotFound(DataKey, _) ⇒
        log.info("No data in endpoint registry yet.")

    }
  }

}

object EndpointsRegistryMonitor {
  def props(registry: EndpointsRegistry, router: ActorRef) = Props(new EndpointsRegistryMonitor(registry, router))
}
