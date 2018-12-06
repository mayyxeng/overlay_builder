package overlay

import chisel3._

class  RandomOverlay (val m : Int, val n : Int, val w : Int = 32, val chns : Int = 4)
extends Module {

  val io = IO(new IOPads(m, n, w))
  val cb_list_top_ctrl = Reg(Vec(n, new CBCTRL(chns, w)))
  val cb_list_bottom_ctrl = Reg(Vec(n, new CBCTRL(chns, w)))
  val cb_list_left_ctrl = Reg(Vec(n, new CBCTRL(chns, w)))
  val cb_list_right_ctrl = Reg(Vec(n, new CBCTRL(chns, w)))
  val cb_list_even_ctrl = Reg(Vec((m - 1) * n, new CBCTRL(chns, w)))
  val cb_list_odd_ctrl = Reg(Vec(m * (n - 1), new CBCTRL(chns, w)))
  val sb_list_top_ctrl = Reg(Vec(n - 1, new NSBCTRL(chns, w)))
  val sb_list_bottom_ctrl = Reg(Vec(n - 1, new SSBCTRL(chns, w)))
  val sb_list_left_ctrl = Reg(Vec(m - 1, new WSBCTRL(chns, w)))
  val sb_list_right_ctrl = Reg(Vec(m - 1, new ESBCTRL(chns, w)))
  val sb_list_even_ctrl = Reg(Vec((m - 1) * (n - 1), new SBCTRL(chns, w)))

  val overlay = Module(new Overlay(m, n, w, chns))
  overlay.io.cb_list_top_ctrl := cb_list_top_ctrl
  overlay.io.cb_list_bottom_ctrl := cb_list_bottom_ctrl
  overlay.io.cb_list_left_ctrl := cb_list_left_ctrl
  overlay.io.cb_list_right_ctrl := cb_list_right_ctrl
  overlay.io.cb_list_even_ctrl := cb_list_even_ctrl
  overlay.io.cb_list_odd_ctrl := cb_list_odd_ctrl
  overlay.io.sb_list_top_ctrl := sb_list_top_ctrl
  overlay.io.sb_list_bottom_ctrl := sb_list_bottom_ctrl
  overlay.io.sb_list_left_ctrl := sb_list_left_ctrl
  overlay.io.sb_list_right_ctrl := sb_list_right_ctrl
  overlay.io.sb_list_even_ctrl := sb_list_even_ctrl



  overlay.io.input_north := io.input_north
  overlay.io.input_south := io.input_south
  overlay.io.input_west := io.input_west
  overlay.io.input_east := io.input_east
  io.output_north := overlay.io.output_north
  io.output_south := overlay.io.output_south
  io.output_west := overlay.io.output_west
  io.output_east := overlay.io.output_east

}

object RandomOverlay extends App {

  chisel3.Driver.execute(args, () => new RandomOverlay(2,2))
}
