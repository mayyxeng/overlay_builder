package overlay


import chisel3._

class IOPads (val m : Int, val n : Int, val w : Int = 32) extends Bundle {
  override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val input_north = Input(Vec(n, UInt(w.W)))
  val input_south = Input(Vec(n, UInt(w.W)))
  val input_west = Input(Vec(n, UInt(w.W)))
  val input_east = Input(Vec(n, UInt(w.W)))
  val output_north = Output(Vec(n, UInt(w.W)))
  val output_south = Output(Vec(n, UInt(w.W)))
  val output_west = Output(Vec(n, UInt(w.W)))
  val output_east = Output(Vec(n, UInt(w.W)))
}
class  Overlay (val m : Int, val n : Int, val w : Int = 32, val chns : Int = 4)
extends Module {

  val io = IO(new IOPads(m, n, w))
  /*

    An overlay with m rows and n columns will actually
    have 2m + 1 rows and 2n + 1 columns because we have to
    fit CBs and SBs
    The patterns is as follows:
      c s c s c s c s c s ... c    : (c s)*n
    c p c p c p c p c p c ... p c  : (c p)*n c
    s c s c s c s c s c s ... c s  : (s c)*n s
    .                     ... . .
    c p c p c p c p c p c ... p c  : (c p)*n c
      c s c s c s c s c s ... c    : (c s)*n

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


  val e_cb_list = Seq.fill(m * (n + 1)) {
    Module(new ConnectionBox(chns, w))
  }
  val o_cb_list = Seq.fill((m + 1) * n) {
    Module(new ConnectionBox(chns, w))
  }
  val sb_list = Seq.fill((m + 1) * (n + 1) - 4) {
    Module(new SwitchBox(chns, w))
  }
  vall pe_list = Seq.fill(m * n) {
    Module(new ProcessingElement(w))
  }

  // Connection PEs inputs
  for (i <- 0 until m) {
    for (j <- 0 until n) {
      val idx = i * n + j
      pe_list(idx).north_in := e_cb_list(i * n + j).west_out
      pe_list(idx).south_in := e_cb_list((i + 1) * n + j).east_out
      pe_list(idx).west_in := o_cb_list(i * (n + 1) + j).east_out
      pe_list(idx).east_in := o_cb_list(i * (n + 1) + j + 1).west_out
    }
  }

  for (i <- 0 until m) {
    for (j <- 0 until n + 1) {
      val idx = i * (n + 1) + j
      if (j == 0) {
        if(i == 0) {
          o_cb_list(idx).north_in := 
        } else if(i == m - 1) {

        }
        o_cb_list(idx).east_in := io.input_west(i)
        o_cb_list(idx).west_in := pe_list(i * n + j - 1).io.east_out

      } else if (j == n) {



      } else {
        o_cb_list(idx).east_in :=
          pe_list(i * n + j).io.west_out
        o_cb_list(idx).west_in :=
          pe_list(i * n + j - 1).io.east_out
        o_cb_list(idx).north_in :=
          sb_list((i - 1) * (n + 1) + j).south_out
        o_cb_list(idx).south_in :=
          sb_list(i * (n + 1) + j).north_out

      }


    }
  }


  for (i <- 0 until (2 * m + 1)){
    for (j <- 0 until (2 * n + 1)) {
      if (i % 2 == 0 && j % 2 == 0){
        if ( i == 0 ){

        }
        print(s"s ")

      } else if ( i % 2 == 0 && j % 2 == 1) {
        /*
          Even rows with odd columns correspond to ConnectionBoxes
          enlosed by SwitchBoxes from sides and IO or PEs from
          below and above.
          In the border rows, (i == 0) || (i == 2m), and border columns
          (j == 0) || (j == 2n), the CBs at the either end are directly
          connected to colliding border column or row.
        */
        if (i == 0 && j == 0) {
          /* NW corner */
          connection_box_list(i).io.chan_west_in :=
            connection_box_list(i + n).io.chan_north_out

          connection_box_list(i).io.chan_north_in :=
            io.input_north(i)

          connection_box_list(i).io.chan_east_in :=
            connection_box_list(i + 1).io.chan_west_out

          connection_box_list(i).io.chan_south_in :=
            connection_box_list(n + 1 + j).io.chan_north_out

          io.input_north(j) := connection_box_list(i).io.chan_north_out


        } else if (i == 0 && j == 2 * n) {

          connection_box_list(i * n + j / 2)
        }
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

  chisel3.Driver.execute(args, () => new Overlay(3,3))
}
