package Top

import Chisel._
import Node._
import Constants._
import Instructions._

object rocketFPConstants
{
  val FCMD_ADD =        Bits("b000000")
  val FCMD_SUB =        Bits("b000001")
  val FCMD_MUL =        Bits("b000010")
  val FCMD_DIV =        Bits("b000011")
  val FCMD_SQRT =       Bits("b000100")
  val FCMD_SGNINJ =     Bits("b000101")
  val FCMD_SGNINJN =    Bits("b000110")
  val FCMD_SGNMUL =     Bits("b000111")
  val FCMD_CVT_L_FMT =  Bits("b001000")
  val FCMD_CVT_LU_FMT = Bits("b001001")
  val FCMD_CVT_W_FMT =  Bits("b001010")
  val FCMD_CVT_WU_FMT = Bits("b001011")
  val FCMD_CVT_FMT_L =  Bits("b001100")
  val FCMD_CVT_FMT_LU = Bits("b001101")
  val FCMD_CVT_FMT_W =  Bits("b001110")
  val FCMD_CVT_FMT_WU = Bits("b001111")
  val FCMD_CVT_FMT_S =  Bits("b010000")
  val FCMD_CVT_FMT_D =  Bits("b010001")
  val FCMD_EQ =         Bits("b010101")
  val FCMD_LT =         Bits("b010110")
  val FCMD_LE =         Bits("b010111")
  val FCMD_MIN =        Bits("b011000")
  val FCMD_MAX =        Bits("b011001")
  val FCMD_MFTX =       Bits("b011100")
  val FCMD_MFFSR =      Bits("b011101")
  val FCMD_MXTF =       Bits("b011110")
  val FCMD_MTFSR =      Bits("b011111")
  val FCMD_MADD =       Bits("b100100")
  val FCMD_MSUB =       Bits("b100101")
  val FCMD_NMSUB =      Bits("b100110")
  val FCMD_NMADD =      Bits("b100111")
  val FCMD_LOAD =       Bits("b111000")
  val FCMD_STORE =      Bits("b111001")
  val FCMD_WIDTH = 6
  val FSR_WIDTH = 8
}
import rocketFPConstants._

class rocketFPUCtrlSigs extends Bundle
{
  val cmd = Bits(width = FCMD_WIDTH)
  val valid = Bool()
  val wen = Bool()
  val sboard = Bool()
  val ren1 = Bool()
  val ren2 = Bool()
  val ren3 = Bool()
  val single = Bool()
  val fromint = Bool()
  val toint = Bool()
  val fastpipe = Bool()
  val fma = Bool()
  val store = Bool()
  val rdfsr = Bool()
  val wrfsr = Bool()
}

class rocketFPUDecoder extends Component
{
  val io = new Bundle {
    val inst = Bits(32, INPUT)
    val sigs = new rocketFPUCtrlSigs().asOutput
  }

