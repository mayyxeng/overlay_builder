package overlay

import chisel3._

class CrossBarIO (val n : Int, val m : Int, val w : Int) extends Bundle {
  val input_vector = Input(Vec(n , UInt(w.W)))
  val output_vector = Output(Vec(n, UInt(w.W)))
  val control_matrix = Input(Vec(n * m, UInt(1.W)))
  // Control matrix is symmetric, so we should be able keep half of it
  // Right now we have double the number of control_signals that we need
  // It is worth to note that for a crossbar with n entry points and m
  // exit points we have the following relations:
  /*
    if m >= n : v = n(n-1)(n-2)...1 = n! valid connetions exist
    if m < n : v = n(n-1)(n-2)...(n-m+1) = n!/(n-m)! valid connections exist
    if we have v valid connections then we could enumerate all possible
    connections with lg2Ceil(v) bits
  */
}

class CrossBar (val n : Int, val m : Int, val w : Int) extends Module {
  val io = IO(new CrossBarIO(n, m, w))


  val zero_val = Seq.fill(m){0.U(w.W)}
  io.output_vector := VecInit(zero_val)
  // k = i * m + j
  for (k <- 0 until m * n) {
    val i = k / m
    val j = k % m
    when (io.control_matrix(k) === 1.U(1.W)) {
      io.output_vector(i) := io.input_vector(j)
    }
  }

}


object CrossBar extends App {

  chisel3.Driver.execute(args, () => new CrossBar(3, 3, 32))
}
