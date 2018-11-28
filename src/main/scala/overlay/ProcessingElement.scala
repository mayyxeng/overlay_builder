package overlay


import chisel3._
import chisel3.util._

class ProcessingElementIO (val w : Int) extends Bundle {
  val north_in = Input(UInt(w.W))
  val south_in = Input(UInt(w.W))
  val west_in = Input(UInt(w.W))
  val east_in = Input(UInt(w.W))
  val north_out = Input(UInt(w.W))
  val south_out = Input(UInt(w.W))
  val west_out = Output(UInt(w.W))
  val east_out = Output(UInt(w.W))
}

class ProcessingElement (val w : Int) extends Module {
  val io = IO(new ProcessingElementIO(w))

  io.north_out := io.north_in
  io.south_out := io.south_in
  io.west_out := io.west_in
  io.east_out := io.east_in

}
