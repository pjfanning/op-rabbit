/*
 * Copyright © 2011-2015 the spray project <http://spray.io>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.pjfanning.op_rabbit

import shapeless._

private [op_rabbit] abstract class ApplyConverter[L <: HList] {
  type In
  def apply(f: In): L ⇒ Handler
}

private [op_rabbit] object ApplyConverter extends ApplyConverterInstances {
  implicit val hac0: ApplyConverter[HNil] {
    type In = Handler
  } = new ApplyConverter[HNil] {
    type In = Handler
    def apply(fn: In) = {
      case HNil ⇒ fn
    }
  }
}
