// package overlay
//
//
// import chisel3._
// import chisel3.util._
//
// class BlockDecoupledGeneric(val chns: Int, val cncs: Int,
//   val wdth: Int) extends Module {
//
//   class CTRL extends Bundle{
//     val cbo = new CBODCTRL(chns, cncs)
//     val cbi = new CBIDCTRL(chns, cncs)
//   }
//   class InOut extends Bundle{
//     val north = new DecoupledChannel(chns, wdth)
//     val east = new DecoupledChannel(chns, wdth)
//     val ctrl = Input(new CTRL())
//   }
//   val io = IO(new InOut(){
//     val cbosb = new DecoupledChannel(chns, wdth)
//     val cbisb = new DecoupledChannel(chns, wdth)
//   })
//
//   val connection_box_in = Module(new CBID(chns, cncs, wdth))
//   val connection_box_out = Module(new CBOD(chns, cncs, wdth))
//   val compute_unit = Module(new CU(cncs, wdth))
//
//   connection_box_out.io.north <=> io.north
//   connection_box_out.io.out <> compute_unit.io.input
//   connection_box_out.io.south <=> io.cbosb
//   connection_box_out.io.ctrl := io.ctrl.cbo
//
//   connection_box_in.io.in <> compute_unit.io.output
//   connection_box_in.io.west <=> io.cbisb
//   connection_box_in.io.east <=> io.east
//   connection_box_in.io.ctrl := io.ctrl.cbi
//
// }
//
//
// class BlockDecoupled(val chns: Int = 4, val cncs: Int = 4,
//   val wdth: Int = 32) extends Module {
//
//   class CTRL extends BlockDecoupledGeneric#CTRL {
//     val sb = new SBDCTRL(chns)
//   }
//
//   class InOut extends BlockDecoupledGeneric#InOut{
//     val south = new DecoupledChannel(chns, wdth)
//     val west = new DecoupledChannel(chns, wdth)
//     override val ctrl = Input(new CTRL())
//   }
//
//   val io = IO(new InOut())
//
//   val generic_block = Module(new BlockDecoupledGeneric(chns, cncs, wdth))
//   val switch_box = Module(new SBD(chns, wdth))
//
//   generic_block.io.north <=> io.north
//   generic_block.io.east <=> io.east
//   generic_block.io.ctrl.cbo := io.ctrl.cbo
//   generic_block.io.ctrl.cbi := io.ctrl.cbi
//   generic_block.io.cbosb <> switch_box.io.north
//   generic_block.io.cbisb <> switch_box.io.east
//   io.west <=> switch_box.io.west
//   io.south <=> switch_box.io.south
//   switch_box.io.ctrl := io.ctrl.sb
//
// }
// object Block extends App {
//
//   chisel3.Driver.execute(args, () => new BlockDecoupledGeneric(4, 4, 32))
//   chisel3.Driver.execute(args, () => new BlockDecoupled(4, 4, 32))
//
// }
