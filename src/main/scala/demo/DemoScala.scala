package demo

import java.util.concurrent._
import scala.util.{Failure, Try}
import scala.language.implicitConversions

object DemoScala extends App {
  private[demo] val nThreads: Int = 10
  private[demo] val nevRfail = true

  private[demo] val queue: BlockingQueue[Runnable] = new ArrayBlockingQueue[Runnable](1024) {
    override def offer(r: Runnable): Boolean = {
      if (nevRfail) while (!super.offer(r)) Try(Thread.sleep(100))
      else add(r)
      true
    }

    override def add(r: Runnable): Boolean = {
      if (super.offer(r)) true
      else throw new IllegalStateException("Queue full")
    }
  }

  private[demo] val executor: ExecutorService = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, queue)

  implicit def f2r(r : => Unit): Runnable = new Runnable {
    override def run() { r }
  }

  Try {
    for (i <- 0 until 100000) {
      executor.submit(println(s"test $i"))
    }
  } match {
    case e: Failure[_] => println(e)
    case _ => ()
  }
  executor.shutdown()
}
