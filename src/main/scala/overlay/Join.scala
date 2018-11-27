package overlay

import chisel3._

class JoinControl (val n:Int) extends Module {
  val io = IO(new Bundle {
    val valid_in_vector = Input(Vec(n, UInt(1.W)))
    val stall_out_vector = Output(Vec(n, UInt(1.W)))
    val valid_out = Output(UInt(1.W))
    val stall_in = Input(UInt(1.W))
  })

  /* Valid out signal is valid_in signals anded together */


  val valid_in_and_reduced = io.valid_in_vector.reduce(_ & _)
  io.valid_out := valid_in_and_reduced

  val unsafe_stall = Wire(UInt(1.W))
  unsafe_stall := ~(~io.stall_in & valid_in_and_reduced)
  for (i <- 0 until n) {
    io.stall_out_vector(i) := io.valid_in_vector(i) & unsafe_stall
  }
}

object JoinControl extends App {
  chisel3.Driver.execute(args, () => new JoinControl(3))
}
