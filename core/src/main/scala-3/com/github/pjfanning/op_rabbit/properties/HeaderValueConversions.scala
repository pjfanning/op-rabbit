package com.github.pjfanning.op_rabbit.properties

import scala.language.implicitConversions

/**
  * Scala 3 does not treat implicit function values as implicit conversions, so expose the
  * `ToHeaderValue` instances in [[HeaderValue]] as an implicit conversion explicitly.
  */
private[properties] trait HeaderValueConversions {
  implicit def toHeaderValue[T](value: T)(implicit converter: ToHeaderValue[T, HeaderValue]): HeaderValue =
    converter(value)
}
