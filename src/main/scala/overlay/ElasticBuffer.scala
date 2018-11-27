package overlay

import chisel3._
import chisel3.util._

class ElasticBufferControlIO extends Bundle {
  val valid_in = Input(UInt(1.W))
  val stall_in = Input(UInt(1.W))
  val valid_out = Output(UInt(1.W))
  val stall_out = Output(UInt(1.W))
  val main_ff_we = Output(UInt(1.W))  //Write enable for the second FF
  val aux_ff_we = Output(UInt(1.W))   //Write enable for the first FF
  val mux_sel = Output(UInt(1.W))
}
class ElasticBufferControl extends Module {
    val io = IO(new ElasticBufferControlIO())

    val sEmpty :: sHalf :: sFull :: Nil = Enum(3)
    // Note that if mux_sel is zero then data goes to main FF
    val state = RegInit(sEmpty)
    io.main_ff_we := 0.U(1.W)
    io.aux_ff_we := 0.U(1.W)
    io.mux_sel := 0.U(1.W)
    io.valid_out := 0.U(1.W)
    io.stall_out := 0.U(1.W)
    switch (state) {
      is (sEmpty) {
        io.main_ff_we := 1.U(1.W)
        when (io.valid_in === 1.U(1.W)) {
          state := sHalf
        } .elsewhen (io.valid_in === 0.U(1.W)) {
          state := sEmpty
        }
      }
      is (sHalf) {
        io.valid_out := 1.U(1.W)
        when (io.valid_in === 0.U(1.W) && io.stall_in === 0.U(1.W)) {
          io.main_ff_we := 1.U(1.W)
          state := sEmpty
        } .elsewhen (io.valid_in === 1.U && io.stall_in === 0.U(1.W)) {
          io.main_ff_we := 1.U(1.W)
          state := sHalf
        } .elsewhen (io.valid_in === 0.U && io.stall_in === 1.U(1.W)) {
          io.aux_ff_we := 1.U(1.W)
          state := sHalf
        } .elsewhen (io.valid_in === 1.U && io.stall_in === 1.U(1.W)) {
          io.aux_ff_we := 1.U(1.W)
          state := sFull
        }
      }
      is (sFull) {
        io.valid_out := 1.U(1.W)
        io.stall_out := 1.U(1.W)
        io.mux_sel := 1.U(1.W)
        when (io.stall_in === 0.U(1.W)) {
          io.main_ff_we := 1.U(1.W)
        }
      }
    }
}

class ElasticBuffer(val w : Int) extends Module {
  val io = IO(new Bundle {
    val data_in = Input(UInt(w.W))
    val data_out = Output(UInt(w.W))
    val valid_in = Input(UInt(1.W))
    val valid_out = Output(UInt(1.W))
    val stall_in = Input(UInt(1.W))
    val stall_out = Output(UInt(1.W))
  })

  val mux_sel = Wire(UInt(1.W))
  val main_ff_we = Wire(UInt(1.W))
  val aux_ff_we = Wire(UInt(1.W))

  val main_ff = RegInit(0.U(w.W))
  val aux_ff = RegInit(0.U(w.W))

  when (main_ff_we === 1.U(1.W)) {
    main_ff := Mux(mux_sel.toBool, io.data_in, aux_ff)
  }
  when (aux_ff_we === 1.U(1.W)) {
    aux_ff := io.data_in
  }

  val controller = Module(new ElasticBufferControl())
  controller.io.stall_in := io.stall_in
  controller.io.valid_in := io.valid_in
  io.valid_out := controller.io.valid_out
  io.stall_out := controller.io.stall_out
  mux_sel := controller.io.mux_sel
  main_ff_we := controller.io.main_ff_we
  aux_ff_we := controller.io.aux_ff_we
  io.data_out := main_ff
}
object ElasticBufferControl extends App {
  chisel3.Driver.execute(args, () => new ElasticBufferControl())
}
object ElasticBuffer extends App {
  chisel3.Driver.execute(args, () => new ElasticBuffer(32))
}
