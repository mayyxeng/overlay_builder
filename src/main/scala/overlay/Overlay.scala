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
    s c s c s c s c s c s ...  : (s c)*n s
    c p c p c p c p c p c ...  : (p c)*n c
    .                     ...
    .                     ...
    .                     ...
    s c s c s c s c s c s .... : (s c)*n s

    where c = ConnectionBox, s = SwitchBox and P = ProcessingElement
    in total we have :
      (n + 1) * (m + 1) = mn + m + n SwitchBoxes
      (n) * (m + 1) + (n + 1) * m = 2mn + n + m ConnectionBoxes
      mn ProcessingElements
  */
  val zero_val = Seq.fill(chns){0.U(w.W)}
  val gnd = Wire(Vec(chns, UInt(w.W)))
  val switch_box_list = Seq.fill((n + 1) * (m + 1)){
    Module(new SwitchBox(chns, w))
  }
  // val connection_box_list = Seq.fill(2 * n * m + m + n){
  //   Module(new ConnectionBox(chns, w))
  // }
  for (i <- 0 until (2 * m + 1)){
    for (j <- 0 until (2 * n + 1)) {
      if (i % 2 == 0 && j % 2 == 0){
        if ( i == 0 ){
          switch_box_list(j / 2).io.chan_north_in := VecInit(zero_val)
          switch_box_list(j / 2).io.chan_south_in := VecInit(zero_val)
          switch_box_list(j / 2).io.chan_west_in := VecInit(zero_val)
          switch_box_list(j / 2).io.chan_east_in := VecInit(zero_val)
          gnd := switch_box_list(j / 2).io.chan_north_out
          gnd := switch_box_list(j / 2).io.chan_south_out
          gnd := switch_box_list(j / 2).io.chan_west_out
          gnd := switch_box_list(j / 2).io.chan_east_out
        }
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

  chisel3.Driver.execute(args, () => new Overlay(3,3))
}