  val N = Bool(false)
  val Y = Bool(true)
  val X = Bool(false)
  val FCMD_X = FCMD_ADD
  val decoder = ListLookup(io.inst,
    List                  (N,FCMD_X,         X,X,X,X,X,X,X,X,X,X,X,X,X),
    Array(FLW      -> List(Y,FCMD_LOAD,      Y,N,N,N,N,Y,N,N,N,N,N,N,N),
          FLD      -> List(Y,FCMD_LOAD,      Y,N,N,N,N,N,N,N,N,N,N,N,N),
          FSW      -> List(Y,FCMD_STORE,     N,N,N,Y,N,Y,N,N,N,N,Y,N,N),
          FSD      -> List(Y,FCMD_STORE,     N,N,N,Y,N,N,N,N,N,N,Y,N,N),
          MXTF_S   -> List(Y,FCMD_MXTF,      Y,N,N,N,N,Y,Y,N,Y,N,N,N,N),
          MXTF_D   -> List(Y,FCMD_MXTF,      Y,N,N,N,N,N,Y,N,Y,N,N,N,N),
          FCVT_S_W -> List(Y,FCMD_CVT_FMT_W, Y,N,N,N,N,Y,Y,N,Y,N,N,N,N),
          FCVT_S_WU-> List(Y,FCMD_CVT_FMT_WU,Y,N,N,N,N,Y,Y,N,Y,N,N,N,N),
          FCVT_S_L -> List(Y,FCMD_CVT_FMT_L, Y,N,N,N,N,Y,Y,N,Y,N,N,N,N),
          FCVT_S_LU-> List(Y,FCMD_CVT_FMT_LU,Y,N,N,N,N,Y,Y,N,Y,N,N,N,N),
          FCVT_D_W -> List(Y,FCMD_CVT_FMT_W, Y,N,N,N,N,N,Y,N,Y,N,N,N,N),
          FCVT_D_WU-> List(Y,FCMD_CVT_FMT_WU,Y,N,N,N,N,N,Y,N,Y,N,N,N,N),
          FCVT_D_L -> List(Y,FCMD_CVT_FMT_L, Y,N,N,N,N,N,Y,N,Y,N,N,N,N),
          FCVT_D_LU-> List(Y,FCMD_CVT_FMT_LU,Y,N,N,N,N,N,Y,N,Y,N,N,N,N),
          MFTX_S   -> List(Y,FCMD_MFTX,      N,N,Y,N,N,Y,N,Y,N,N,N,N,N),
          MFTX_D   -> List(Y,FCMD_MFTX,      N,N,Y,N,N,N,N,Y,N,N,N,N,N),
          FCVT_W_S -> List(Y,FCMD_CVT_W_FMT, N,N,Y,N,N,Y,N,Y,N,N,N,N,N),
          FCVT_WU_S-> List(Y,FCMD_CVT_WU_FMT,N,N,Y,N,N,Y,N,Y,N,N,N,N,N),
          FCVT_L_S -> List(Y,FCMD_CVT_L_FMT, N,N,Y,N,N,Y,N,Y,N,N,N,N,N),
          FCVT_LU_S-> List(Y,FCMD_CVT_LU_FMT,N,N,Y,N,N,Y,N,Y,N,N,N,N,N),
          FCVT_W_D -> List(Y,FCMD_CVT_W_FMT, N,N,Y,N,N,N,N,Y,N,N,N,N,N),
          FCVT_WU_D-> List(Y,FCMD_CVT_WU_FMT,N,N,Y,N,N,N,N,Y,N,N,N,N,N),
          FCVT_L_D -> List(Y,FCMD_CVT_L_FMT, N,N,Y,N,N,N,N,Y,N,N,N,N,N),
          FCVT_LU_D-> List(Y,FCMD_CVT_LU_FMT,N,N,Y,N,N,N,N,Y,N,N,N,N,N),
          FEQ_S    -> List(Y,FCMD_EQ,        N,N,Y,Y,N,Y,N,Y,N,N,N,N,N),
          FLT_S    -> List(Y,FCMD_LT,        N,N,Y,Y,N,Y,N,Y,N,N,N,N,N),
          FLE_S    -> List(Y,FCMD_LE,        N,N,Y,Y,N,Y,N,Y,N,N,N,N,N),
          FEQ_D    -> List(Y,FCMD_EQ,        N,N,Y,Y,N,N,N,Y,N,N,N,N,N),
          FLT_D    -> List(Y,FCMD_LT,        N,N,Y,Y,N,N,N,Y,N,N,N,N,N),
          FLE_D    -> List(Y,FCMD_LE,        N,N,Y,Y,N,N,N,Y,N,N,N,N,N),
          MTFSR    -> List(Y,FCMD_MTFSR,     N,N,N,N,N,Y,N,Y,N,N,N,Y,Y),
          MFFSR    -> List(Y,FCMD_MFFSR,     N,N,N,N,N,Y,N,Y,N,N,N,Y,N)
//          FADD_S   -> List(Y,FCMD_ADD,       Y,Y,Y,Y,N,Y,N,N,N,Y,N,N,N),
//          FSUB_S   -> List(Y,FCMD_SUB,       Y,Y,Y,Y,N,Y,N,N,N,Y,N,N,N),
//          FMUL_S   -> List(Y,FCMD_MUL,       Y,Y,Y,Y,N,Y,N,N,N,Y,N,N,N),
//          FMIN_S   -> List(Y,FCMD_MIN,       Y,N,Y,Y,N,Y,N,N,Y,N,N,N,N),
//          FMAX_S   -> List(Y,FCMD_MAX,       Y,N,Y,Y,N,Y,N,N,Y,N,N,N,N)
          ))
  val valid :: cmd :: wen :: sboard :: ren1 :: ren2 :: ren3 :: single :: fromint :: toint :: fastpipe :: fma :: store :: rdfsr :: wrfsr :: Nil = decoder

