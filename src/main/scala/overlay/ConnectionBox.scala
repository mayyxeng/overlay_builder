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


object ConnectionBox extends App {

  chisel3.Driver.execute(args, () => new ConnectionBox(2, 32))
}
