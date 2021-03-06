package io.mth.route

import scalaz.syntax.foldable._
import scalaz.syntax.applicative._
import scalaz.std.list._
import scalaz.std.string._

sealed trait Path {
  val parts: List[Part]

  def asString = parts.map(_.fragment).intercalate("/")

  def length = parts.size

  def head = parts.head

  def tail = path(parts.tail)

  def init = path(parts.init)

  def last = parts.last

  def split(n: Int) = parts.splitAt(n) match {
    case (xs, ys) => (path(xs), path(ys))
  }

  def </>(p: Path): Path =
    path(parts ::: p.parts)

  def <%>[A](w: Wildcard[A]) =
    wildpart(this, w)

  def rest: WildcardPath[String] =
    wildcardpath(p => p.startsWith(this,
      rest => found((base, rest.asString)),
      notfound
    ))

  def apply[A](a: A) =
    constant(a)

  def route[A](r: Route[A]): Route[A] =
    Route.route(req =>
      if (req.path.parts == parts)
        r(req)
      else
        notfound
    )

  def constant[A](a: A): Route[A] =
    route(a.point[Route])

  def startsWith[A](p: Path, t: Path => A, f: => A) =
    if (length >= p.length) {
      val (common, rest) = parts.splitAt(p.length)
      if (common == p.parts)
        t(path(rest))
      else
        f
    } else
      f

}

object Path extends Paths {
  implicit def StringToPath(s: String): Path = PartToPath(part(s))

  implicit def PartToPath(p: Part): Path = path(p :: Nil)
}

trait Paths {
  def base: Path = path(Nil)

  def path(ps: List[Part]): Path = new Path {
    val parts = ps
  }

  def parsePath(s: String): Path =
    path(s.split("/").toList filterNot (p => p.trim == "") map (p => Part.part(p)))
}
