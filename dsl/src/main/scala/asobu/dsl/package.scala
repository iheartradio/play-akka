package asobu

import cats.data.{Kleisli, XorT}
import play.api.mvc.{AnyContent, Request, Result}
import play.core.routing.RouteParams

import scala.concurrent.Future

package object dsl {

  type Processor[-RMT, +PRT] = Request[RMT] ⇒ Future[PRT]

  type Directive[-RMT] = Processor[RMT, Result]

  type PartialDirective[-RMT] = PartialFunction[Request[RMT], Future[Result]]

  type Filter[-RMT] = (Request[RMT], ⇒ Future[Result]) ⇒ Future[Result]

  type Extractor[T] = Kleisli[ExtractResult, Request[AnyContent], T]

}

