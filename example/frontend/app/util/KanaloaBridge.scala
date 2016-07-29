package util

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.util.Timeout
import api.ErrorResult
import asobu.distributed.gateway.AbstractKanaloaBridge
import kanaloa.reactive.dispatcher._
import play.api.Configuration
import KanaloaBridge._
import concurrent.duration._

class KanaloaBridge @Inject() (implicit configuration: Configuration, system: ActorSystem) extends AbstractKanaloaBridge {

  override protected def resultChecker: ResultChecker = {
    case e: ErrorResult[_] ⇒ Left(e.toString)
    case m                ⇒ Right(m)
  }
}

object KanaloaBridge {
  implicit val timeout: Timeout = 30.seconds
}
