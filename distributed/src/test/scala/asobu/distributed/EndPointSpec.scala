package asobu.distributed

import org.specs2.mutable.Specification
import play.api.http.HttpVerbs

import play.routes.compiler._

object EndPointSpec extends Specification {
  import play.api.http.HttpVerbs._
  "Endpoint" >> {
    val routeString =
      """
        |# Some Comments
        |# Some other Comments
        |GET   /api/dsl/factorialOf/:n   a.b.c(n: Int)
        |
      """.stripMargin

    val parserResult = EndPoint.parse(EndPoint(routeString))
    parserResult must beRight[Option[Route]]
    val route: Route = parserResult.right.get.get

    route.verb === HttpVerb(GET)
    route.comments === List(Comment(" Some Comments"), Comment(" Some other Comments"))
    val parameter = route.call.parameters.get.head
    parameter.name === "n"
    parameter.typeName === "Int"
  }
}
