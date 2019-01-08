package overlay


import chisel3._
import chisel3.util._
class CUCTRL (val cncs: Int) extends Bundle {
  val cross_in = Vec(cncs, UInt(log2Ceil(8).W))
  val cross_out = Vec(9, UInt(log2Ceil(cncs).W))
  val alu = new ALUCTRL()
}
class CUIO (val cncs: Int, val wdth: Int) extends Bundle {
  val input = Flipped(new DecoupledConnection(cncs, wdth))
  val output = new DecoupledConnection(cncs, wdth)
  val ctrl = Input(new CUCTRL(cncs))
}
class CU (val cncs: Int, val wdth: Int) extends Module {

  val io = IO(new CUIO(cncs, wdth))

  val cross_in = Module(new CrossBarDecoupled(cncs, 8, wdth))
  val alu = Module(new ALU(wdth))
  val branch = Module(new BranchDecoupled(2, wdth))
  val merge = Module(new MergeDecoupled(2, wdth))
  val fork = Module(new EagerForkDecoupled(4, wdth))
  val buffer = Module(new ElasticBufferDecoupled(wdth))
  val cross_out = Module(new CrossBarDecoupled(9, cncs, wdth))

  // inputs
  cross_in.io.inputs <> io.input.connection

  cross_in.io.outputs(0) <> alu.io.in1
  cross_in.io.outputs(1) <> alu.io.in2
  cross_in.io.outputs(2) <> branch.io.input

  branch.io.conditional := cross_in.io.outputs(3).bits
  cross_in.io.outputs(3).ready := true.B

  cross_in.io.outputs(4) <> merge.io.input_vector(0)
  cross_in.io.outputs(5) <> merge.io.input_vector(1)
  cross_in.io.outputs(6) <> fork.io.input
  cross_in.io.outputs(7) <> buffer.io.input


  // outputs
  cross_out.io.inputs(0) <> alu.io.out
  cross_out.io.inputs(1) <> branch.io.output_vector(0)
  cross_out.io.inputs(2) <> branch.io.output_vector(1)
  cross_out.io.inputs(3) <> merge.io.output
  cross_out.io.inputs(4) <> fork.io.output_vector(0)
  cross_out.io.inputs(5) <> fork.io.output_vector(1)
  cross_out.io.inputs(6) <> fork.io.output_vector(2)
  cross_out.io.inputs(7) <> fork.io.output_vector(3)
  cross_out.io.inputs(8) <> buffer.io.output

  cross_out.io.outputs <> io.output.connection


  // control signals
  alu.io.ctrl := io.ctrl.alu
  cross_out.io.ctrl := io.ctrl.cross_out
  cross_in.io.ctrl := io.ctrl.cross_in

}
class ProcessingElementIO (val w : Int) extends Bundle {
  val north_in = Input(UInt(w.W))
  val south_in = Input(UInt(w.W))
  val west_in = Input(UInt(w.W))
  val east_in = Input(UInt(w.W))
  val north_out = Output(UInt(w.W))
  val south_out = Output(UInt(w.W))
  val west_out = Output(UInt(w.W))
  val east_out = Output(UInt(w.W))
  //val control = Input()
}

class ProcessingElement (val w : Int) extends Module {
  val io = IO(new ProcessingElementIO(w))




  io.north_out := io.north_in
  io.south_out := io.south_in
  io.west_out := io.west_in
  io.east_out := io.east_in

}

class ALUCTRL extends Bundle {
  val opcode = UInt(2.W)
}
class ALUIO (val w: Int) extends Bundle {
  val in1 = Flipped(Decoupled(UInt(w.W)))
  val in2 = Flipped(Decoupled(UInt(w.W)))
  val out = Decoupled(UInt(w.W))
  val ctrl = Input(new ALUCTRL())
}
class ALU (val w: Int) extends Module{

  val io = IO(new ALUIO(w))
  val add :: sub :: mult :: comp :: Nil = Enum(4)
  val joinCtrl = Module(new JoinControl(2))


  when (io.ctrl.opcode === add) {
    io.out.bits := io.in1.bits + io.in2.bits
  } .elsewhen (io.ctrl.opcode === sub) {
    io.out.bits := io.in1.bits - io.in2.bits
  } .elsewhen (io.ctrl.opcode === mult) {
    io.out.bits := io.in1.bits * io.in2.bits
  } .elsewhen (io.ctrl.opcode === comp) {
    io.out.bits := (io.in1.bits > io.in2.bits).asUInt
  } .otherwise {
    io.out.bits := 0.U(w.W)
  }

  io.in1.ready := ~joinCtrl.io.stall_out_vector(0)
  io.in2.ready := ~joinCtrl.io.stall_out_vector(1)
  io.out.valid := joinCtrl.io.valid_out
  joinCtrl.io.valid_in_vector(0) := io.in1.valid
  joinCtrl.io.valid_in_vector(1) := io.in2.valid
  joinCtrl.io.stall_in := ~io.out.ready

}

object CU extends App {
  chisel3.Driver.execute(args, () => new ALU(32))
  chisel3.Driver.execute(args, () => new CU(4, 32))

}
