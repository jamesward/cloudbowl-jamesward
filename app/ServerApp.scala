import cask.Request

import scala.util.Random
import upickle.default._

object ServerApp extends cask.MainRoutes {

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

  sealed trait Move {
    override def toString: String = this match {
      case T => "T"
      case F => "F"
      case L => "L"
      case R => "R"
    }
  }
  case object T extends Move
  case object F extends Move

  sealed trait Turn
  case object L extends Move with Turn
  case object R extends Move with Turn

  case class Self(href: String)
  case class Links(self: Self)
  case class PlayerState(x: Int, y: Int, direction: Direction, wasHit: Boolean, score: Int)

  case class Arena(width: Int, height: Int, state: Map[String, PlayerState])

  case class ArenaUpdate(_links: Links, arena: Arena)

  implicit val selfReader: Reader[Self] = macroR
  implicit val linksReader: Reader[Links] = macroR
  implicit val directionReader: Reader[Direction] = reader[String].map {
    case "N" => N
    case "E" => E
    case "S" => S
    case "W" => W
  }
  implicit val playerStateReader: Reader[PlayerState] = macroR

  implicit val arenaReader: Reader[Arena] = reader[ujson.Obj].map { json =>
    val dims = read[Seq[Int]](json("dims"))
    val state = read[Map[String, PlayerState]](json("state"))
    Arena(dims(0), dims(1), state)
  }

  implicit val arenaUpdateReader: Reader[ArenaUpdate] = macroR


  def isSomeoneInLineOfFire(me: PlayerState, all: Iterable[PlayerState]): Boolean = {
    val dist = 3

    def range(a: Int, b: Int): Range = {
      Range.inclusive(Math.min(a, b), Math.max(a, b))
    }

    val (hittableX, hittableY) = me.direction match {
      case N =>
        (range(me.x, me.x), range(me.y - 1, me.y - dist))
      case E =>
        (range(me.x + 1, me.x + dist), range(me.y, me.y))
      case S =>
        (range(me.x, me.x), range(me.y + 1, me.y + dist))
      case W =>
        (range(me.x - 1, me.x - dist), range(me.y, me.y))
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

  def decide(arenaUpdate: ArenaUpdate): Move = {
    val me = arenaUpdate.arena.state(arenaUpdate._links.self.href)
    val all = arenaUpdate.arena.state.values

    // todo: better way to find the nearest route to a hit
    if (isSomeoneInLineOfFire(me, all)) {
      T
    }
    else if (isSomeoneInLineOfFire(turn(me, L), all)) {
      L
    }
    else if (isSomeoneInLineOfFire(turn(me, R), all)) {
      R
    }
    // hittable player behind me
    else if (isSomeoneInLineOfFire(turn(turn(me, R), R), all)) {
      R
    }
    else if (isSomeoneInLineOfFire(forward(me), all)) {
      F
    }
    else if (isSomeoneInLineOfFire(turn(forward(me), L), all)) {
      F
    }
    else if (isSomeoneInLineOfFire(turn(forward(me), R), all)) {
      F
    }
    else if (isSomeoneInLineOfFire(forward(turn(me, L)), all)) {
      L
    }
    else if (isSomeoneInLineOfFire(forward(turn(me, R)), all)) {
      R
    }
    // dunno
    else {
      Random.shuffle(Seq(F, R, L)).head
    }
  }

  @cask.post("/")
  def index(request: Request) = {
    val arenaUpdate = read[ArenaUpdate](request.data)
    decide(arenaUpdate).toString
  }

  initialize()

}

