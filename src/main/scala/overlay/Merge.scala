package overlay

import chisel3._

class Merge(val n : Int, val w : Int) extends Module {

  override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val io = IO(new Bundle{
    val data_in_vector = Input(Vec(n, UInt(w.W)))
    val data_out = Output(UInt(w.W))
    val valid_out = Output(UInt(1.W))
    val valid_in_vector = Input(Vec(n, UInt(1.W)))
    val stall_out_vector = Output(Vec(n, UInt(1.W)))
    val stall_in = Input(UInt(1.W))
  })

  val valid_in_as_word = Seq.tabulate(n)(n => io.valid_in_vector(n).toBool)
  val mux_sel = util.PriorityEncoder(valid_in_as_word)
  //mux_sel :=
  val data_mux = Module(new MuxN(n, w))
  for (i <- 0 until n) {
    data_mux.io.ins(i) := io.data_in_vector(i)
  }
  data_mux.io.sel := mux_sel
  io.data_out := data_mux.io.out
  io.valid_out := io.valid_in_vector.reduce(_&_)
  for (i <- 0 until n) {
    io.stall_out_vector(i) := io.stall_in
  }
}

object Merge extends App {
  chisel3.Driver.execute(args, () => new Merge(5, 32))
}
