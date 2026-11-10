/*
 * Copyright (c) 2011-16 Miles Sabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pjfanning.op_rabbit.compat

/**
  * Minimal HList implementation used on Scala 3, where shapeless 2 is not available.
  * It is derived from the shapeless 2 HList and Prepend code and mirrors the subset of that API op-rabbit relies on.
  */
sealed trait HList extends Product with Serializable

final case class ::[+H, +T <: HList](head: H, tail: T) extends HList {
  def ::[G](g: G): G :: H :: T = new ::(g, this)
  override def toString: String = s"$head :: $tail"
}

sealed trait HNil extends HList {
  def ::[H](h: H): H :: HNil = new ::(h, this)
}

case object HNil extends HNil {
  override def toString: String = "HNil"
}

/**
  * Type class witnessing that `S` can be appended to `P`.
  */
trait Prepend[P <: HList, S <: HList] {
  type Out <: HList
  def apply(prefix: P, suffix: S): Out
}

object Prepend {
  type Aux[P <: HList, S <: HList, Out0 <: HList] = Prepend[P, S] { type Out = Out0 }

  implicit def hnilPrepend[S <: HList]: Aux[HNil, S, S] =
    new Prepend[HNil, S] {
      type Out = S
      def apply(prefix: HNil, suffix: S): S = suffix
    }

  implicit def hlistPrepend[H, T <: HList, S <: HList](implicit pt: Prepend[T, S]): Aux[H :: T, S, H :: pt.Out] =
    new Prepend[H :: T, S] {
      type Out = H :: pt.Out
      def apply(prefix: H :: T, suffix: S): H :: pt.Out = new ::(prefix.head, pt(prefix.tail, suffix))
    }
}
