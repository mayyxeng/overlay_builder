package overlay

import chisel3._
import chisel3.iotesters.PeekPokeTester
import chisel3.util._

object MuxLookupDecoupled {
  /** @param key a key to search for
    * @param default a default value if nothing is found
    * @param mapping a sequence to search of keys and values
    * @return the value found or the default if not
    */
  def apply[S <: UInt, T <: Data] (key: S, mapping: Seq[(S, DecoupledIO[T])], output: DecoupledIO[T]): DecoupledIO[T] = {
    var res = Wire(output.cloneType)
    res.bits := 0.U
    res.valid := false.B
    res.ready := false.B
    for ((k, v) <- mapping.reverse) {
      res.bits := Mux(k === key, v.bits, res.bits)
      res.valid := Mux(k === key, v.valid, res.valid)
      if (k == key)
        v.ready := output.ready
      else
        v.ready := false.B
    }
    res
  }
}

object MuxN {
  /** @param key a key to search for
    * @param default a default value if nothing is found
    * @param mapping a sequence to search of keys and values
    * @return the value found or the default if not
    */
  def apply[S <: UInt, T <: Data] (key: S, default: T, mapping: Vec[T], n: Int): T = {
    var res = default
    val sel_seq = Seq.tabulate(n) (n => (n.U, mapping(n)))
    res = MuxLookup[UInt, T](key, default, sel_seq)
    res
  }
}

class MuxNDec (val n: Int, val w: Int) extends Module {
  val io = IO(new Bundle {
    val input_vector = Flipped(Vec(n, Decoupled(UInt(w.W))))
    val output_vector = Decoupled(UInt(w.W))
    val sel = Input(UInt(log2Ceil(n).W))
  })
  val sel_seq = Seq.tabulate(n)( n => (n.U, io.input_vector(n)))
  io.output_vector <> MuxLookupDecoupled [UInt, UInt](io.sel, sel_seq, io.output_vector)
}
class MuxN (val n: Int, val w: Int) extends Module {
  val io = IO(new Bundle {
    val ins = Input(Vec(n, UInt(w.W)))
    val out = Output(UInt(w.W))
    val sel = Input(UInt(log2Ceil(n).W))
  })
  //val sel_seq = Seq.tabulate(n) (n => (n.U, io.ins(n)))
  //io.out := MuxLookup[UInt, UInt](io.sel, 0.U, sel_seq)
  io.out := MuxN(io.sel, 0.U, io.ins, n)

}
object MuxNVerilog extends App {

  chisel3.Driver.execute(args, () => new MuxN(3, 32))
  chisel3.Driver.execute(args, () => new MuxNDec(3, 32))
}
