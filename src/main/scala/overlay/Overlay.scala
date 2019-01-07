package overlay


import chisel3._
import chisel3.util._

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
class BDGCTRL(val chns: Int, val cncs: Int) extends Bundle{
  val cbo = new CBODCTRL(chns, cncs)
  val cbi = new CBIDCTRL(chns, cncs)
}
class BlockDecoupledGenericIO(val chns: Int, val cncs: Int,
  val wdth: Int) extends Bundle{

  val north = new DecoupledChannel(chns, wdth)
  val east = new DecoupledChannel(chns, wdth)
  val ctrl = Input(new BDGCTRL(chns, cncs))
}

class BlockDecoupledGeneric(val chns: Int, val cncs: Int,
  val wdth: Int) extends Module {

  class CTRL extends Bundle{
    val cbo = new CBODCTRL(chns, cncs)
    val cbi = new CBIDCTRL(chns, cncs)
  }
  class InOut(val chns: Int, val cncs: Int,
    val wdth: Int) extends Bundle{

    val north = new DecoupledChannel(chns, wdth)
    val east = new DecoupledChannel(chns, wdth)
    val ctrl = Input(new CTRL)
  }
  val io = IO(new BlockDecoupledGenericIO(chns, cncs, wdth){
    val cbosb = new DecoupledChannel(chns, wdth)
    val cbisb = new DecoupledChannel(chns, wdth)
  })

  val connection_box_in = Module(new CBID(chns, cncs, wdth))
  val connection_box_out = Module(new CBOD(chns, cncs, wdth))
  val compute_unit = Module(new CU(cncs, wdth))

  connection_box_out.io.north <=> io.north
  connection_box_out.io.out <> compute_unit.io.input
  connection_box_out.io.south <=> io.cbosb
  connection_box_out.io.ctrl := io.ctrl.cbo

  connection_box_in.io.in <> compute_unit.io.output
  connection_box_in.io.west <=> io.cbisb
  connection_box_in.io.east <=> io.east
  connection_box_in.io.ctrl := io.ctrl.cbi

}
class BDCTRL(chns: Int, cncs: Int) extends BDGCTRL(chns, cncs) {
  val sb = new SBDCTRL(chns)
}
class BlockDecoupledIO(chns: Int, cncs: Int, wdth: Int) extends
  BlockDecoupledGenericIO(chns, cncs, wdth) {
  val south = new DecoupledChannel(chns, wdth)
  val west = new DecoupledChannel(chns, wdth)
  override val ctrl = Input(new BDCTRL(chns, cncs))
}
class BlockDecoupled(val chns: Int = 4, val cncs: Int = 4,
  val wdth: Int = 32) extends Module {

  val io = IO(new BlockDecoupledIO(chns, cncs, wdth))

  val generic_block = Module(new BlockDecoupledGeneric(chns, cncs, wdth))
  val switch_box = Module(new SBD(chns, wdth))

  generic_block.io.north <=> io.north
  generic_block.io.east <=> io.east
  generic_block.io.ctrl.cbo := io.ctrl.cbo
  generic_block.io.ctrl.cbi := io.ctrl.cbi
  generic_block.io.cbosb <> switch_box.io.north
  generic_block.io.cbisb <> switch_box.io.east
  io.west <=> switch_box.io.west
  io.south <=> switch_box.io.south
  switch_box.io.ctrl := io.ctrl.sb

}
class BDBCTRL(chns: Int, cncs: Int) extends BDGCTRL(chns, cncs) {
  val sb = new SBD3CTRL(chns)
}
class BlockDecoupledBorderIO(chns: Int, cncs: Int, wdth: Int) extends
  BlockDecoupledGenericIO(chns, cncs, wdth) {
  val sbout = new DecoupledChannel(chns, wdth)
  override val ctrl = Input(new BDBCTRL(chns, cncs))
}
class BlockDecoupledBorder(val chns: Int, val cncs: Int,
  val wdth: Int) extends Module {

  val io = IO(new BlockDecoupledBorderIO(chns, cncs, wdth))

  val generic_block = Module(new BlockDecoupledGeneric(chns, cncs, wdth))
  val switch_box = Module(new SBD3(chns, wdth))

  generic_block.io.cbosb <> switch_box.io.dir0
  generic_block.io.cbisb <> switch_box.io.dir1

  generic_block.io.north <=> io.north
  generic_block.io.east <=> io.east

  switch_box.io.dir2 <=> io.sbout

  switch_box.io.ctrl := io.ctrl.sb
  generic_block.io.ctrl.cbo := io.ctrl.cbo
  generic_block.io.ctrl.cbi := io.ctrl.cbi
}
class ODCCTRL(val rws: Int, val cls: Int,
  val chns: Int, val cncs: Int) extends Bundle {

  val core = Vec(rws * cls, new BDCTRL(chns, cncs))
}
class ODCIO (val rws: Int, val cls: Int,
  val wdth: Int, val chns: Int, val cncs: Int) extends Bundle {
    val north = Vec(cls, new DecoupledChannel(chns, wdth))
    val south = Vec(cls, new DecoupledChannel(chns, wdth))
    val west = Vec(cls, new DecoupledChannel(chns, wdth))
    val east = Vec(cls, new DecoupledChannel(chns, wdth))
    val ctrl = Input(new ODCCTRL(rws, cls, chns, cncs))
}
class OverlayDecoupledCore(val rws:Int, val cls: Int, val wdth: Int = 32,
  val chns: Int = 4, val cncs: Int = 4) extends Module {

  val io = IO(new ODCIO(rws, cls, wdth, chns, cncs))
  val blocks = Seq.fill((rws) * (cls)) {
    Module(new BlockDecoupled(chns, cncs, wdth))
  }


  for (i <- 0 until rws ) {
    for (j <- 0 until cls ) {
      val idx = i * cls + j
      blocks(idx).io.ctrl := io.ctrl.core(idx)
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
class IOPBCTRL(val chns: Int) extends Bundle {
  val cbi = new CBIDCTRL(chns, 1)
  val cbo = new CBODCTRL(chns, 1)
}
class IOPadBlock(val chns: Int, val wdth: Int) extends Module {

  val io = IO(new Bundle {
    val in = Flipped(Decoupled(UInt(wdth.W)))
    val out = Decoupled(UInt(wdth.W))
    val dir0 = new DecoupledChannel(chns, wdth)
    val dir1 = new DecoupledChannel(chns, wdth)
    val ctrl = Input(new IOPBCTRL(chns))
  })

  val cbo = Module(new CBOD(chns, 1, wdth))
  val cbi = Module(new CBID(chns, 1, wdth))

  cbo.io.out.connection(0) <> io.out
  cbi.io.in.connection(0) <> io.in

  cbo.io.north <> cbi.io.west

  io.dir0 <=> cbo.io.south
  io.dir1 <=> cbi.io.east
  cbo.io.ctrl := io.ctrl.cbo
  cbi.io.ctrl := io.ctrl.cbi
}

class ODTCTRL(rws: Int, cls: Int , chns: Int, cncs: Int)
extends ODCCTRL(rws, cls , chns, cncs){
  val left = Vec(rws, new BDBCTRL(chns, cncs))
  val bottom = Vec(cls, new BDBCTRL(chns, cncs))
  val corner = new BDGCTRL(chns, cncs)
}
class ODTIO(val rws: Int, val cls: Int,
  val wdth: Int, val chns: Int, val cncs: Int) extends Bundle {

  val north = Vec(cls, new DecoupledChannel(chns, wdth))
  val east = Vec(rws, new DecoupledChannel(chns, wdth))

  val ctrl = Input(new ODTCTRL(rws - 1, cls - 1, chns, cncs))

}
class OverlayDecoupledTrimmed(val rws:Int, val cls: Int, val wdth: Int = 32,
  val chns: Int = 4, val cncs: Int = 4) extends Module {

  val io = IO(new ODTIO(rws, cls, wdth, chns, cncs))

  val left_blocks = Seq.fill(rws - 1) {
    Module(new BlockDecoupledBorder(chns, cncs, wdth))
  }
  val bottom_blocks = Seq.fill(cls - 1) {
    Module(new BlockDecoupledBorder(chns, cncs, wdth))
  }
  val corner_block = Module(new BlockDecoupledGeneric(chns, cncs, wdth))
  corner_block.io.cbosb <> corner_block.io.cbisb
  corner_block.io.ctrl := io.ctrl.corner
  val core_blocks = Module(
    new OverlayDecoupledCore(rws - 1, cls - 1, wdth, chns, cncs))
  core_blocks.io.ctrl.core := io.ctrl.core

  for (i <- 0 until rws - 1) {
    left_blocks(i).io.east <> core_blocks.io.west(i)
    left_blocks(i).io.ctrl := io.ctrl.left(i)
    if (i == 0) {
      left_blocks(i).io.north <=> io.north(0)
    } else {
      left_blocks(i).io.north <> left_blocks(i - 1).io.sbout
    }
    if (i == rws - 2) {
      left_blocks(i).io.sbout <> corner_block.io.north
    } else {
      left_blocks(i).io.sbout <> left_blocks(i + 1).io.north
    }
    io.east(i) <=> core_blocks.io.east(i)
    // if (i == rws - 2) {
    //   left_blocks(i).io.sbout <> corner_block.io.north
    // } else {
    //   left_blocks(i).io.sbout <> left_blocks(i + 1).io.north
    // }
  }

  for (j <- 0 until cls - 1) {

    bottom_blocks(j).io.north <> core_blocks.io.south(j)
    bottom_blocks(j).io.ctrl := io.ctrl.bottom(j)
    if (j == 0) {
      bottom_blocks(j).io.sbout <> corner_block.io.east
    } else {
      bottom_blocks(j).io.sbout <> bottom_blocks(j - 1).io.east
    }
    if(j == cls - 2) {
      bottom_blocks(j).io.east <=> io.east(cls - 1)

    } else {
      bottom_blocks(j).io.east <> bottom_blocks(j + 1).io.sbout
    }
    io.north(j + 1) <=> core_blocks.io.north(j)
  }
}


class IOPBarCTRL (val lgnth: Int, val chns: Int, val num: Int) extends Bundle{
  val sb = Vec(lgnth - 1, new SBD3CTRL(chns))
  val pad = Vec(num, new IOPBCTRL(chns))
}


/** @brief IOPadBar class to limit the number of GPIOs for the
 *  OvelayDecoupledTrimmed Class
 *  @param lngth the length of the bar, equals the number of rows or columns in
 *               the overlay module
 *  @param wdth bit width for the overlay
 *  @param chns number of channels in the overlay, this is the VPR's
 *              channel length times 2
 *  @param strt start position of the IOPadBlocks in the IOPadBar
 *  @param intrvl interval between IOPadBlocks in the bar
 */
class IOPadBar (val lngth: Int, val wdth: Int, val chns: Int, val strt: Int,
  val intrvl: Int) extends Module {
  val correction: Int = if ((lngth - strt) % (intrvl + 1) != 0) 1 else 0
  val num: Int = (lngth - strt) / (intrvl + 1) + correction.toInt
  println(s"IOPadBar with $num i/os of length $lngth and interval $intrvl")
  for (j <- 0 until lngth){
    val pad_idx = PadIndex(j)
    print(s"$pad_idx,")
  }
  println("")
  val io = IO(new Bundle {
    val gpi = Vec(num, Flipped(Decoupled(UInt(wdth.W))))
    val gpo = Vec(num, Decoupled(UInt(wdth.W)))
    val ovio = Vec(lngth, new DecoupledChannel(chns, wdth))
    val tail = new DecoupledChannel(chns, wdth)
    val ctrl = Input(new IOPBarCTRL(lngth, chns, num))
  })

  def PadIndex(iter: Int): Int =
    if ((iter - strt) % (intrvl + 1) == 0)
      (iter - strt) / (intrvl + 1)
    else
      -1

  val sb_list = Seq.fill(lngth - 1) {
    Module(new SBD3(chns, wdth))
  }
  val pad_list = Seq.fill(num) {
    Module(new IOPadBlock(chns, wdth))
  }

  for (j <- 0 until lngth - 1) {
    val pad_idx = PadIndex(j + 1)
    if (j == lngth - 2)
      if (pad_idx != -1){
        sb_list(j).io.dir2 <> pad_list(pad_idx).io.dir0
        pad_list(pad_idx).io.dir1 <=> io.tail
      } else
        sb_list(j).io.dir2 <=> io.tail
    else
      if (pad_idx != -1)
        sb_list(j).io.dir2 <> pad_list(pad_idx).io.dir0
      else {
        //println(j)
        sb_list(j).io.dir2 <> sb_list(j + 1).io.dir0
      }
  }

  for (j <- 0 until lngth - 1) {
    val pad_idx = PadIndex(j)
    if (j == 0)
      if (pad_idx != -1) {
        sb_list(j).io.dir0 <> pad_list(pad_idx).io.dir1
        pad_list(pad_idx).io.dir0 <=> io.ovio(j)
      } else {
        sb_list(j).io.dir0 <=> io.ovio(j)
      }
    else
      if (pad_idx != -1)
        sb_list(j).io.dir0 <> pad_list(pad_idx).io.dir1
      else
        sb_list(j).io.dir0 <> sb_list(j - 1).io.dir2
  }

  for (j <- 0 until lngth - 1) {
    sb_list(j).io.dir1 <=> io.ovio(j + 1)
    sb_list(j).io.ctrl := io.ctrl.sb(j)
  }

  for (j <- 0 until num) {
    pad_list(j).io.ctrl := io.ctrl.pad(j)
    pad_list(j).io.in <> io.gpi(j)
    io.gpo(j) <> pad_list(j).io.out
  }
}


/** A wrapper for the class OverlayDecoupledTrimmed.
 *  This class wraps the OvelayDecoupledTrimmed class into an Overlay
 *  with custom number of I/O pins that is specified by the user.
 *  It is necessary to reduce the number of I/Os so that it could fit in
 *  a FPGA or any other hardware setting. Excluding the controll signals
 *  OverlayDecoupledTrimmed has (wdth + 2) * (rws + cls) io bits as I/O.
 *  @param rws: number of rows of blocks
 *  @param cls: number of columns of blocks
 *  @param wdth: number of bits in data lanes
 *  @param chns: number of channels in global routing network. Channels go
 *               in two directions, for example each switch box with chns = 2
 *               has 2 (wdth + 2) bits comming in and 2(wdth + 2) bits comming
 *               out at each port. In VPRs terms, chns = route_chan_width / 2
 *  @param cncs: number of incoming and/or outgoing connections from each
 *               cumpute unit. cncs = 2 means that 2(wdth + 2) bits are going
 *               into each compute unit and 2(wdth + 2) bits are comming out of
 *               each compute unit.
 *  @param sx: start x index for the horizontal(top) IOPads array. blocks are
 *             indexed starting from top-left and end at bottom-right. sx = 0
 *             means that IOPads will be placed from the first possible location
 *             (top-left).
 *  @param ix: increment x for top IOPads. Indicates the distance between two
 *             consecutive IOPads in the x direction. ix = 0 means IOPads are
 *             placed one after the other with no empty space in between.
 *  @param sy: start y for right IOPads. Same as sx but for the vertical(right)
 *             IOPads.
 *  @param iy: same as ix but for right IOPads.
 *
 */
class ODCPCTRL (
                 val rws: Int,
                 val cls: Int,
                 val chns: Int,
                 val cncs: Int,
                 val nx: Int,
                 val ny: Int
               )
 extends Bundle{
   val overlay = new ODTCTRL(rws - 1, cls - 1, chns, cncs)
   val pad_x = new IOPBarCTRL(cls, chns, nx)
   val pad_y = new IOPBarCTRL(rws, chns, ny)
 }
class ODCPIO (
               val rws: Int,
               val cls: Int,
               val wdth: Int,
               val chns: Int,
               val cncs: Int,
               val nx: Int,
               val ny: Int)
 extends Bundle{

   val in_top = Flipped(Vec(nx, Decoupled(UInt(wdth.W))))
   val out_top = Vec(nx, Decoupled(UInt(wdth.W)))
   val in_right = Flipped(Vec(ny, Decoupled(UInt(wdth.W))))
   val out_right = Vec(ny, Decoupled(UInt(wdth.W)))
   val ctrl = Input(new ODCPCTRL(rws, cls, chns, cncs, nx, ny))

}
class OverlayDecoupledCustomPins(
                                  val rws: Int,
                                  val cls: Int,
                                  val wdth: Int,
                                  val chns: Int,
                                  val cncs: Int,
                                  val sx: Int,
                                  val ix: Int,
                                  val sy: Int,
                                  val iy: Int)
extends Module{

  val pad_x = Module(new IOPadBar(cls, wdth, chns, sx, ix))
  val pad_y = Module(new IOPadBar(rws, wdth, chns, sy, iy))
  val overlay = Module(new OverlayDecoupledTrimmed(rws, cls, wdth, chns, cncs))
  val nx = pad_x.num
  val ny = pad_y.num
  val io = IO(new ODCPIO(rws, cls, wdth, chns, cncs, nx, ny))

  pad_x.io.tail <> pad_y.io.tail


  pad_y.io.gpi <> io.in_right
  pad_y.io.gpo <> io.out_right

  pad_x.io.gpi <> io.in_top
  pad_x.io.gpo <> io.out_top

  pad_x.io.ctrl := io.ctrl.pad_x
  pad_y.io.ctrl := io.ctrl.pad_y

  overlay.io.ctrl := io.ctrl.overlay
  for (j <- 0 until cls)
    overlay.io.north(j) <> pad_x.io.ovio(j)
  for (i <- 0 until rws)
    overlay.io.east(i) <> pad_y.io.ovio(i)
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

  chisel3.Driver.execute(args, () => new Overlay(10,10,32, 32))

}
object OverlayDecoupled extends App {
  //chisel3.Driver.execute(args, () => new IOPadBar(9, 32, 4, 1, 3))
  chisel3.Driver.execute(args, () => new OverlayDecoupledCustomPins(8, 8, 32, 4, 4, 0, 1, 0, 1))
  // chisel3.Driver.execute(args, () => new IOPadBlock(4, 32))
  chisel3.Driver.execute(args, () => new BlockDecoupledGeneric(4, 4, 32))
  // chisel3.Driver.execute(args, () => new BlockDecoupled(4, 4, 32))
  // chisel3.Driver.execute(args, () => new BlockDecoupledBorder(4, 4, 32))
  // chisel3.Driver.execute(args, () => new OverlayDecoupledCore(2, 2, 32, 4, 4))
  //chisel3.Driver.execute(args, () => new OverlayDecoupledTrimmed(2, 2, 32, 4, 4))
}
// val rws: Int,
// val cls: Int,
// val wdth: Int,
// val chns: Int,
// val cncs: Int,
// val sx: Int,
// val ix: Int,
// val sy: Int,
// val iy: Int)
