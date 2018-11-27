package overlay


import chisel3._
import chisel3.util._

class SwitchBoxIO (val n : Int, val w : Int) extends Bundle {
  val chan_north_in = Input(Vec(n, UInt(w.W)))
  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_west_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))
  val sel_north = Input(Vec(n, UInt(2.W)))
  val sel_south = Input(Vec(n, UInt(2.W)))
  val sel_west = Input(Vec(n, UInt(2.W)))
  val sel_east = Input(Vec(n, UInt(2.W)))
  val chan_north_out = Output(Vec(n, UInt(w.W)))
  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_west_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}
class SwitchBox (val n : Int, val w : Int) extends Module {
  //override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val io = IO(new SwitchBoxIO(n, w))


  for (i <- 0 until n) {
    val mux_north = Module(new MuxN(3, w))
    val mux_south = Module(new MuxN(3, w))
    val mux_west = Module(new MuxN(3, w))
    val mux_east = Module(new MuxN(3, w))

    mux_north.io.ins(0) := io.chan_south_in(i)
    mux_north.io.ins(1) := io.chan_west_in(i)
    mux_north.io.ins(2) := io.chan_east_in(i)
    mux_north.io.sel := io.sel_north(i)
    io.chan_north_out(i) := mux_north.io.out

    mux_south.io.ins(0) := io.chan_west_in(i)
    mux_south.io.ins(1) := io.chan_east_in(i)
    mux_south.io.ins(2) := io.chan_north_in(i)
    mux_south.io.sel := io.sel_south(i)
    io.chan_south_out(i) := mux_south.io.out

    mux_west.io.ins(0) := io.chan_east_in(i)
    mux_west.io.ins(1) := io.chan_north_in(i)
    mux_west.io.ins(2) := io.chan_south_in(i)
    mux_west.io.sel := io.sel_west(i)
    io.chan_west_out(i) := mux_west.io.out

    mux_east.io.ins(0) := io.chan_north_in(i)
    mux_east.io.ins(1) := io.chan_south_in(i)
    mux_east.io.ins(2) := io.chan_west_in(i)
    mux_east.io.sel := io.sel_east(i)
    io.chan_east_out(i) := mux_east.io.out

  }
}

object SwitchBox extends App {

  chisel3.Driver.execute(args, () => new SwitchBox(2, 32))
}
