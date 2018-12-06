package overlay


import chisel3._
import chisel3.util._

class CBCTRL (val n : Int, val w : Int) extends Bundle {
  val north = Vec(n, UInt(2.W))
  val south = Vec(n, UInt(2.W))
  val west = UInt((log2Ceil(n * 2 + 1)).W)
  val east = UInt((log2Ceil(n * 2 + 1)).W)
}

class ConnectionBoxIO(val n : Int, val w : Int) extends Bundle {
  val chan_north_in = Input(Vec(n, UInt(w.W)))
  val chan_south_in = Input(Vec(n, UInt(w.W)))
  // val sel_north = Input(Vec(n, UInt(2.W)))
  // val sel_south = Input(Vec(n, UInt(2.W)))
  // val sel_west = Input(UInt((log2Ceil(n * 2 + 1)).W))
  // val sel_east = Input(UInt((log2Ceil(n * 2 + 1)).W))
  val west_in = Input(UInt(w.W))
  val east_in = Input(UInt(w.W))
  val chan_north_out = Output(Vec(n, UInt(w.W)))
  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val west_out = Output(UInt(w.W))
  val east_out = Output(UInt(w.W))
  val sel = Input(new CBCTRL(n, w))
}

class ConnectionBox (val n : Int, val w : Int) extends Module {
  // override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val io = IO(new ConnectionBoxIO(n, w))
  val mux_north = Seq.fill(n){ Module(new MuxN(3, w)) }
  val mux_south = Seq.fill(n){ Module(new MuxN(3, w)) }
  for (i <- 0 until n) {
    // val mux_north = Module(new MuxN(3, w))
    mux_north(i).io.ins(0) := io.chan_south_in(i)
    mux_north(i).io.ins(1) := io.east_in
    mux_north(i).io.ins(2) := io.west_in
    mux_north(i).io.sel := io.sel.north(i)
    io.chan_north_out(i) := mux_north(i).io.out


    // val mux_south = Module(new MuxN(3, w))
    mux_south(i).io.ins(0) := io.chan_north_in(i)
    mux_south(i).io.ins(1) := io.east_in
    mux_south(i).io.ins(2) := io.west_in
    mux_south(i).io.sel := io.sel.south(i)
    io.chan_south_out(i) := mux_south(i).io.out

  }
  val west_bound = Wire(Vec(2 * n + 1, UInt(w.W)))
  val east_bound = Wire(Vec(2 * n + 1, UInt(w.W)))
  for (i <- 0 until 2 * n) {
    if (i < n) {
      west_bound(i) := io.chan_north_in(i)
      east_bound(i) := io.chan_north_in(i)
    } else {
      west_bound(i) := io.chan_south_in(i - n)
      east_bound(i) := io.chan_south_in(i - n)
    }
  }
  west_bound(2 * n) := io.east_in
  east_bound(2 * n) := io.west_in
  io.west_out := west_bound(io.sel.west)
  io.east_out := east_bound(io.sel.east)

}
class DecoupledChannel(val num: Int, val wdth: Int) extends Bundle {

  val out = Vec(num, Decoupled(UInt(wdth.W)))
  val in = Flipped(Vec(num, Decoupled(UInt(wdth.W))))

  def <> (that: DecoupledChannel) = {
    that.in <> this.out
    that.out <> this.in
  }
  def <=> (that: DecoupledChannel) = {
    that.in <> this.in
    that.out <> this.out
  }

}
class DecoupledConnection(val num: Int, val wdth: Int) extends Bundle {
  val connection = Vec(num, Decoupled(UInt(wdth.W)))
}
class CBODCTRL (val chns: Int, val cncs: Int) extends Bundle {
  val cond = Vec(2 * chns, UInt(log2Ceil(cncs).W))
}
class CBIDCTRL (val chns: Int, val cncs: Int) extends Bundle {
  val cond = Vec(cncs, UInt(log2Ceil(2 * chns).W))
}
class CBODIO (val chns: Int, val cncs: Int, val wdth: Int) extends Bundle {

