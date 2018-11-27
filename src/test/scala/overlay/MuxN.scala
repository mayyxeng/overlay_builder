package overlay

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.util.log2Ceil

class MuxNTester(c: MuxN) extends PeekPokeTester(c) {
  val ins = Array.fill(c.n){ 0 }
  for (i <- 0 until 10) {
    var out = 0
    val sel = rnd.nextInt(8)
    for (i <- 0 until c.n) {
      ins(i) = rnd.nextInt(1 << 25)
      poke(c.io.ins(i), ins(i))
    }
    poke(c.io.sel, sel)
    out = ins(sel)
    step(1)
    expect(c.io.out, out)
  }
}
class MuxNSpec extends ChiselFlatSpec {
  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new MuxN(9, 32)) {
      c => new MuxNTester(c)
    } should be (true)
  }
}
