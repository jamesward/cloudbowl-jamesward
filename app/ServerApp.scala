import play.api.Mode
import play.api.mvc.Results
import play.api.routing.Router
import play.api.routing.sird._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.core.server.{DefaultAkkaHttpServerComponents, ServerConfig}

import scala.util.{Random, Try}

object ServerApp {

  // todo: better type representation of circularness
  sealed trait Direction {
    def left: Direction
    def right: Direction
  }

  case object N extends Direction {
    override def left: Direction = W
    override def right: Direction = E
  }
  case object E extends Direction {
    override def left: Direction = N
    override def right: Direction = S
  }
  case object S extends Direction {
    override def left: Direction = E
    override def right: Direction = W
  }
  case object W extends Direction {
    override def left: Direction = S
    override def right: Direction = N
  }

  object Direction {
    def apply(s: String): Option[Direction] = {
      s match {
        case "N" => Some(N)
        case "E" => Some(E)
        case "S" => Some(S)
        case "W" => Some(W)
        case  _  => None
      }
    }
  }

  sealed trait Turn
  case object L extends Turn
  case object R extends Turn

  // todo: better way to validate?
  implicit val directionReads: Reads[Direction] = __.read[String].flatMap { s =>
    Direction(s).fold(Reads.failed[Direction]("Could not parse direction"))(Reads.pure(_))
  }

  case class Self(href: String)
  implicit val selfReads = Json.reads[Self]

  case class Links(self: Self)
  implicit val linksReads = Json.reads[Links]

  case class PlayerState(x: Int, y: Int, direction: Direction, wasHit: Boolean, score: Int)
  implicit val playerStateReads = Json.reads[PlayerState]

  case class Arena(width: Int, height: Int, state: Map[String, PlayerState])
  // todo: only parse the seq once?
  implicit val arenaReads = (
    (__ \ "dims").read[Seq[Int]].collect(JsonValidationError("Could not get first element of dims")) {
      case width :: _ => width
    } ~
    (__ \ "dims").read[Seq[Int]].collect(JsonValidationError("Could not get second element of dims")) {
      case _ :: height :: _ => height
    } ~
    (__ \ "state").read[Map[String, PlayerState]]
  )(Arena.apply _)

  case class ArenaUpdate(_links: Links, arena: Arena)
  implicit val arenaUpdateReads = Json.reads[ArenaUpdate]


  def isSomeoneInLineOfFire(me: PlayerState, all: Iterable[PlayerState]): Boolean = {
    val dist = 3
    def floor(i: Int): Int = {
      if (i < 0) 0 else i
    }

    val (hittableX, hittableY) = me.direction match {
      case N =>
        (Range.inclusive(me.x, me.x), Range.inclusive(floor(me.y - 1), floor(me.y - dist)))
      case E =>
        (Range.inclusive(me.x + 1, me.x + dist), Range.inclusive(me.y, me.y))
      case S =>
        (Range.inclusive(me.x, me.x), Range.inclusive(me.y + 1, me.y + dist))
      case W =>
        (Range.inclusive(floor(me.x - 1), floor(me.x - dist)), Range.inclusive(me.y, me.y))
    }

    all.exists { playerState =>
      hittableX.contains(playerState.x) && hittableY.contains(playerState.y)
    }
  }

  def turn(me: PlayerState, turn: Turn): PlayerState = {
    turn match {
      case L => me.copy(direction = me.direction.left)
      case R => me.copy(direction = me.direction.right)
    }
  }

  def forward(me: PlayerState): PlayerState = {
    me.direction match {
      case N => me.copy(y = me.y - 1)
      case E => me.copy(x = me.x + 1)
      case S => me.copy(y = me.y + 1)
      case W => me.copy(x = me.x - 1)
    }
  }

  lazy val components = new DefaultAkkaHttpServerComponents {
    private[this] lazy val port = sys.env.get("PORT").flatMap(s => Try(s.toInt).toOption).getOrElse(8080)
    private[this] lazy val mode = if (configuration.get[String]("play.http.secret.key").contains("changeme")) Mode.Dev else Mode.Prod

    override lazy val serverConfig: ServerConfig = ServerConfig(port = Some(port), mode = mode)


    override lazy val router: Router = Router.from {
      case POST(p"/$_*") =>
        Action(parse.json[ArenaUpdate]) { request =>
          val me = request.body.arena.state(request.body._links.self.href)
          val all = request.body.arena.state.values

          // todo: better way to find the nearest route to a hit
          val move = if (isSomeoneInLineOfFire(me, all)) {
            "T"
          }
          else if (isSomeoneInLineOfFire(turn(me, L), all)) {
            "L"
          }
          else if (isSomeoneInLineOfFire(turn(me, R), all)) {
            "R"
          }
          // hittable player behind me
          else if (isSomeoneInLineOfFire(turn(turn(me, R), R), all)) {
            "R"
          }
          else if (isSomeoneInLineOfFire(forward(me), all)) {
            "F"
          }
          else if (isSomeoneInLineOfFire(forward(turn(me, L)), all)) {
            "L"
          }
          else if (isSomeoneInLineOfFire(forward(turn(me, R)), all)) {
            "R"
          }
          // dunno
          else {
            Random.shuffle(Seq("F", "R", "L")).head
          }

          Results.Ok(move)
        }
    }
  }

  def main(args: Array[String]): Unit = {
    // server is lazy so eval it to start it
    components.server
  }

}

