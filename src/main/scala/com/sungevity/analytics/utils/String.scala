package com.sungevity.analytics.utils

import scala.math.min

object String {

  implicit class RichString(str: String) {

    def levenshteinDistance[A](that: Iterable[A]) =
      ((0 to that.size).toList /: str.toCharArray)((prev, x) =>
        (prev zip prev.tail zip that).scanLeft(prev.head + 1) {
          case (h, ((d, v), y)) => min(min(h + 1, v + 1), d + (if (x == y) 0 else 1))
        }) last


    def ≈≈(str: String) = this.levenshteinDistance(str) < 2

  }

}
