package overlay


import chisel3._
import chisel3.util._


class SBDCTRL (val chns: Int) extends Bundle {
  val cond_north = Vec(chns, UInt(2.W))
  val cond_south = Vec(chns, UInt(2.W))
  val cond_east = Vec(chns, UInt(2.W))
  val cond_west = Vec(chns, UInt(2.W))
}
class SBDIO (val chns: Int, val wdth: Int) extends Bundle {
  val north = new DecoupledChannel(chns, wdth)
  val south = new DecoupledChannel(chns, wdth)
  val west = new DecoupledChannel(chns, wdth)
  val east = new DecoupledChannel(chns, wdth)
  val ctrl = Input(new SBDCTRL(chns))
}
class SBD (val chns: Int, val wdth: Int) extends Module {
  val io = IO(new SBDIO(chns, wdth))

  // Branch orders are NSWE
  val branch_list_north = Seq.fill(chns) {
    Module(new BranchDecoupled(3, wdth))
  }
  val branch_list_south = Seq.fill(chns) {
    Module(new BranchDecoupled(3, wdth))
  }
  val branch_list_west = Seq.fill(chns) {
    Module(new BranchDecoupled(3, wdth))
  }
  val branch_list_east = Seq.fill(chns) {
    Module(new BranchDecoupled(3, wdth))
  }

  val branch_out_north = Seq.fill(chns) {
     Wire(Vec(3, DecoupledIO(UInt(wdth.W))))
  }
  val branch_out_south = Seq.fill(chns) {
     Wire(Vec(3, DecoupledIO(UInt(wdth.W))))
  }
  val branch_out_west = Seq.fill(chns) {
     Wire(Vec(3, DecoupledIO(UInt(wdth.W))))
  }
  val branch_out_east = Seq.fill(chns) {
     Wire(Vec(3, DecoupledIO(UInt(wdth.W))))
  }

  val merge_list_north = Seq.fill(chns) {
    Module(new MergeDecoupled(3, wdth))
  }
  val merge_list_south = Seq.fill(chns) {
    Module(new MergeDecoupled(3, wdth))
  }
  val merge_list_west = Seq.fill(chns) {
    Module(new MergeDecoupled(3, wdth))
  }
  val merge_list_east = Seq.fill(chns) {
    Module(new MergeDecoupled(3, wdth))
  }

  for (i <- 0 until chns) {
    branch_out_north(i) <> branch_list_north(i).io.output_vector
    branch_out_south(i) <> branch_list_south(i).io.output_vector
    branch_out_west(i) <> branch_list_west(i).io.output_vector
    branch_out_east(i) <> branch_list_east(i).io.output_vector
    branch_list_north(i).io.conditional := io.ctrl.cond_north(i)
    branch_list_south(i).io.conditional := io.ctrl.cond_south(i)
    branch_list_west(i).io.conditional := io.ctrl.cond_west(i)
    branch_list_east(i).io.conditional := io.ctrl.cond_east(i)
    io.north.in(i) <> branch_list_north(i).io.input
    io.south.in(i) <> branch_list_south(i).io.input
    io.west.in(i) <> branch_list_west(i).io.input
    io.east.in(i) <> branch_list_east(i).io.input

    merge_list_north(i).io.output <> io.north.out(i)
    merge_list_south(i).io.output <> io.south.out(i)
    merge_list_west(i).io.output <> io.west.out(i)
    merge_list_east(i).io.output <> io.east.out(i)

    merge_list_north(i).io.input_vector(0) <>
      branch_out_south(i)(2)
    merge_list_north(i).io.input_vector(1) <>
      branch_out_west(i)(1)
    merge_list_north(i).io.input_vector(2) <>
      branch_out_east(i)(0)

    merge_list_south(i).io.input_vector(0) <>
      branch_out_north(i)(0)
    merge_list_south(i).io.input_vector(1) <>
      branch_out_west(i)(2)
    merge_list_south(i).io.input_vector(2) <>
      branch_out_east(i)(1)

    merge_list_west(i).io.input_vector(0) <>
      branch_out_north(i)(1)
    merge_list_west(i).io.input_vector(1) <>
      branch_out_south(i)(0)
    merge_list_west(i).io.input_vector(2) <>
      branch_out_east(i)(2)

    merge_list_east(i).io.input_vector(0) <>
      branch_out_north(i)(2)
    merge_list_east(i).io.input_vector(1) <>
      branch_out_south(i)(1)
    merge_list_east(i).io.input_vector(2) <>
      branch_out_west(i)(0)

  }

}

