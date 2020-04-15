import ServerApp._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class ServerAppSpec extends AnyWordSpec with Matchers {

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
    "be true for a player right in front of me" in {
      val me = PlayerState(0, 0, S, false, 0)
      val other = PlayerState(0, 1, S, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (true)
    }
    "be false for no other players" in {
      val me = PlayerState(0, 0, S, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me)) must be (false)
    }
    "be false for an out of range player" in {
      val me = PlayerState(0, 0, S, false, 0)
      val other = PlayerState(0, 4, S, false, 0)

      ServerApp.isSomeoneInLineOfFire(me, Iterable(me, other)) must be (false)
    }
  }

}
