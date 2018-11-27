package overlay

import chisel3._
import chisel3.iotesters.PeekPokeTester
import chisel3.util.log2Ceil

class MuxN (val n: Int, val w: Int) extends Module {
  val io = IO(new Bundle {
    val ins = Input(Vec(n, UInt(w.W)))
    val out = Output(UInt(w.W))
    val sel = Input(UInt(log2Ceil(n).W))
  })

  io.out := io.ins(io.sel)
}
object MuxN extends App {

  chisel3.Driver.execute(args, () => new MuxN(3, 32))
}