class SBD3CTRL(val chns: Int) extends Bundle {
  val cond_dir0 = Vec(chns, UInt(1.W))
  val cond_dir1 = Vec(chns, UInt(1.W))
  val cond_dir2 = Vec(chns, UInt(1.W))
}
class SBD3IO(val chns: Int, val wdth: Int) extends Bundle {
  val dir0 = new DecoupledChannel(chns, wdth)
  val dir1 = new DecoupledChannel(chns, wdth)
  val dir2 = new DecoupledChannel(chns, wdth)
  val ctrl = Input(new SBD3CTRL(chns))
}
class SBD3(val chns: Int, val wdth: Int) extends Module {
  val io = IO(new SBD3IO(chns, wdth))
  def makeBranchList () = Seq.fill(chns) {
    Module (new BranchDecoupled(2, wdth))
  }
  def makeMergeList () = Seq.fill(chns) {
    Module (new MergeDecoupled(2, wdth))
  }

  val branch_list_dir0 = makeBranchList()
  val branch_list_dir1 = makeBranchList()
  val branch_list_dir2 = makeBranchList()
  val merge_list_dir0 = makeMergeList()
  val merge_list_dir1 = makeMergeList()
  val merge_list_dir2 = makeMergeList()

  for(i <- 0 until chns) {
    branch_list_dir0(i).io.input <> io.dir0.in(i)
    branch_list_dir1(i).io.input <> io.dir1.in(i)
    branch_list_dir2(i).io.input <> io.dir2.in(i)


    branch_list_dir0(i).io.conditional := io.ctrl.cond_dir0(i)
    branch_list_dir1(i).io.conditional := io.ctrl.cond_dir1(i)
    branch_list_dir2(i).io.conditional := io.ctrl.cond_dir2(i)

    merge_list_dir0(i).io.output <> io.dir0.out(i)
    merge_list_dir1(i).io.output <> io.dir1.out(i)
    merge_list_dir2(i).io.output <> io.dir2.out(i)

    merge_list_dir0(i).io.input_vector(0) <>
      branch_list_dir1(i).io.output_vector(1)
    merge_list_dir0(i).io.input_vector(1) <>
      branch_list_dir2(i).io.output_vector(0)

    merge_list_dir1(i).io.input_vector(0) <>
      branch_list_dir0(i).io.output_vector(0)
    merge_list_dir1(i).io.input_vector(1) <>
      branch_list_dir2(i).io.output_vector(1)

    merge_list_dir2(i).io.input_vector(0) <>
      branch_list_dir0(i).io.output_vector(1)
    merge_list_dir2(i).io.input_vector(1) <>
      branch_list_dir1(i).io.output_vector(0)

  }
}


class NSBCTRL(val n : Int, val w : Int) extends Bundle {

  val south = Vec(n, UInt(1.W))
  val west = Vec(n, UInt(1.W))
  val east = Vec(n, UInt(1.W))
}
class NSwitchBoxIO(val n : Int, val w : Int) extends Bundle {

  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_west_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))

  val sel = Input(new NSBCTRL(n, w))

  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_west_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}

class SSBCTRL(val n : Int, val w : Int) extends Bundle {

  val north = Vec(n, UInt(1.W))
  val west = Vec(n, UInt(1.W))
  val east = Vec(n, UInt(1.W))
}
class SSwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))

  val chan_west_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))

  val sel = Input(new SSBCTRL(n, w))

  val chan_north_out = Output(Vec(n, UInt(w.W)))

  val chan_west_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}

class WSBCTRL(val n : Int, val w : Int) extends Bundle {

  val north = Vec(n, UInt(1.W))
  val south = Vec(n, UInt(1.W))
  val east = Vec(n, UInt(1.W))
}
class WSwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))

  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))

  val sel = Input(new WSBCTRL(n, w))

  val chan_north_out = Output(Vec(n, UInt(w.W)))

  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}

class ESBCTRL(val n : Int, val w : Int) extends Bundle {

