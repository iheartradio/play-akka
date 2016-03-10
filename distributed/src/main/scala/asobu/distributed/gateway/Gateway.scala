package asobu.distributed.gateway

import javax.inject.{Provider, Inject, Singleton}

import akka.actor.{Deploy, ActorRef, ActorSystem, Props}

import akka.routing.{SmallestMailboxPool, DefaultOptimalSizeExploringResizer, RoundRobinPool}
import asobu.distributed.{DefaultEndpointsRegistry, EndpointDefinition, EndpointsRegistry}
import play.api.{Configuration, Environment}
import play.api.inject.Module

@Singleton
class Gateway @Inject() (implicit system: ActorSystem) {
  val akkaRouterForEndpointsRouters: ActorRef = {
    val routerProps =
      SmallestMailboxPool(
        8,
        Some(DefaultOptimalSizeExploringResizer(upperBound = 200))
      ).props(EndpointsRouter.props)
    system.actorOf(routerProps, "gateway-router")
  }

  val monitor = system.actorOf(
    EndpointsRegistryMonitor.props(
      DefaultEndpointsRegistry(),
      akkaRouterForEndpointsRouters
    )
  )

}

/**
 * Eagerly start the Gateway
 */
class GateWayModule extends Module {
  def bindings(
    environment: Environment,
    configuration: Configuration
  ) = Seq(
    bind[Gateway].toSelf.eagerly
  )
}
