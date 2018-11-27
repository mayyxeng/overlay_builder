package overlay

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class MaxNTester(c: MaxN) extends PeekPokeTester(c) {
  val ins = Array.fill(c.n){ 0 }
  for (i <- 0 until 10) {
    var mx = 0
    for (i <- 0 until c.n) {
      ins(i) = rnd.nextInt(1 << 31)
      poke(c.io.ins(i), ins(i))
      mx = if (ins(i) > mx) ins(i) else mx
    }
    step(1)
    expect(c.io.out, mx)
  }
}
class MaxNSpec extends ChiselFlatSpec {
  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new MaxN(8, 32)) {
      c => new MaxNTester(c)
    } should be (true)
  }
}
