package asobu.distributed.gateway

import akka.actor.{Props, Actor}
import asobu.distributed.gateway.Endpoint.Prefix
import asobu.distributed.gateway.EndpointsRouter.Update

class EndpointsRouter extends Actor {

  def receive: Receive = handling(Map())

  def handling(endpoints: Map[Prefix, Endpoint]): Receive = {
    def toPartial(endpoint: Endpoint): Receive = {
      case req @ endpoint(rp) ⇒
        val rf = endpoint.handle(rp, req)
        import context.dispatcher
        rf.foreach(r ⇒ println("response back" + r))
    }

    val endpointsHandlerPartial = //todo: improve performance by doing a prefix search first
      endpoints.values.map(toPartial).foldLeft(PartialFunction.empty: Receive)(_ orElse _)

    ({
      case Update(endpoints) ⇒
        val updated = endpoints.map(ep ⇒ ep.definition.prefix → ep).toMap
        context become handling(updated)
    }: Receive) orElse endpointsHandlerPartial
  }

}

object EndpointsRouter {

  def props = Props(new EndpointsRouter)
  case class Update(endpoints: List[Endpoint])
}
