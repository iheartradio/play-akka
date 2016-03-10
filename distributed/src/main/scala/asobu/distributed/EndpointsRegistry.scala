package asobu.distributed

import akka.actor._
import akka.cluster.Cluster
import akka.cluster.ddata.{LWWMap, LWWMapKey, DistributedData}
import akka.cluster.ddata.Replicator._
import concurrent.duration._

trait EndpointsRegistry {
  val DataKey = EndpointsRegistry.DataKey

  def writeConsistency: WriteConsistency
  def readConsistency: ReadConsistency
  def emptyData: LWWMap[EndpointDefinition]

  implicit def node: Cluster
  def replicator: ActorRef
}

object EndpointsRegistry {
  val DataKey = LWWMapKey[EndpointDefinition]("endpoints-registry")
}

case class DefaultEndpointsRegistry(implicit system: ActorSystem) extends EndpointsRegistry {
  val timeout = 30.seconds

  val writeConsistency = WriteAll(timeout)
  val readConsistency = ReadMajority(timeout)
  val emptyData = LWWMap.empty[EndpointDefinition]

  implicit val node = Cluster(system)
  val replicator: ActorRef = DistributedData(system).replicator
}

class EndpointsRegistryUpdater(registry: EndpointsRegistry) extends Actor with ActorLogging {
  import EndpointsRegistryUpdater._
  import registry._

  def receive: Receive = {
    case Add(endpointDef) ⇒
      update(Added(sender)) { m ⇒
        m + (endpointDef.id → endpointDef)
      }

    case Remove(role) ⇒
      update(Removed(sender)) { m ⇒
        m.entries.values.foldLeft(m) { (lwwMap, endpointDef) ⇒
          if (endpointDef.clusterRole.fold(false)(_ == role))
            lwwMap - endpointDef.id
          else
            lwwMap
        }
      }

    case UpdateSuccess(_, Some(result: Result)) ⇒
      log.info(s"EndpointRegistry updated by $result")
      result.confirm()
  }

  def update(res: Result)(f: LWWMap[EndpointDefinition] ⇒ LWWMap[EndpointDefinition]): Unit = {
    replicator ! Update(DataKey, emptyData, writeConsistency, Some(res))(f)
  }

}

object EndpointsRegistryUpdater {

  def props(registry: EndpointsRegistry) = Props(new EndpointsRegistryUpdater(registry))

  sealed trait UpdateRequest

  case class Add(endpointDef: EndpointDefinition) extends UpdateRequest
  case class Remove(role: String) extends UpdateRequest

  sealed trait Result {
    def replyTo: ActorRef
    def confirm(): Unit = {
      replyTo ! this
    }
  }

  case class Added(replyTo: ActorRef) extends Result
  case class Removed(replyTo: ActorRef) extends Result

}

