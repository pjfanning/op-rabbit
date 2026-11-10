package com.github.pjfanning.op_rabbit

/**
  * Scala 2 HList support, backed by shapeless.
  */
package object compat {
  type HList = shapeless.HList
  type HNil = shapeless.HNil
  val HNil: shapeless.HNil.type = shapeless.HNil
  type ::[H, T <: HList] = shapeless.::[H, T]
  val :: : shapeless.::.type = shapeless.::
  type Prepend[P <: HList, S <: HList] = shapeless.ops.hlist.Prepend[P, S]
}
