package overlay


import chisel3._
import chisel3.util.log2Ceil
class IOPads (val m : Int, val n : Int, val w : Int = 32) extends Bundle {

  val input_north = Input(Vec(n, UInt(w.W)))
  val input_south = Input(Vec(n, UInt(w.W)))
  val input_west = Input(Vec(n, UInt(w.W)))
  val input_east = Input(Vec(n, UInt(w.W)))
  val output_north = Output(Vec(n, UInt(w.W)))
  val output_south = Output(Vec(n, UInt(w.W)))
  val output_west = Output(Vec(n, UInt(w.W)))
  val output_east = Output(Vec(n, UInt(w.W)))
}
class BDCTRL(val chns: Int, val cncs: Int) extends Bundle {
  val cbo = new CBODCTRL(chns, cncs)
  val cbi = new CBIDCTRL(chns, cncs)
  val sb = new SBDCTRL(chns)
}
class BlockDecoupledIO(val chns: Int, val cncs: Int,
  val wdth: Int) extends Bundle {
  val north = new DecoupledChannel(chns, wdth)
  val south = new DecoupledChannel(chns, wdth)
  val west = new DecoupledChannel(chns, wdth)
  val east = new DecoupledChannel(chns, wdth)
  val ctrl = Input(new BDCTRL(chns, cncs))
}
class BlockDecoupled(val chns: Int = 4, val cncs: Int = 4,
  val wdth: Int = 32) extends Module {

  val io = IO(new BlockDecoupledIO(chns, cncs, wdth))

  val connection_box_in = Module(new CBID(chns, cncs, wdth))
  val connection_box_out = Module(new CBOD(chns, cncs, wdth))
  val switch_box = Module(new SBD(chns, wdth))
  val compute_unit = Module(new CU(cncs, wdth))

  connection_box_out.io.out <> compute_unit.io.input
  connection_box_out.io.south.out <> switch_box.io.north.in
  connection_box_out.io.south.in <> switch_box.io.north.out

  connection_box_in.io.in <> compute_unit.io.output

  connection_box_in.io.west <> switch_box.io.east

  io.north <=> connection_box_out.io.north
  io.south <=> switch_box.io.south
  io.west <=> switch_box.io.west
  io.east <=> connection_box_in.io.east


  connection_box_in.io.ctrl := io.ctrl.cbi
  connection_box_out.io.ctrl := io.ctrl.cbo
  switch_box.io.ctrl := io.ctrl.sb


}
class ODIO (val rws: Int, val cls: Int,
  val wdth: Int, val chns: Int, val cncs: Int) extends Bundle {
    val north = Vec(cls, new DecoupledChannel(chns, wdth))
    val south = Vec(cls, new DecoupledChannel(chns, wdth))
    val west = Vec(cls, new DecoupledChannel(chns, wdth))
    val east = Vec(cls, new DecoupledChannel(chns, wdth))
    val ctrl = Input(Vec(rws * cls, new BDCTRL(chns, cncs)))
  }
