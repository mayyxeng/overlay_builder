package overlay


import chisel3._


class  Overlay_ (val w : Int, val chns : Int) extends Module {
  override val compileOptions = chisel3.core.ExplicitCompileOptions.NotStrict.copy(explicitInvalidate = false)
  val io = IO(new IOPads(2, 2, w))
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
  val zero_sel = Seq.fill(chns){0.U(2.W)}
  //val gnd = Wire(Vec(chns, UInt(w.W)))
  val switch_box_list = Seq.fill((2) * (2)){
    Module(new SwitchBox(chns, w))
  }
  switch_box_list(0).io.chan_north_in := zero_val
  switch_box_list(0).io.chan_west_in := zero_val
  switch_box_list(0).io.chan_south_in := switch_box_list(3).io.chan_north_out
  switch_box_list(0).io.chan_east_in := switch_box_list(1).io.chan_west_out

  switch_box_list(0).io.sel_north := zero_sel
  switch_box_list(0).io.sel_south := zero_sel
  switch_box_list(0).io.sel_west := zero_sel
  switch_box_list(0).io.sel_east := zero_sel



}

object Overlay_ extends App {

  chisel3.Driver.execute(args, () => new Overlay_(32, 2) )
}
