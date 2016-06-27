package fr.cnrs.liris.common.random

import java.util.UUID

import scala.reflect.ClassTag
import scala.util.Random

/**
 * Utils related to randomness.
 */
object RandomUtils {
  private[this] val adjs = Array("autumn", "hidden", "bitter", "misty", "silent",
    "empty", "dry", "dark", "summer", "icy", "delicate", "quiet", "white", "cool",
    "spring", "winter", "patient", "twilight", "dawn", "crimson", "wispy",
    "weathered", "blue", "billowing", "broken", "cold", "damp", "falling",
    "frosty", "green", "long", "late", "lingering", "bold", "little", "morning",
    "muddy", "old", "red", "rough", "still", "small", "sparkling", "throbbing",
    "shy", "wandering", "withered", "wild", "black", "holy", "solitary",
    "fragrant", "aged", "snowy", "proud", "floral", "restless", "divine",
    "polished", "purple", "lively", "nameless", "puffy", "fluffy",
    "calm", "young", "golden", "avenging", "ancestral", "ancient", "argent",
    "reckless", "daunting", "short", "rising", "strong", "timber", "tumbling",
    "silver", "dusty", "celestial", "cosmic", "crescent", "double", "far", "half",
    "inner", "milky", "northern", "southern", "eastern", "western", "outer",
    "terrestrial", "huge", "deep", "epic", "titanic", "mighty", "powerful")

  private[this] val nouns = Array("waterfall", "river", "breeze", "moon", "rain",
    "wind", "sea", "morning", "snow", "lake", "sunset", "pine", "shadow", "leaf",
    "dawn", "glitter", "forest", "hill", "cloud", "meadow", "glade",
    "bird", "brook", "butterfly", "bush", "dew", "dust", "field",
    "flower", "firefly", "feather", "grass", "haze", "mountain", "night", "pond",
    "darkness", "snowflake", "silence", "sound", "sky", "shape", "surf",
    "thunder", "violet", "wildflower", "wave", "water", "resonance",
    "sun", "wood", "dream", "cherry", "tree", "fog", "frost", "voice", "paper",
    "frog", "smoke", "star", "sierra", "castle", "fortress", "tiger", "day",
    "sequoia", "cedar", "wrath", "blessing", "spirit", "nova", "storm", "burst",
    "protector", "drake", "dragon", "knight", "fire", "king", "jungle", "queen",
    "giant", "elemental", "throne", "game", "weed", "stone", "apogee", "bang",
    "cluster", "corona", "cosmos", "equinox", "horizon", "light", "nebula",
    "solstice", "spectrum", "universe", "magnitude", "parallax")

  /**
   * A shared [[scala.util.Random]] instance.
   */
  val random = new Random

  /**
   * Generate a random name, under the form [adjective]-[noun]-[unique suffix].
   *
   * @see https://github.com/bmarcot/haiku
   * @return A random name
   */
  def randomName: String = {
    val range = 1000 to 9999
    val suffix = (range.head + random.nextInt(range.end - range.head)).toString
    randomElement(adjs) + "-" + randomElement(nouns) + "-" + suffix
  }

  /**
   * Draw a random element from a sequence.
   *
   * @param xs A non-empty sequence
   * @tparam T Sequence elements' type
   */
  def randomElement[T](xs: Seq[T]): T = {
    require(xs.nonEmpty, "Cannot draw a random element from an empty collection")
    if (xs.size == 1) xs.head else {
      xs(random.nextInt(xs.size))
    }
  }

  /**
   * Draw a random element from an array.
   *
   * @param xs A non-empty array
   * @tparam T Array elements' type
   */
  def randomElement[T](xs: Array[T]): T = randomElement(xs.toSeq)

  /**
   * Shuffle the elements of a collection into a random order, returning the
   * result in a new collection. Unlike [[scala.util.Random.shuffle]], this method
   * uses a local random number generator, avoiding inter-thread contention.
   *
   * @param input A traversable to shuffle
   * @param rand  A random number generator
   * @tparam T Traversable elements' type
   */
  def randomize[T: ClassTag](input: TraversableOnce[T], rand: Random = new Random): Seq[T] =
    randomizeInPlace(input.toArray, rand).toSeq

  /**
   * Shuffle the elements of an array into a random order, modifying the
   * original array.
   *
   * @param input An array to shuffle
   * @param rand  A random number generator
   * @return The original array
   */
  def randomizeInPlace[T](input: Array[T], rand: Random = new Random): Array[T] = {
    for (i <- (input.length - 1) to 1 by -1) {
      val j = rand.nextInt(i)
      val tmp = input(j)
      input(j) = input(i)
      input(i) = tmp
    }
    input
  }
}
