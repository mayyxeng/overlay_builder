package overlay

import chisel3._
import chisel3.util.{DecoupledIO, Decoupled}

// class JoinDecoupledIO(val n: Int, val w: Int) extends Bundle {
//   val input_vector = Flipped(Vec(n, Decoupled(UInt(w.W))))
//   val output = Decoupled(UInt(w.W))
// }
// class JoinDecouped(val n, Int, val w: Int) extends Module {
//   val io = IO(new JoinDecoupedIO(n, w))
//
//   val valid_in_vec = Seq.tabulate(n) {n => io.input_vector(n).valid}
//   val valid_reduced = valid_in_vec.reduce(_ & _)
//   val unsafe_ready = (io.output.ready & valid_reduced)
//   for(i <- 0 until n) {
//     io.input_vector(i).ready := io.input_vector(i).valid & (~unsafe_ready)
//   }
//   io.output.ready := valid_reduced
// }
class JoinControlIO (val n : Int) extends Bundle {
  val valid_in_vector = Input(Vec(n, UInt(1.W)))
  val stall_out_vector = Output(Vec(n, UInt(1.W)))
  val valid_out = Output(UInt(1.W))
  val stall_in = Input(UInt(1.W))
}
class JoinControl (val n:Int) extends Module {
  val io = IO(new JoinControlIO(n))
  /* Valid out signal is valid_in signals anded together */
  val valid_in_and_reduced = io.valid_in_vector.reduce(_ & _)
  io.valid_out := valid_in_and_reduced

  val unsafe_stall = Wire(UInt(1.W))
  unsafe_stall := ~(~io.stall_in & valid_in_and_reduced)
  for (i <- 0 until n) {
    io.stall_out_vector(i) := io.valid_in_vector(i) & unsafe_stall
  }
}
class Join (val n : Int, val w : Int) extends Module {
  val io = IO(new JoinControlIO(n) {
    val data_in = Input(Vec(n, UInt(w.W)))
    val data_out = Output(Vec(n, UInt(w.W)))
  })
  io.data_out := io.data_in
  val controller = Module(new JoinControl(n))
  controller.io.valid_in_vector := io.valid_in_vector
  controller.io.stall_in := io.stall_in
  io.valid_out := controller.io.valid_out
  io.stall_out_vector := controller.io.stall_out_vector
}
object JoinControl extends App {
  chisel3.Driver.execute(args, () => new JoinControl(3))
}

object Join extends App {
  chisel3.Driver.execute(args, () => new Join(3, 32))
}