  io.sigs.valid := valid.toBool
  io.sigs.cmd := cmd
  io.sigs.wen := wen.toBool
  io.sigs.sboard := sboard.toBool
  io.sigs.ren1 := ren1.toBool
  io.sigs.ren2 := ren2.toBool
  io.sigs.ren3 := ren3.toBool
  io.sigs.single := single.toBool
  io.sigs.fromint := fromint.toBool
  io.sigs.toint := toint.toBool
  io.sigs.fastpipe := fastpipe.toBool
  io.sigs.fma := fma.toBool
  io.sigs.store := store.toBool
  io.sigs.rdfsr := rdfsr.toBool
  io.sigs.wrfsr := wrfsr.toBool
}

class ioDpathFPU extends Bundle {
  val inst = Bits(32, OUTPUT)
  val fromint_data = Bits(64, OUTPUT)

  val store_data = Bits(64, INPUT)
  val toint_data = Bits(64, INPUT)

  val dmem_resp_val = Bool(OUTPUT)
  val dmem_resp_type = Bits(3, OUTPUT)
  val dmem_resp_tag = UFix(5, OUTPUT)
  val dmem_resp_data = Bits(64, OUTPUT)
}

class ioCtrlFPU extends Bundle {
  val valid = Bool(OUTPUT)
  val nack = Bool(INPUT)
  val illegal_rm = Bool(INPUT)
  val killx = Bool(OUTPUT)
  val killm = Bool(OUTPUT)
  val dec = new rocketFPUCtrlSigs().asInput
}

class rocketFPIntUnit extends Component
{
  val io = new Bundle {
    val single = Bool(INPUT)
    val cmd = Bits(FCMD_WIDTH, INPUT)
    val fsr = Bits(FSR_WIDTH, INPUT)
    val in1 = Bits(65, INPUT)
    val in2 = Bits(65, INPUT)
    val store_data = Bits(64, OUTPUT)
    val toint_data = Bits(64, OUTPUT)
    val exc = Bits(5, OUTPUT)
  }

  val unrec_s = new hardfloat.recodedFloat32ToFloat32
  val unrec_d = new hardfloat.recodedFloat64ToFloat64
  unrec_s.io.in := io.in1
  unrec_d.io.in := io.in1

  io.store_data := Mux(io.single, Cat(unrec_s.io.out, unrec_s.io.out), unrec_d.io.out)

  val scmp = new hardfloat.recodedFloat32Compare
  scmp.io.a := io.in1
  scmp.io.b := io.in2
  val scmp_out = (io.cmd(1,0) & Cat(scmp.io.a_lt_b, scmp.io.a_eq_b)).orR
  val scmp_exc = (io.cmd(1,0) & Cat(scmp.io.a_lt_b_invalid, scmp.io.a_eq_b_invalid)).orR << UFix(4)

  val s2i = new hardfloat.recodedFloat32ToAny
  s2i.io.in := io.in1
  s2i.io.roundingMode := io.fsr >> UFix(5)
  s2i.io.typeOp := ~io.cmd(1,0)

  val dcmp = new hardfloat.recodedFloat64Compare
  dcmp.io.a := io.in1
  dcmp.io.b := io.in2
  val dcmp_out = (io.cmd(1,0) & Cat(dcmp.io.a_lt_b, dcmp.io.a_eq_b)).orR
  val dcmp_exc = (io.cmd(1,0) & Cat(dcmp.io.a_lt_b_invalid, dcmp.io.a_eq_b_invalid)).orR << UFix(4)

