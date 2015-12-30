package asobu.distributed

import java.io.File

import play.routes.compiler.{RoutesCompilationError, Route, RoutesFileParser}

case class EndPoint(routeString: String) {

}

object EndPoint {
  def parse(endPoint: EndPoint): Either[Seq[RoutesCompilationError], Option[Route]] = RoutesFileParser.parseContent(
    endPoint.routeString, new File("remote-routes")
  ).right.map(_.headOption.map(_.asInstanceOf[Route]))
}
