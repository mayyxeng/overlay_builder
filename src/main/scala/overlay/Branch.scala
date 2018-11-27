package overlay

import chisel3._
import chisel3.util.log2Ceil

class BranchControlIO (val n: Int) extends Bundle {
  val conditional = Input(UInt(log2Ceil(n).W))
  val valid_in = Input(UInt(1.W))
  val stall_out = Output(UInt(1.W))
  val valid_out = Output(Vec(n, UInt(1.W)))
  val stall_in = Input(Vec(n, UInt(1.W)))
}
class BranchControl (val n : Int) extends Module {
  val io = IO(new BranchControlIO(n))

  val zero_init = Seq.fill(n){0.U(1.W)}
  io.valid_out := VecInit(zero_init)
  io.valid_out(io.conditional) := 1.U(1.W)
  io.stall_out := io.stall_in(io.conditional)

}
class Branch (val n : Int, val w : Int) extends Module {
  val io = IO(new BranchControlIO(n) {
    val data_in = Input(UInt(w.W))
    val data_out = Output(Vec(n, UInt(w.W)))
  })
  val controller = Module(new BranchControl(n))
  controller.io.valid_in := io.valid_in
  controller.io.stall_in := io.stall_in
  controller.io.conditional := io.conditional
  io.stall_out := controller.io.stall_out
  io.valid_out := controller.io.valid_out

  val zero_init = Seq.fill(n){0.U(w.W)}
  io.data_out := VecInit(zero_init)
  io.data_out(io.conditional) := io.data_in
}
object BranchControl extends App {
  chisel3.Driver.execute (args, () => new BranchControl(3))
}

object Branch extends App {
  chisel3.Driver.execute (args, () => new Branch(3 ,32))
}