  val d2i = new hardfloat.recodedFloat64ToAny
  d2i.io.in := io.in1
  d2i.io.roundingMode := io.fsr >> UFix(5)
  d2i.io.typeOp := ~io.cmd(1,0)

  // output muxing
  val (out_s, exc_s) = (Wire() { Bits() }, Wire() { Bits() })
  out_s := Cat(Fill(32, unrec_s.io.out(31)), unrec_s.io.out)
  exc_s := Bits(0)
  val (out_d, exc_d) = (Wire() { Bits() }, Wire() { Bits() })
  out_d := unrec_d.io.out
  exc_d := Bits(0)

  when (io.cmd === FCMD_MTFSR || io.cmd === FCMD_MFFSR) {
    out_s := io.fsr
  }
  when (io.cmd === FCMD_CVT_W_FMT || io.cmd === FCMD_CVT_WU_FMT) {
    out_s := Cat(Fill(32, s2i.io.out(31)), s2i.io.out(31,0))
    exc_s := s2i.io.exceptionFlags
    out_d := Cat(Fill(32, d2i.io.out(31)), d2i.io.out(31,0))
    exc_d := d2i.io.exceptionFlags
  }
  when (io.cmd === FCMD_CVT_L_FMT || io.cmd === FCMD_CVT_LU_FMT) {
    out_s := s2i.io.out
    exc_s := s2i.io.exceptionFlags
    out_d := d2i.io.out
    exc_d := d2i.io.exceptionFlags
  }
  when (io.cmd === FCMD_EQ || io.cmd === FCMD_LT || io.cmd === FCMD_LE) {
    out_s := scmp_out
    exc_s := scmp_exc
    out_d := dcmp_out
    exc_d := dcmp_exc
  }

  io.toint_data := Mux(io.single, out_s, out_d)
  io.exc := Mux(io.single, exc_s, exc_d)
}

class rocketIntFPUnit extends Component
{
  val io = new Bundle {
    val single = Bool(INPUT)
    val cmd = Bits(FCMD_WIDTH, INPUT)
    val fsr = Bits(FSR_WIDTH, INPUT)
    val in = Bits(64, INPUT)
    val out = Bits(65, OUTPUT)
    val exc = Bits(5, OUTPUT)
  }

  val rec_s = new hardfloat.float32ToRecodedFloat32
  val rec_d = new hardfloat.float64ToRecodedFloat64
  rec_s.io.in := io.in
  rec_d.io.in := io.in

  val i2s = new hardfloat.anyToRecodedFloat32
  i2s.io.in := io.in
  i2s.io.roundingMode := io.fsr >> UFix(5)
  i2s.io.typeOp := ~io.cmd(1,0)

  val i2d = new hardfloat.anyToRecodedFloat64
  i2d.io.in := io.in
  i2d.io.roundingMode := io.fsr >> UFix(5)
  i2d.io.typeOp := ~io.cmd(1,0)

  // output muxing
  val (out_s, exc_s) = (Wire() { Bits() }, Wire() { Bits() })
  out_s := rec_s.io.out
  exc_s := Bits(0)
  val (out_d, exc_d) = (Wire() { Bits() }, Wire() { Bits() })
  out_d := rec_d.io.out
  exc_d := Bits(0)

  when (io.cmd === FCMD_MTFSR || io.cmd === FCMD_MFFSR) {
    out_s := io.in(FSR_WIDTH-1,0)
  }
  when (io.cmd === FCMD_CVT_FMT_W || io.cmd === FCMD_CVT_FMT_WU ||
        io.cmd === FCMD_CVT_FMT_L || io.cmd === FCMD_CVT_FMT_LU) {
    out_s := i2s.io.out
    exc_s := i2s.io.exceptionFlags
    out_d := i2d.io.out
    exc_d := i2d.io.exceptionFlags
  }

  io.out := Mux(io.single, Cat(Fill(32,UFix(1)), out_s), out_d)
  io.exc := Mux(io.single, exc_s, exc_d)
}

class rocketFPU(sfma_latency: Int, dfma_latency: Int) extends Component
{
  val io = new Bundle {
    val ctrl = new ioCtrlFPU().flip()
    val dpath = new ioDpathFPU().flip()
  }

