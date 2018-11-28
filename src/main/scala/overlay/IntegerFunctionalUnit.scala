package overlay

import chisel3._
import chisel3.util._

class IntegerFunctionalUnitIO (val w : Int, val num_ops : Int = 8)
extends Bundle{
  val in_0 = Input(UInt(w.W))
  val in_1 = Input(UInt(w.W))
  val opcode = Input(UInt(log2Ceil(num_ops).W))
  val out = Output(UInt(w.W))
}

class IntegerFunctionalUnit (val w : Int) extends Module{
  val io = IO(new IntegerFunctionalUnitIO(w, 4){
    val gt = Output(UInt(1.W))
    val eq = Output(UInt(1.W))
  })

  val opAdd :: opSub :: opMul :: opCmp :: Nil = Enum(4)
  io.out := 0.U(w.W)
  io.gt := 0.U(1.W)
  io.eq := 0.U(1.W)

  when (io.opcode === opAdd) {
    io.out := io.in_0 + io.in_1
  } .elsewhen (io.opcode === opSub) {
    io.out := io.in_0 - io.in_1
  } .elsewhen (io.opcode === opMul) {
    io.out := io.in_0 * io.in_1
  } .elsewhen (io.opcode === opCmp) {
    when (io.in_0 > io.in_1) {
      io.gt := 1.U(1.W)
    } .elsewhen (io.in_0 === io.in_1) {
      io.eq := 1.U(1.W)
    }
  }
}

object IntegerFunctionalUnit extends App {

  chisel3.Driver.execute(args, () => new IntegerFunctionalUnit(32))
}