class OverlayDecoupled(val rws:Int, val cls: Int, val wdth: Int = 32,
  val chns: Int = 4, val cncs: Int = 4) extends Module {

  val io = IO(new ODIO(rws, cls, wdth, chns, cncs))
  val blocks = Seq.fill(rws * cls) {
    Module(new BlockDecoupled(chns, cncs, wdth))
  }

  for (i <- 0 until rws) {
    for (j <- 0 until cls) {
      val idx = i * cls + j
      blocks(idx).io.ctrl := io.ctrl(idx)
      if (i == 0) {
        blocks(idx).io.north <=> io.north(j)
      } else {
        blocks(idx).io.north <> blocks(idx - cls).io.south
      }
      if (i == rws - 1) {
        blocks(idx).io.south <=> io.south(j)
      } else {
        blocks(idx).io.south <> blocks(idx + cls).io.north
      }
      if (j == 0) {
        blocks(idx).io.west <=> io.west(i)
      } else {
        blocks(idx).io.west <> blocks(idx - 1).io.east
      }
      if (j == cls - 1) {
        blocks(idx).io.east <=> io.east(i)
      } else {
        blocks(idx).io.east <> blocks(idx + 1).io.west
      }

    }
  }
}
class  Overlay (val m : Int, val n : Int, val w : Int = 32, val chns : Int = 4)
extends Module {

  val io = IO(new IOPads(m, n, w) {
    val cb_list_top_ctrl = Input(Vec(n, new CBCTRL(chns, w)))
    val cb_list_bottom_ctrl = Input(Vec(n, new CBCTRL(chns, w)))
    val cb_list_left_ctrl = Input(Vec(n, new CBCTRL(chns, w)))
    val cb_list_right_ctrl = Input(Vec(n, new CBCTRL(chns, w)))
    val cb_list_even_ctrl = Input(Vec((m - 1) * n, new CBCTRL(chns, w)))
    val cb_list_odd_ctrl = Input(Vec(m * (n - 1), new CBCTRL(chns, w)))
    val sb_list_top_ctrl = Input(Vec(n - 1, new NSBCTRL(chns, w)))
    val sb_list_bottom_ctrl = Input(Vec(n - 1, new SSBCTRL(chns, w)))
    val sb_list_left_ctrl = Input(Vec(m - 1, new WSBCTRL(chns, w)))
    val sb_list_right_ctrl = Input(Vec(m - 1, new ESBCTRL(chns, w)))
    val sb_list_even_ctrl = Input(Vec((m - 1) * (n - 1), new SBCTRL(chns, w)))
  })
  /*

    An overlay with m rows and n columns will actually
    have 2m + 1 rows and 2n + 1 columns because we have to
    fit CBs and SBs
    The patterns is as follows:
      c s c s c s c s c s ... c    : c (s c) * (n - 1)
    c p c p c p c p c p c ... p c  : (c p)*n c
    s c s c s c s c s c s ... c s  : (s c)*n s
    .                     ... . .
    c p c p c p c p c p c ... p c  : (c p)*n c
      c s c s c s c s c s ... c    : c (s c) * (n - 1)

    where c = ConnectionBox, s = SwitchBox and P = ProcessingElement
    in total we have :
      (n + 1) * (m + 1) - 4 = mn + m + n - 3 SwitchBoxes
      (n) * (m + 1) + (n + 1) * m = 2mn + n + m ConnectionBoxes
      mn ProcessingElements

      in ODD rows:
      m * n : PE
      m * (n + 1) : CB

      in EVEN rows:
      (m + 1) * n : CB
      (m + 1) * (n + 1) - 4: SB

  */

  /*
    There are 6 lists for CBs, namely the 4 list for CBs at overlay border
    and 2 for CBs in the middle. The CBs in even rows are turned 90 degrees
    cc-wise.
    Also there are 5 lists for SBs. Four lists are for borders and 1 fills
    in the middle.
    We have only one list for PEs.
  */

  val zero_vec_sel_cb = Seq.fill(chns) { 0.U }
  val pe_list = Seq.fill(m * n) {
    Module(new ProcessingElement(w))
  }

  val cb_list_top = Seq.fill(n) {
    Module(new ConnectionBox(chns ,w))
  }
  val cb_list_bottom = Seq.fill(n) {
    Module(new ConnectionBox(chns, w))
  }
  val cb_list_left = Seq.fill(m) {
    Module(new ConnectionBox(chns, w))
  }
  val cb_list_right = Seq.fill(m) {
    Module(new ConnectionBox(chns, w))
  }
  val cb_list_even = Seq.fill((m - 1) * n) {
    Module(new ConnectionBox(chns, w))
  }
  val cb_list_odd = Seq.fill(m * (n - 1)) {
    Module(new ConnectionBox(chns, w))
  }

  val sb_list_top = Seq.fill(n - 1) {
    Module(new NSwitchBox(chns, w))
  }
  val sb_list_bottom = Seq.fill(n - 1) {
    Module(new SSwitchBox(chns, w))
  }
  val sb_list_left = Seq.fill(m - 1) {
    Module(new WSwitchBox(chns, w))
  }
  val sb_list_right = Seq.fill(m - 1) {
    Module(new ESwitchBox(chns, w))
  }
  val sb_list_even = Seq.fill((m - 1) * (n - 1)) {
    Module(new SwitchBox(chns, w))
  }
  // This part connects inputs of PEs
  for (i <- 0 until m) {
    for (j <- 0 until n) {
      val pe_idx = i * n + j
      val cb_odd_idx = i * (n - 1) + j
      val cb_even_idx = i * n + j

      if (j == 0) {
        pe_list(pe_idx).io.west_in := cb_list_left(i).io.east_out
      } else {
        pe_list(pe_idx).io.west_in := cb_list_odd(cb_odd_idx - 1).io.east_out
      }
      if (j == n - 1) {
        pe_list(pe_idx).io.east_in := cb_list_right(i).io.west_out
      } else {
        pe_list(pe_idx).io.east_in := cb_list_odd(cb_odd_idx).io.west_out
      }
      if (i == 0) {
        pe_list(pe_idx).io.north_in := cb_list_top(j).io.west_out
      } else {
        pe_list(pe_idx).io.north_in :=
          cb_list_even(cb_even_idx - n).io.west_out
      }
      if (i == m - 1) {
        pe_list(pe_idx).io.south_in := cb_list_bottom(j).io.east_out
      } else {
        pe_list(pe_idx).io.south_in := cb_list_even(cb_even_idx).io.east_out
      }

    }
  }

  // This part connects inputs of CBs in odd rows
  for (i <- 0 until m) {
    for (j <- 0 until n - 1) {
      val cb_odd_idx = i * (n - 1) + j
      val sb_idx = i * (n - 1) + j
      val pe_idx = i * n + j
      if (i == 0) {
        cb_list_odd(cb_odd_idx).io.chan_north_in :=
          sb_list_top(j).io.chan_south_out
      } else {
        cb_list_odd(cb_odd_idx).io.chan_north_in :=
          sb_list_even(sb_idx - n + 1).io.chan_south_out
      }
      if (i == m - 1) {
        cb_list_odd(cb_odd_idx).io.chan_south_in :=
          sb_list_bottom(j).io.chan_north_out
      } else {
        cb_list_odd(cb_odd_idx).io.chan_south_in :=
          sb_list_even(sb_idx).io.chan_north_out
      }
      cb_list_odd(cb_odd_idx).io.west_in := pe_list(pe_idx).io.east_out
      cb_list_odd(cb_odd_idx).io.east_in := pe_list(pe_idx + 1).io.west_out
      cb_list_odd(cb_odd_idx).io.sel := io.cb_list_odd_ctrl(cb_odd_idx)

    }
  }

  // This part connects inputs of CBs in even rows.
  for (i <- 0 until m - 1) {
    for (j <- 0 until n) {
      val cb_even_idx = i * n + j
      val pe_idx = i * n + j
      val sb_even_idx = i * (n - 1) + j
      if (j == 0) {
        cb_list_even(cb_even_idx).io.chan_north_in :=
          sb_list_left(i).io.chan_east_out
      } else {
        cb_list_even(cb_even_idx).io.chan_north_in :=
          sb_list_even(sb_even_idx - 1).io.chan_east_out
      }
      if (j == n - 1) {
        cb_list_even(cb_even_idx).io.chan_south_in :=
          sb_list_right(i).io.chan_west_out
      } else {
        cb_list_even(cb_even_idx).io.chan_south_in :=
          sb_list_even(sb_even_idx).io.chan_west_in
      }
      cb_list_even(cb_even_idx).io.east_in := pe_list(pe_idx).io.south_out
      cb_list_even(cb_even_idx).io.west_in := pe_list(pe_idx + n).io.north_out
      cb_list_even(cb_even_idx).io.sel := io.cb_list_even_ctrl(cb_even_idx)


    }
  }
  // This part connects left and right border CBs
  for (i <- 0 until m) {
    val cb_left_idx = i
    val cb_right_idx = i
    val sb_right_idx = i
    val sb_left_idx = i
    val pe_left_idx = i * n
    val pe_right_idx = (i + 1) * n - 1
    if ( i == 0) {
      cb_list_left(cb_left_idx).io.chan_north_in :=
        cb_list_top(0).io.chan_north_out
      cb_list_right(cb_right_idx).io.chan_north_in :=
        cb_list_top(n - 1).io.chan_south_out
    } else {
      cb_list_left(cb_left_idx).io.chan_north_in :=
        sb_list_left(sb_left_idx - 1).io.chan_south_out
      cb_list_right(cb_right_idx).io.chan_north_in :=
        sb_list_right(sb_right_idx - 1).io.chan_south_out
    }
    if (i == m - 1) {
      cb_list_left(cb_left_idx).io.chan_south_in :=
        cb_list_bottom(0).io.chan_north_out
      cb_list_right(cb_right_idx).io.chan_south_in :=
        cb_list_bottom(n - 1).io.chan_south_out
    } else {
      cb_list_left(cb_left_idx).io.chan_south_in :=
        sb_list_left(sb_left_idx).io.chan_north_out
      cb_list_right(cb_right_idx).io.chan_south_in :=
        sb_list_right(sb_right_idx).io.chan_north_out
    }
    cb_list_left(cb_left_idx).io.east_in := pe_list(pe_left_idx).io.west_out
    cb_list_left(cb_left_idx).io.west_in := io.input_west(i)
    cb_list_right(cb_right_idx).io.west_in := pe_list(pe_right_idx).io.east_out
    cb_list_right(cb_right_idx).io.east_in := io.input_east(i)

    cb_list_left(cb_left_idx).io.sel := io.cb_list_left_ctrl(cb_left_idx)
    cb_list_right(cb_right_idx).io.sel := io.cb_list_right_ctrl(cb_right_idx)

    io.output_west(i) := cb_list_left(cb_left_idx).io.west_out
    io.output_east(i) := cb_list_right(cb_right_idx).io.east_out

  }

  // This part takes care of top and bottom border CBs
  for (j <- 0 until n) {
    val cb_top_idx = j
    val cb_bottom_idx = j
    val sb_top_idx = j
    val sb_bottom_idx = j
    val pe_top_idx = j
    val pe_bottom_idx = (m - 1) * n + j

    if (j == 0) {
      cb_list_top(cb_top_idx).io.chan_north_in :=
        cb_list_left(0).io.chan_north_out
      cb_list_bottom(cb_bottom_idx).io.chan_north_in :=
        cb_list_left(m - 1).io.chan_south_out
    } else {
      cb_list_top(cb_top_idx).io.chan_north_in :=
        sb_list_top(sb_top_idx - 1).io.chan_east_out
      cb_list_bottom(cb_bottom_idx).io.chan_north_in :=
        sb_list_bottom(sb_bottom_idx - 1).io.chan_east_out
    }
    if (j == n - 1) {
      cb_list_top(cb_top_idx).io.chan_south_in :=
        cb_list_right(0).io.chan_north_out
      cb_list_bottom(cb_bottom_idx).io.chan_south_in :=
        cb_list_right(m - 1).io.chan_south_out
    } else {
      cb_list_top(cb_top_idx).io.chan_south_in :=
        sb_list_top(sb_top_idx).io.chan_west_out
      cb_list_bottom(cb_bottom_idx).io.chan_south_in :=
        sb_list_bottom(cb_bottom_idx).io.chan_west_out
    }
    cb_list_top(cb_top_idx).io.east_in := io.input_north(j)
    cb_list_bottom(cb_bottom_idx).io.west_in := io.input_south(j)

    cb_list_top(cb_top_idx).io.west_in := pe_list(pe_top_idx).io.north_out
    cb_list_bottom(cb_bottom_idx).io.east_in :=
      pe_list(pe_bottom_idx).io.south_out

    cb_list_top(cb_top_idx).io.sel :=
      io.cb_list_top_ctrl(cb_top_idx)
    cb_list_bottom(cb_bottom_idx).io.sel :=
      io.cb_list_bottom_ctrl(cb_bottom_idx)

    io.output_north(j) := cb_list_top(cb_top_idx).io.east_out
    io.output_south(j) := cb_list_bottom(cb_bottom_idx).io.west_out
  }

  // This part connects even rows sb inputs
  for (i <- 0 until m - 1) {
    for (j <- 0 until n - 1) {
      val sb_idx = i * (n - 1) + j
      val cb_even_idx =  i * n + j
      val cb_odd_idx = i * (n - 1) + j
      sb_list_even(sb_idx).io.chan_west_in :=
        cb_list_even(cb_even_idx).io.chan_south_out
      sb_list_even(sb_idx).io.chan_east_in :=
        cb_list_even(cb_even_idx + 1).io.chan_north_out
      sb_list_even(sb_idx).io.chan_north_in :=
        cb_list_odd(cb_odd_idx).io.chan_south_out
      sb_list_even(sb_idx).io.chan_south_in :=
        cb_list_odd(cb_odd_idx + n - 1).io.chan_north_out
      sb_list_even(sb_idx).io.sel :=
        io.sb_list_even_ctrl(sb_idx)
    }
  }

  // This part connects top and bottom SBs
  for (j <- 0 until n - 1) {
    val sb_top_idx = j
    val sb_bottom_idx = j
    val cb_top_idx = j
    val cb_bottom_idx = (m - 1) * (n - 1) + j

    sb_list_top(sb_top_idx).io.chan_west_in :=
      cb_list_top(cb_top_idx).io.chan_south_out
    sb_list_bottom(sb_bottom_idx).io.chan_west_in :=
      cb_list_bottom(cb_top_idx).io.chan_south_out

    sb_list_top(sb_top_idx).io.chan_east_in :=
      cb_list_top(cb_top_idx + 1).io.chan_north_out
    sb_list_bottom(sb_bottom_idx).io.chan_east_in :=
      cb_list_bottom(cb_top_idx + 1).io.chan_north_out

    sb_list_top(sb_top_idx).io.chan_south_in :=
      cb_list_odd(cb_top_idx).io.chan_north_out
    sb_list_bottom(sb_bottom_idx).io.chan_north_in :=
      cb_list_odd(cb_bottom_idx).io.chan_south_out

    sb_list_top(sb_top_idx).io.sel :=
      io.sb_list_top_ctrl(sb_top_idx)
    sb_list_bottom(sb_bottom_idx).io.sel :=
      io.sb_list_bottom_ctrl(sb_bottom_idx)
  }

  // This part connects left and right border SBs
  for (i <- 0 until m - 1) {
    val sb_left_idx = i
    val sb_right_idx = i
    val cb_left_idx = i * n
    val cb_right_idx = (i + 1) * n - 1

    sb_list_left(sb_left_idx).io.chan_east_in :=
      cb_list_even(cb_left_idx).io.chan_north_out
    sb_list_right(sb_right_idx).io.chan_west_in :=
      cb_list_even(cb_right_idx).io.chan_south_out

    sb_list_left(sb_left_idx).io.chan_north_in :=
      cb_list_left(i).io.chan_south_out
    sb_list_right(sb_right_idx).io.chan_north_in :=
      cb_list_right(i).io.chan_south_out

    sb_list_left(sb_left_idx).io.chan_south_in :=
      cb_list_left(i + 1).io.chan_north_out
    sb_list_right(sb_right_idx).io.chan_south_in :=
      cb_list_right(i + 1).io.chan_north_out

    sb_list_left(sb_left_idx).io.sel :=
      io.sb_list_left_ctrl(sb_left_idx)
    sb_list_right(sb_right_idx).io.sel :=
      io.sb_list_right_ctrl(sb_right_idx)
  }
  for (i <- 0 until (2 * m + 1)){
    for (j <- 0 until (2 * n + 1)) {
      if (i % 2 == 0 && j % 2 == 0){
        print(s"s ")
      } else if ( i % 2 == 0 && j % 2 == 1) {
        print(s"c ")
      } else if ( i % 2 == 1 && j % 2 == 0 ) {
        print(s"c ")
      } else if ( i % 2 == 1 && j % 2 == 1) {
        print(s"p ")
      }
    }
    print(s"\n")
  }
}

object Overlay extends App {

  chisel3.Driver.execute(args, () => new Overlay(2,2, 8, 4))

}
object OverlayDecoupled extends App {
  chisel3.Driver.execute(args, () => new BlockDecoupled(4, 4, 32))
  chisel3.Driver.execute(args, () => new OverlayDecoupled(4, 4, 32, 4, 4))
}