  val reg_inst = Reg() { Bits() }
  when (io.ctrl.valid) {
    reg_inst := io.dpath.inst
  }
  val reg_valid = Reg(io.ctrl.valid, Bool(false))

  val fp_decoder = new rocketFPUDecoder
  fp_decoder.io.inst := io.dpath.inst

  val ctrl = Reg() { new rocketFPUCtrlSigs }
  when (io.ctrl.valid) {
    ctrl := fp_decoder.io.sigs
  }

  // load response
  val load_wb = Reg(io.dpath.dmem_resp_val, resetVal = Bool(false))
  val load_wb_single = Reg() { Bool() }
  val load_wb_data = Reg() { Bits(width = 64) } // XXX WTF why doesn't bit width inference work for the regfile?!
  val load_wb_tag = Reg() { UFix() }
  when (io.dpath.dmem_resp_val) {
    load_wb_single := io.dpath.dmem_resp_type === MT_W || io.dpath.dmem_resp_type === MT_WU
    load_wb_data := io.dpath.dmem_resp_data
    load_wb_tag := io.dpath.dmem_resp_tag
  }
  val rec_s = new hardfloat.float32ToRecodedFloat32
  val rec_d = new hardfloat.float64ToRecodedFloat64
  rec_s.io.in := load_wb_data
  rec_d.io.in := load_wb_data
  val load_wb_data_recoded = Mux(load_wb_single, Cat(Fill(32,UFix(1)), rec_s.io.out), rec_d.io.out)

  val fsr_rm = Reg() { Bits(width = 3) }
  val fsr_exc = Reg() { Bits(width = 5) }

