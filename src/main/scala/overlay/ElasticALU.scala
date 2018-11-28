package overlay

import chisel3._

class ElasticALUIO (w : Int) extends IntegerFunctionalUnitIO(w, 4) {
  val gt = Output(UInt(1.W))
  val eq = Output(UInt(1.W))
  val stall_in = Input(UInt(1.W))
  val stall_out_vector = Output(Vec(2, UInt(1.W)))
  val valid_in_vector = Input(Vec(2, UInt(1.W)))
  val valid_out = Output(UInt(1.W))
}

class ElasticALU (val w : Int) extends Module {

  val io = IO(new ElasticALUIO(w))

  val entry_join = Module(new Join(2, w))
  val alu = Module(new IntegerFunctionalUnit(w))
  
  entry_join.io.stall_in := io.stall_in
  entry_join.io.valid_in_vector := io.valid_in_vector
  io.valid_out := entry_join.io.valid_out
  io.stall_out_vector := entry_join.io.stall_out_vector

  entry_join.io.data_in(0) := io.in_0
  entry_join.io.data_in(1) := io.in_1

  alu.io.in_0 := entry_join.io.data_out(0)
  alu.io.in_1 := entry_join.io.data_out(1)

  io.out := alu.io.out

  alu.io.opcode := io.opcode

  io.gt := alu.io.gt
  io.eq := alu.io.eq


}

object ElasticALU extends App {

  chisel3.Driver.execute(args, () => new ElasticALU(32))
}
