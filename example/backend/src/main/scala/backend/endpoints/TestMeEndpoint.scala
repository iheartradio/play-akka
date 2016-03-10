package backend.endpoints

import akka.actor.ActorSystem
import asobu.distributed.service.Action.DistributedResult
import asobu.distributed.service.{BodyExtractor, EndpointsRegistryClient, DistributedController, Controller}
import play.api.mvc.Results._


case class TestMeEndpoint(implicit sys: ActorSystem, epc: EndpointsRegistryClient) extends DistributedController {
  import concurrent.ExecutionContext.Implicits.global
  import asobu.dsl.DefaultExtractorImplicits._

  case class TestMeReq(n: Int, bar: String)

  handle[TestMeReq]("ep1",
    from(
      fields(bar = header[String]("bar")),
      BodyExtractor.empty
    )) { t =>
    println("got" + t)
    DistributedResult.from(Ok)
  }

}
