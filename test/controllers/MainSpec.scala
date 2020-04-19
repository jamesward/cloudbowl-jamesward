package controllers

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import Main._


class MainSpec extends AnyWordSpec with Matchers {

  "arenaUpdateReads" must {
    "work" in {
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

      Json.parse(json).validate[ArenaUpdate].isSuccess must be (true)
    }
  }


  "isSomeoneInLineOfFire" must {
    "be true for a player below of me" in {
      val me = PlayerState(0, 0, S, false, 0)
      val other = PlayerState(0, 1, S, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (true)
    }
    "be true for a player to the right of me" in {
      val me = PlayerState(0, 0, E, false, 0)
      val other = PlayerState(1, 0, E, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (true)
    }
    "be true for a player above of me" in {
      val me = PlayerState(0, 1, N, false, 0)
      val other = PlayerState(0, 0, N, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (true)
    }
    "be true for a player to the left of me" in {
      val me = PlayerState(1, 0, W, false, 0)
      val other = PlayerState(0, 0, W, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (true)
    }
    "be false for no other players" in {
      val me = PlayerState(0, 0, S, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me)) must be (false)
    }
    "be false when facing a left or top wall" in {
      val me = PlayerState(0, 0, W, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me)) must be (false)
    }
    "be false for an out of range player" in {
      val me = PlayerState(0, 0, S, false, 0)
      val other = PlayerState(0, 4, S, false, 0)

      Main.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (false)
    }
  }

}
