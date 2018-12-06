package overlay

import chisel3._
import chisel3.util._

class MergeIO(val n : Int, val w : Int) extends Bundle {
  val data_in_vector = Input(Vec(n, UInt(w.W)))
  val data_out = Output(UInt(w.W))
  val valid_out = Output(UInt(1.W))
  val valid_in_vector = Input(Vec(n, UInt(1.W)))
  val stall_out_vector = Output(Vec(n, UInt(1.W)))
  val stall_in = Input(UInt(1.W))
}
class MergeIODecoupled(val n: Int, val w: Int) extends Bundle {
  val input_vector = Flipped(Vec(n, Decoupled(UInt(w.W))))
  val output = Decoupled(UInt(w.W))
}

class MergeDecoupled(val n: Int, val w: Int) extends Module {
  val io = IO(new MergeIODecoupled(n, w))

  val selection_seq = Seq.tabulate(n) (n => (n.U, io.input_vector(n).bits))
  val valid_in_as_word = Seq.tabulate(n) (n => io.input_vector(n).valid)
  val sel = PriorityEncoder(valid_in_as_word)
  io.output.bits := MuxLookup(sel, 0.U, selection_seq)

  val valid_in_vector = Seq.tabulate(n) (n => io.input_vector(n).valid)
  io.output.valid := valid_in_vector.reduce(_&_)
  for (i <- 0 until n) {
    io.input_vector(i).ready := io.output.ready
  }

}
class Merge(val n : Int, val w : Int) extends Module {

  override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val io = IO(new MergeIO(n, w))

  val valid_in_as_word = Seq.tabulate(n)(n => io.valid_in_vector(n).toBool)
  val mux_sel = PriorityEncoder(valid_in_as_word)
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
  chisel3.Driver.execute(args, () => new MergeDecoupled(5, 32))
}
