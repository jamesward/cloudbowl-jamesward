import ServerApp._
import org.http4s.dsl.io._
import org.specs2.mutable.Specification

class ServerAppSpec extends Specification {

  "arenaUpdateReads" >> {
    "work" >> {
      val json = """
        |{
        |  "_links": {
        |    "self": {
        |      "href": "https://foo.com"
        |    }
        |  },
        |  "arena": {
        |    "dims": [4,3],
        |    "state": {
        |      "https://foo.com": {
        |        "x": 0,
        |        "y": 0,
        |        "direction": "N",
        |        "wasHit": false,
        |        "score": 0
        |      }
        |    }
        |  }
        |}
        |""".stripMargin

      Ok(json).flatMap(_.as[ArenaUpdate]).unsafeRunSync().arena.width must beEqualTo(4)
    }
  }


  "isSomeoneInLineOfFire" >> {
    "be true for a player below of me" >> {
      val me = PlayerState(0, 0, S, false, 0)
      val other = PlayerState(0, 1, S, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must beTrue
    }
    "be true for a player to the right of me" >> {
      val me = PlayerState(0, 0, E, false, 0)
      val other = PlayerState(1, 0, E, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must beTrue
    }
    "be true for a player above of me" >> {
      val me = PlayerState(0, 1, N, false, 0)
      val other = PlayerState(0, 0, N, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must beTrue
    }
    "be true for a player to the left of me" >> {
      val me = PlayerState(1, 0, W, false, 0)
      val other = PlayerState(0, 0, W, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must beTrue
    }
    "be false for no other players" >> {
      val me = PlayerState(0, 0, S, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me)) must beFalse
    }
    "be false when facing a left or top wall" >> {
      val me = PlayerState(0, 0, W, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me)) must beFalse
    }
    "be false for an out of range player" >> {
      val me = PlayerState(0, 0, S, false, 0)
      val other = PlayerState(0, 4, S, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must beFalse
    }
  }

}
