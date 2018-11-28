package overlay


import chisel3._
import chisel3.util._

// class SwitchBoxIO (val n : Int, val w : Int, val ways : Int = 4)
// extends Bundle {
//   val chan_in_vec = Input(Vec(ways, Vec(n, UInt(w.W))))
//   val sel_vec = Input(Vec(ways, Vec(n, UInt(log2Ceil(ways - 1).W))))
//   val chan_out_vec = Output(Vec(ways, Vec(n, UInt(w.W))))
// }
//
// class SwitchBox (val n : Int, val w : Int, val ways : Int = 4) extends Module {
//   val io = IO(new SwitchBoxIO(n, w, ways))
//
//   val mux_list = Seq.fill(n * ways){ Module(new MuxN(ways - 1, w)) }
//   for (i <- 0 until n) {
//     for (j <- 0 until ways) {
//       mux_list(i * ways + j).io.ins :=
//     }
//   }
//
// }
class NSwitchBoxIO(val n : Int, val w : Int) extends Bundle {

  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_west_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))

  val sel_south = Input(Vec(n, UInt(1.W)))
  val sel_west = Input(Vec(n, UInt(1.W)))
  val sel_east = Input(Vec(n, UInt(1.W)))

  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_west_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}
class SSwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))

  val chan_west_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))
  val sel_north = Input(Vec(n, UInt(1.W)))

  val sel_west = Input(Vec(n, UInt(1.W)))
  val sel_east = Input(Vec(n, UInt(1.W)))
  val chan_north_out = Output(Vec(n, UInt(w.W)))

  val chan_west_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}
class WSwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))

  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_east_in = Input(Vec(n, UInt(w.W)))
  val sel_north = Input(Vec(n, UInt(1.W)))

  val sel_south = Input(Vec(n, UInt(1.W)))
  val sel_east = Input(Vec(n, UInt(1.W)))
  val chan_north_out = Output(Vec(n, UInt(w.W)))

  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_east_out = Output(Vec(n, UInt(w.W)))
}
class ESwitchBoxIO (val n : Int, val w : Int) extends Bundle {

  val chan_north_in = Input(Vec(n, UInt(w.W)))
  val chan_south_in = Input(Vec(n, UInt(w.W)))
  val chan_west_in = Input(Vec(n, UInt(w.W)))

  val sel_north = Input(Vec(n, UInt(1.W)))
  val sel_south = Input(Vec(n, UInt(1.W)))
  val sel_west = Input(Vec(n, UInt(1.W)))

  val chan_north_out = Output(Vec(n, UInt(w.W)))
  val chan_south_out = Output(Vec(n, UInt(w.W)))
  val chan_west_out = Output(Vec(n, UInt(w.W)))

}
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

class NSwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new NSwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_south_out(i) :=
      Mux(io.sel_south(i).toBool, io.chan_west_in(i), io.chan_east_in(i))

    io.chan_west_out(i) :=
      Mux(io.sel_west(i).toBool, io.chan_east_in(i), io.chan_south_in(i))

    io.chan_east_out(i) :=
      Mux(io.sel_east(i).toBool, io.chan_south_in(i), io.chan_west_in(i))
  }
}

class SSwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new SSwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_north_out(i) :=
      Mux(io.sel_north(i).toBool, io.chan_west_in(i), io.chan_east_in(i))

    io.chan_west_out(i) :=
      Mux(io.sel_west(i).toBool, io.chan_east_in(i), io.chan_north_in(i))

    io.chan_east_out(i) :=
      Mux(io.sel_east(i).toBool, io.chan_north_in(i), io.chan_west_in(i))
  }
}

class WSwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new WSwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_north_out(i) :=
      Mux(io.sel_north(i).toBool, io.chan_south_in(i), io.chan_east_in(i))

    io.chan_south_out(i) :=
      Mux(io.sel_south(i).toBool, io.chan_east_in(i), io.chan_north_in(i))

    io.chan_east_out(i) :=
      Mux(io.sel_east(i).toBool, io.chan_north_in(i), io.chan_south_in(i))
  }
}
class ESwitchBox (val n : Int, val w : Int) extends Module {

  val io = IO(new ESwitchBoxIO(n, w))

  for (i <- 0 until n) {

    io.chan_north_out(i) :=
      Mux(io.sel_north(i).toBool, io.chan_south_in(i), io.chan_west_in(i))

    io.chan_south_out(i) :=
      Mux(io.sel_south(i).toBool, io.chan_west_in(i), io.chan_north_in(i))

    io.chan_west_out(i) :=
      Mux(io.sel_west(i).toBool, io.chan_north_in(i), io.chan_south_in(i))
  }
}
object SwitchBox extends App {

  chisel3.Driver.execute(args, () => new SwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new NSwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new SSwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new WSwitchBox(2, 32))
  chisel3.Driver.execute(args, () => new ESwitchBox(2, 32))
}