  val north = Vec(n, UInt(1.W))
  val south = Vec(n, UInt(1.W))
  val west = Vec(n, UInt(1.W))
}
class ESwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))
  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_west_in = Input(Vec(n, UInt(w.W)))

  val sel = Input(new ESBCTRL(n, w))

  val chan_north_out = Output(Vec(n, UInt(w.W)))
  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_west_out = Output(Vec(n, UInt(w.W)))

}

class SBCTRL(val n : Int, val w : Int) extends Bundle {

  val north = Vec(n, UInt(2.W))
  val south = Vec(n, UInt(2.W))
  val east = Vec(n, UInt(2.W))
  val west = Vec(n, UInt(2.W))
}
class SwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))
  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_west_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))

  val sel = Input(new SBCTRL(n, w))

  val chan_north_out = Output(Vec(n, UInt(w.W)))
  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_west_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}

class SwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new SwitchBoxIO(n, w))


  for (i <- 0 until n) {
    val mux_north = Module(new MuxN(3, w))
    val mux_south = Module(new MuxN(3, w))
    val mux_west = Module(new MuxN(3, w))
    val mux_east = Module(new MuxN(3, w))

    mux_north.io.ins(0) := io.chan_south_in(i)
    mux_north.io.ins(1) := io.chan_west_in(i)
    mux_north.io.ins(2) := io.chan_east_in(i)
    mux_north.io.sel := io.sel.north(i)
    io.chan_north_out(i) := mux_north.io.out

    mux_south.io.ins(0) := io.chan_west_in(i)
    mux_south.io.ins(1) := io.chan_east_in(i)
    mux_south.io.ins(2) := io.chan_north_in(i)
    mux_south.io.sel := io.sel.south(i)
    io.chan_south_out(i) := mux_south.io.out

    mux_west.io.ins(0) := io.chan_east_in(i)
    mux_west.io.ins(1) := io.chan_north_in(i)
    mux_west.io.ins(2) := io.chan_south_in(i)
    mux_west.io.sel := io.sel.west(i)
    io.chan_west_out(i) := mux_west.io.out

    mux_east.io.ins(0) := io.chan_north_in(i)
    mux_east.io.ins(1) := io.chan_south_in(i)
    mux_east.io.ins(2) := io.chan_west_in(i)
    mux_east.io.sel := io.sel.east(i)
    io.chan_east_out(i) := mux_east.io.out

  }
}

class NSwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new NSwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_south_out(i) :=
      Mux(io.sel.south(i).toBool, io.chan_west_in(i), io.chan_east_in(i))

    io.chan_west_out(i) :=
      Mux(io.sel.west(i).toBool, io.chan_east_in(i), io.chan_south_in(i))

    io.chan_east_out(i) :=
      Mux(io.sel.east(i).toBool, io.chan_south_in(i), io.chan_west_in(i))
  }
}

class SSwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new SSwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_north_out(i) :=
      Mux(io.sel.north(i).toBool, io.chan_west_in(i), io.chan_east_in(i))

    io.chan_west_out(i) :=
      Mux(io.sel.west(i).toBool, io.chan_east_in(i), io.chan_north_in(i))

    io.chan_east_out(i) :=
      Mux(io.sel.east(i).toBool, io.chan_north_in(i), io.chan_west_in(i))
  }
}

class WSwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new WSwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_north_out(i) :=
      Mux(io.sel.north(i).toBool, io.chan_south_in(i), io.chan_east_in(i))

    io.chan_south_out(i) :=
      Mux(io.sel.south(i).toBool, io.chan_east_in(i), io.chan_north_in(i))

    io.chan_east_out(i) :=
      Mux(io.sel.east(i).toBool, io.chan_north_in(i), io.chan_south_in(i))
  }
}
class ESwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new ESwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_north_out(i) :=
      Mux(io.sel.north(i).toBool, io.chan_south_in(i), io.chan_west_in(i))

    io.chan_south_out(i) :=
      Mux(io.sel.south(i).toBool, io.chan_west_in(i), io.chan_north_in(i))

    io.chan_west_out(i) :=
      Mux(io.sel.west(i).toBool, io.chan_north_in(i), io.chan_south_in(i))
  }
}
object SwitchBox extends App {

  chisel3.Driver.execute(args, () => new SwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new NSwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new SSwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new WSwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new ESwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new SBD(4, 32))
  chisel3.Driver.execute(args, () => new SBD3(2, 8))
}
