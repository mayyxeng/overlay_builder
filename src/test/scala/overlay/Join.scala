package overlay

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


class JoinControlTester(c: JoinControl) extends PeekPokeTester(c) {

  for (i <- 0 until 20) {
    var valid_in = Seq.fill(c.n)(rnd.nextInt(2))
    var valid_in_reduced = valid_in.reduce(_ & _)
    var stall_in = rnd.nextInt(1)
    var unsafe_stall = ~(~stall_in & valid_in_reduced)
    var stall_out = Seq.tabulate(c.n)(i => valid_in(i) & unsafe_stall)
    var valid_out = valid_in_reduced
    for (i <- 0 until c.n) {
      poke(c.io.valid_in_vector(i), valid_in(i))

    }
    poke(c.io.stall_in, stall_in)
    step(1)
    expect(c.io.valid_out, valid_out)
    for (i <- 0 until c.n) {
      expect(c.io.stall_out_vector(i), stall_out(i))
    }
    //expect(c.io.stall_out, stall_out)
  }

/* Run test with
  sbt 'testOnly overlay.JoinControlSpec'
*/
class JoinControlSpec extends ChiselFlatSpec {
  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new JoinControl(3)) {
      c => new JoinControlTester(c)
    } should be (true)
  }
}
object JoinControlTest extends App {
  iotesters.Driver.execute(args, () => new JoinControl(3)) {
    c => new JoinControlTester(c)
  }
}