  // regfile
  val regfile = Mem(32, load_wb, load_wb_tag, load_wb_data_recoded);
  regfile.setReadLatency(0);
  regfile.setTarget('inst);

  val ex_rs1 = regfile.read(reg_inst(26,22))
  val ex_rs2 = regfile.read(reg_inst(21,17))
  val ex_rs3 = regfile.read(reg_inst(16,12))

  val fp_fromint_val = Reg(resetVal = Bool(false))
  val fp_fromint_data = Reg() { Bits() }
  val fp_toint_val = Reg(resetVal = Bool(false))
  val fp_toint_data = Reg() { Bits() }
  val fp_wrfsr_val = Reg(resetVal = Bool(false))
  val fp_cmp_data = Reg() { Bits() }
  val fp_toint_single = Reg() { Bool() }
  val fp_toint_cmd = Reg() { Bits() }
  val fp_waddr = Reg() { Bits() }

  fp_fromint_val := Bool(false)
  fp_toint_val := Bool(false)
  fp_wrfsr_val := Bool(false)
  when (reg_valid) {
    fp_waddr := reg_inst(31,27)
    when (ctrl.fromint || ctrl.wrfsr) {
      fp_fromint_val := !io.ctrl.killx
      fp_fromint_data := io.dpath.fromint_data
    }
    when (ctrl.wrfsr) {
      fp_wrfsr_val := !io.ctrl.killx
    }
    when (ctrl.toint) {
      fp_toint_val := !io.ctrl.killx
    }
    when (ctrl.toint || ctrl.fastpipe) {
      fp_toint_data := ex_rs1
      when (ctrl.ren2) {
        fp_cmp_data := ex_rs2
      }
    }
    when (ctrl.store) {
      fp_toint_data := ex_rs2
    }
    when (ctrl.toint || ctrl.store) {
      fp_toint_single := ctrl.single
      fp_toint_cmd := ctrl.cmd
    }
  }

  // currently we assume FP stores and FP->int ops take 1 cycle (MEM)
  val fpiu = new rocketFPIntUnit
  fpiu.io.single := ctrl.single
  fpiu.io.cmd := ctrl.cmd
  fpiu.io.fsr := Cat(fsr_rm, fsr_exc)
  fpiu.io.in1 := fp_toint_data
  fpiu.io.in2 := fp_cmp_data

  io.dpath.store_data := fpiu.io.store_data
  io.dpath.toint_data := fpiu.io.toint_data

  val ifpu = new rocketIntFPUnit
  ifpu.io.single := ctrl.single
  ifpu.io.cmd := ctrl.cmd
  ifpu.io.fsr := Cat(fsr_rm, fsr_exc)
  ifpu.io.in := fp_fromint_data

  val retire_wrfsr = Reg(!io.ctrl.killm && fp_wrfsr_val, resetVal = Bool(false))
  val retire_toint = Reg(!io.ctrl.killm && fp_toint_val, resetVal = Bool(false))
  val retire_toint_exc = Reg(fpiu.io.exc)
  val retire_fromint = Reg(!io.ctrl.killm && fp_fromint_val, resetVal = Bool(false))
  val retire_fromint_exc = Reg(ifpu.io.exc)
  val retire_fromint_wdata = Reg(ifpu.io.out)
  val retire_fromint_waddr = Reg(fp_waddr)

  when (retire_toint || retire_fromint) {
    fsr_exc := fsr_exc |
      Fill(fsr_exc.getWidth, retire_toint) & retire_toint_exc |
      Fill(fsr_exc.getWidth, retire_fromint) & retire_fromint_exc
  }
  when (retire_wrfsr) { // MTFSR
    fsr_exc := retire_fromint_wdata(4,0)
    fsr_rm := retire_fromint_wdata(7,5)
  }

  // writeback arbitration
  val wen = Reg(resetVal = Bits(0, dfma_latency-1))
  val winfo = Vec(dfma_latency-1) { Reg() { Bits() } }
  val mem_stage_wen = Reg(resetVal = Bool(false))

  val fastpipe_latency = 2
  require(fastpipe_latency < sfma_latency && sfma_latency <= dfma_latency)
  val ex_stage_fu_latency = Mux(ctrl.fastpipe, UFix(fastpipe_latency-1),
                            Mux(ctrl.single, UFix(sfma_latency-1),
                                UFix(dfma_latency-1)))
  val mem_stage_fu_latency = Reg(ex_stage_fu_latency - UFix(1))
  val write_port_busy = ctrl.fastpipe && wen(fastpipe_latency-1) ||
    Bool(sfma_latency < dfma_latency) && ctrl.fma && ctrl.single && wen(sfma_latency-1) ||
    mem_stage_wen && mem_stage_fu_latency === ex_stage_fu_latency
  mem_stage_wen := reg_valid && !io.ctrl.killx && (ctrl.fma || ctrl.fastpipe)
  val ex_stage_wsrc = Cat(ctrl.fastpipe, ctrl.single)
  val mem_stage_winfo = Reg(Cat(reg_inst(31,27), ex_stage_wsrc))

  for (i <- 0 until dfma_latency-2) {
    winfo(i) := winfo(i+1)
  }
  when (mem_stage_wen) {
    when (!io.ctrl.killm) {
      wen := (wen >> UFix(1)) | (UFix(1) << mem_stage_fu_latency)
    }
    for (i <- 0 until dfma_latency-1) {
      when (UFix(i) === mem_stage_fu_latency) {
        winfo(i) := mem_stage_winfo
      }
    }
  }
  .otherwise {
    wen := wen >> UFix(1)
  }

  val wsrc = winfo(0)(1,0)
  val wdata = Mux(wsrc === UFix(0), UFix(0), // DFMA
              Mux(wsrc === UFix(1), UFix(0), // SFMA
              Mux(wsrc === UFix(2), retire_fromint_wdata,
                  retire_fromint_wdata)))
  val waddr = winfo(0) >> UFix(2)
  regfile.write(waddr, wdata, wen(0))

  val fp_inflight = fp_toint_val || retire_toint || mem_stage_wen || wen.orR
  val fsr_busy = ctrl.rdfsr && fp_inflight || fp_wrfsr_val || retire_wrfsr
  val units_busy = Bool(false)
  io.ctrl.nack := fsr_busy || units_busy || write_port_busy
  io.ctrl.dec <> fp_decoder.io.sigs
  // we don't currently support round-max-magnitude (rm=4)
  io.ctrl.illegal_rm := Mux(reg_inst(11,9) === Bits(7), fsr_rm(2), reg_inst(11))
}
