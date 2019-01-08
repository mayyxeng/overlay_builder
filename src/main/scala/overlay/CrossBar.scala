package overlay

import chisel3._
import chisel3.util._


class CrossBarDecoupledIO (val in_count: Int, val out_count: Int, val w: Int)
extends Bundle {
  val inputs = Vec(in_count, Flipped(Decoupled(UInt(w.W))))
  val outputs = Vec(out_count, Decoupled(UInt(w.W)))
  val ctrl = Input(Vec(in_count, UInt(log2Ceil(out_count).W)))
}
class CrossBarDecoupled (val in_count: Int, val out_count: Int, val w: Int)
extends Module {
  val io = IO(new CrossBarDecoupledIO(in_count, out_count, w))

  val branch_list = Seq.fill(in_count) {
    Module(new BranchDecoupled(out_count, w))
  }
  val merge_list = Seq.fill(out_count) {
    Module(new MergeDecoupled(in_count, w))
  }

  for (i <- 0 until in_count) {
    branch_list(i).io.input <> io.inputs(i)
    branch_list(i).io.conditional := io.ctrl(i)
  }
  for (i <- 0 until out_count) {
    io.outputs(i) <> merge_list(i).io.output
    for (j <- 0 until in_count) {
      merge_list(i).io.input_vector(j) <> branch_list(j).io.output_vector(i)
    }
  }

}
/**
  *@brief
  * Control matrix is symmetric, so we should be able keep half of it
  * Right now we have double the number of control_signals that we need
  * It is worth to note that for a crossbar with n entry points and m
  * exit points we have the following relations:
  *   if m >= n : v = n(n-1)(n-2)...1 = n! valid connetions exist
  *   if m < n : v = n(n-1)(n-2)...(n-m+1) = n!/(n-m)! valid connections exist
  *   if we have v valid connections then we could enumerate all possible
  *   connections with lg2Ceil(v) bits
  *
  *
  */
class CrossBarIO (val n : Int, val m : Int, val w : Int) extends Bundle {
  val input_vector = Flipped(Vec(n, Decoupled(UInt(w.W))))
  val output_vector = Vec(m, Decoupled(UInt(w.W)))
  val control_matrix = Input(Vec(n * m, UInt(1.W)))

}

class CrossBar (val n : Int, val m : Int, val w : Int) extends Module {


  val io = IO(new CrossBarIO(n, m, w))
  val zero_val = Seq.fill(m){0.U(w.W)}
  //io.output_vector.bits := VecInit(zero_val)
  // k = i * m + j
  for (k <- 0 until m * n) {
    val i = k / m
    val j = k % m
    when (io.control_matrix(k) === 1.U(1.W)) {
      io.output_vector(i) <> io.input_vector(j)

    } .otherwise {
      io.output_vector(i).bits := 0.U
      io.output_vector(i).valid := false.B
      io.input_vector(j).ready := false.B
    }

  }

}


object CrossBar extends App {

  chisel3.Driver.execute(args, () => new CrossBar(3, 3, 32))
  chisel3.Driver.execute(args, () => new CrossBarDecoupled(3, 3, 32))
}
