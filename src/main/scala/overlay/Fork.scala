package overlay

import chisel3._
import chisel3.util._

class EagerForkDecoupledIO (val n: Int, val w: Int) extends Bundle {
  val input = Flipped(Decoupled(UInt(w.W)))
  val output_vector = Vec(n, Decoupled(UInt(w.W)))
}
class EagerForkDecoupled (val n: Int, val w: Int) extends Module {

  val io = IO(new EagerForkDecoupledIO(n, w))

  val v_reg = RegInit(VecInit(Seq.fill(n){0.U(1.W)}))

  val anded_stalls = Wire(Vec(n, UInt(1.W)))
  val ready_out = ~anded_stalls.reduce(_|_)

  for (i <- 0 until n) {
    v_reg(i) := anded_stalls(i) | (~io.input.valid) | ready_out
    io.output_vector(i).valid := io.input.valid & v_reg(i)
    anded_stalls(i) := v_reg(i) & (~io.output_vector(i).ready)
    io.output_vector(i).bits := io.input.bits
  }
  io.input.ready := ready_out
}

class EagerForkControlIO (val n : Int) extends Bundle {
  val valid_in = Input(UInt(1.W))
  val stall_in_vector = Input(Vec(n, UInt(1.W)))
  val valid_out_vector = Output(Vec(n, UInt(1.W)))
  val stall_out = Output(UInt(1.W))
}
class EagerForkControl(val n : Int) extends Module {
  //override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val io = IO(new EagerForkControlIO(n))
  val initVecVal = Seq.fill(n){0.U(1.W)}
  val v_reg = RegInit(VecInit(initVecVal))
  val anded_stalls = Wire(Vec(n, UInt(1.W)))
  val stall_out = anded_stalls.reduce(_|_)
  val valid_and_stall_not = ~(io.valid_in & stall_out)
  for (i <- 0 until n) {
    v_reg(i) := anded_stalls(i) | valid_and_stall_not
    io.valid_out_vector(i) := io.valid_in & v_reg(i)
    anded_stalls(i) := v_reg(i) & io.stall_in_vector(i)

  }
  io.stall_out := stall_out
}
class EagerFork(val n : Int, val w : Int) extends Module {
  val io = IO(new EagerForkControlIO(n){
    val data_in = Input(UInt(w.W))
    val data_out= Output(Vec(n, UInt(w.W)))
  })
  val data_in_replica = Seq.fill(n){ io.data_in }
  io.data_out := VecInit(data_in_replica)

  val fork_control = Module(new EagerForkControl(n))
  fork_control.io.valid_in := io.valid_in
  fork_control.io.stall_in_vector := io.stall_in_vector
  io.valid_out_vector := fork_control.io.valid_out_vector
  io.stall_out := fork_control.io.stall_out
}
object EagerForkControl extends App {
  chisel3.Driver.execute(args, () => new EagerForkControl(3))
}

object EagerFork extends App {
  chisel3.Driver.execute(args, () => new EagerFork(3, 32))
  chisel3.Driver.execute(args, () => new EagerForkDecoupled(3, 32))
}
