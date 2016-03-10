package backend

import akka.actor._
import akka.cluster.Cluster
import akka.routing.FromConfig
import akka.util.Timeout
import asobu.distributed.{DefaultEndpointsRegistry, EndpointDefinition}
import asobu.distributed.service.{EndpointsRegistryClientImp, EndpointsRegistryClient}
import backend.endpoints.TestMeEndpoint
import com.typesafe.config.ConfigFactory
import backend.factorial._
import scala.collection.JavaConversions._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.util.Try
import concurrent.duration._

/**
 * Booting a cluster backend node with all actors
 */
object Backend extends App {

  // Simple cli parsing
  val port = args match {
    case Array()     => "0"
    case Array(port) => port
    case args        => throw new IllegalArgumentException(s"only ports. Args [ $args ] are invalid")
  }

  // System initialization
  val properties = Map(
      "akka.remote.netty.tcp.port" -> port
  )
  
  implicit val system: ActorSystem = ActorSystem("application", (ConfigFactory parseMap properties)
    .withFallback(ConfigFactory.load())
  )

  // Deploy actors and services
  FactorialBackend startOn system
  implicit val ao: Timeout = 30.seconds

  Cluster(system).registerOnMemberUp {
    implicit val rec: EndpointsRegistryClient = new EndpointsRegistryClientImp(DefaultEndpointsRegistry())

    val initControllers = Try {
      TestMeEndpoint()
    }

    initControllers.recover {
      case e: Throwable =>
        system.log.error(e, s"Cannot initialize controllers, Exiting")
        system.terminate()
    }
  }

}
