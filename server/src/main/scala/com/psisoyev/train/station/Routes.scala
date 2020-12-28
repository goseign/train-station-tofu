package com.psisoyev.train.station

import cats.Monad
import cats.effect.Sync
import com.psisoyev.train.station.Main.Routes
import com.psisoyev.train.station.arrival.ArrivalValidator.ArrivalError
import com.psisoyev.train.station.arrival.{ ArrivalValidator, Arrivals, ExpectedTrains }
import com.psisoyev.train.station.departure.Departures
import com.psisoyev.train.station.departure.Departures.DepartureError
import cr.pulsar.Producer
import io.chrisdavenport.log4cats.Logger
import org.http4s.implicits._
import tofu.generate.GenUUID
import tofu.{ Raise, WithRun }

object Routes {
  def make[
    Init[_]: Sync,
    Run[_]: Monad: GenUUID: WithRun[*[_], Init, Context]: Logger: Tracing
  ](
    config: Config,
    producer: Producer[Init, Event],
    expectedTrains: ExpectedTrains[Run]
  )(implicit
    A: Raise[Run, ArrivalError],
    D: Raise[Run, DepartureError]
  ): Routes[Init] = {
    val arrivalValidator = ArrivalValidator.make[Run](expectedTrains)
    val arrivals         = Arrivals.make[Run](config.city, expectedTrains)
    val departures       = Departures.make[Run](config.city, config.connectedTo)

    new StationRoutes[Init, Run](arrivals, arrivalValidator, producer, departures).routes.orNotFound
  }
}