  val north = new DecoupledChannel(chns, wdth)
  val south = new DecoupledChannel(chns, wdth)
  val out = new DecoupledConnection(cncs, wdth)
  val ctrl = Input(new CBODCTRL(chns, cncs))
}
class CBIDIO (val chns: Int, val cncs: Int, val wdth: Int) extends Bundle {
  val west = new DecoupledChannel(chns, wdth)
  val east = new DecoupledChannel(chns, wdth)
  val in = Flipped(new DecoupledConnection(cncs, wdth))
  val ctrl = Input(new CBIDCTRL(chns, cncs))
}
class CBOD (val chns: Int, val cncs: Int, val wdth: Int) extends Module {

  val io = IO(new CBODIO(chns, cncs, wdth))

  val branch_list = Seq.fill(2 * chns) {
    Module( new BranchDecoupled(cncs + 1, wdth))
  }

  val branched_connections_list = Seq.fill (2 * chns) {
    Wire(Vec(cncs, DecoupledIO(UInt(wdth.W))))
  }
  for (j <- 0 until chns) {
    branch_list(j).io.input <> io.north.in(j)
    branch_list(j).io.conditional := io.ctrl.cond(j)

    branch_list(j + chns).io.input <> io.south.in(j)
    branch_list(j + chns).io.conditional := io.ctrl.cond(j + chns)

    io.south.out(j) <> branch_list(j).io.output_vector(0)

    io.north.out(j) <> branch_list(j + chns).io.output_vector(0)
    for (k <- 0 until cncs) {
      branched_connections_list(j)(k) <>
        branch_list(j).io.output_vector(k + 1)
      branched_connections_list(j + chns)(k) <>
        branch_list(j + chns).io.output_vector(k + 1)
    }
  }

  val merge_list = Seq.fill(cncs) {
    Module(new MergeDecoupled(2 * chns, wdth))
  }

  for (i <- 0 until cncs) {

    merge_list(i).io.output <> io.out.connection(i)
    for ( j <- 0 until chns) {
      merge_list(i).io.input_vector(j) <>
        branched_connections_list(j)(i)
      merge_list(i).io.input_vector(j + chns) <>
        branched_connections_list(j + chns)(i)
    }
  }
}

class CBID (val chns: Int, val cncs: Int, val wdth: Int) extends Module {
  val io = IO(new CBIDIO(chns, cncs, wdth))

  val branch_array = Seq.fill(cncs) {
    Module(new BranchDecoupled(2 * chns, wdth))
  }
  val branch_out = Seq.fill(cncs) {
    Wire(Vec(2 * chns, DecoupledIO(UInt(wdth.W))))
  }

  for (i <- 0 until cncs) {
    branch_array(i).io.input <> io.in.connection(i)
    branch_array(i).io.output_vector <> branch_out(i)
    branch_array(i).io.conditional := io.ctrl.cond(i)
  }

  val merge_list = Seq.fill(2 * chns) {
    Module(new MergeDecoupled(cncs + 1, wdth))
  }
  for (j <- 0 until chns) {
    merge_list(j).io.output <> io.west.out(j)
    merge_list(j + chns).io.output <> io.east.out(j)
    merge_list(j).io.input_vector(0) <> io.west.in(j)
    merge_list(j + chns).io.input_vector(0) <> io.east.in(j)
    for (i <- 0 until cncs) {
      merge_list(j).io.input_vector(i + 1) <> branch_out(i)(j)
      merge_list(j + chns).io.input_vector(i + 1) <> branch_out(i)(j + chns)
    }
  }
}
object ConnectionBox extends App {

  chisel3.Driver.execute(args, () => new ConnectionBox(2, 32))
  chisel3.Driver.execute(args, () => new CBOD(3, 2, 32))
  chisel3.Driver.execute(args, () => new CBID(16, 8, 32))
}
