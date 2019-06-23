package com.spingo.scoped_fixtures

import org.scalatest.{Suite, Status, Args}
import ScopedFixtures._

object ScopedFixtures {
  type ScopedFn[T] = (T => Status) => Status
  sealed abstract class ScopedFixtureException(message: String) extends Exception(message)
  class InvalidForwardReference(fixtureType: TestFixture[Any]) extends ScopedFixtureException(s"Forward referencing of non-lazy ${fixtureType.name}")
  class ReadOutsideOfTestAttempt(fixtureType: TestFixture[Any]) extends ScopedFixtureException(s"Invalid attempt to resolve ${fixtureType.name} outside of a test")
  class DeclarationOutsideOfTestAttempt() extends ScopedFixtureException(s"Defining scoped variables outside of a test is not allowed")
}

trait TestFixture[+T] {
  def name: String = getClass.getSimpleName
  protected def instance: T
  protected val insideTestScope: () => Boolean

  def apply() = {
    if (insideTestScope())
      instance
    else
      throw(new ReadOutsideOfTestAttempt(this))
  }

  def aroundTest(body: () => Status): Status
}

class ScopedFixture[T]( protected val insideTestScope: () => Boolean, scopedGetter: ScopedFn[T] ) extends TestFixture[T] {
  private [this] var _initialized = false
  private [this] var _instance: T = _
  def instance: T = {
    if (!_initialized)
      throw(new InvalidForwardReference(this))
    else
      _instance
  }

  override def aroundTest(body: () => Status) = {
    scopedGetter({ value: T =>
      _initialized = true
      _instance = value
      val status = body()
      _initialized = false
      status
    })
  }
}

class LazyFixture[T](protected val insideTestScope: () => Boolean, getter: => T) extends TestFixture[T] {
  private class LazyGetter {
    lazy val value: T = getter
  }

  private [this] var _lazyGetter: LazyGetter = _
  def instance = _lazyGetter.value

  override def aroundTest(body: () => Status) = {
    _lazyGetter = new LazyGetter
    body()
  }
}

class EagerFixture[T]( protected val insideTestScope: () => Boolean, getter: => T) extends TestFixture[T] {
  private [this] var _initialized = false
  private [this] var _instance: T = _
  def instance = if (!_initialized)
    throw(new InvalidForwardReference(this))
  else
    _instance

  override def aroundTest(body: () => Status) = {
    _instance = getter
    _initialized = true
    val status = body()
    _initialized = false
    status
  }
}

trait ScopedFixtures extends Suite {
  private var insideTestScope = false
  private val insideTestScopeGetter = () => insideTestScope
  private [this] var fixtures = List.empty[TestFixture[Any]]
  private def registeringFixture[T](fixture: TestFixture[T]): TestFixture[T] = synchronized {
    fixtures = fixture :: fixtures
    fixture
  }


  object ScopedFixture {
    def apply[T]( fn: ScopedFn[T] ) = registeringFixture {
      new ScopedFixture[T](insideTestScopeGetter, fn)
    }
  }

  object LazyFixture {
    def apply[T]( getter: => T) = registeringFixture {
      new LazyFixture[T](insideTestScopeGetter, getter)
    }
  }
  object EagerFixture {
    def apply[T]( fn: => T) = registeringFixture {
      new EagerFixture[T](insideTestScopeGetter, fn)
    }
  }

  abstract protected override def runTest(testName: String, args: Args): Status = {
    def iterate(fns: List[TestFixture[Any]]): Status = {
      fns match {
        case head :: tail =>
          head.aroundTest { () => iterate(tail) }
        case Nil =>
          super.runTest(testName, args)
      }
    }
    insideTestScope = true
    val status = iterate(fixtures.reverse)
    insideTestScope = false
    status
  }
}
