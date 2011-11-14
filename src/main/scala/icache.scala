package Top {

import Chisel._;
import Node._;
import Constants._;
import scala.math._;

// interface between I$ and pipeline/ITLB (32 bits wide)
class ioImem(view: List[String] = null) extends Bundle (view)
{
  val invalidate = Bool('input);
  val itlb_miss  = Bool('input);
  val req_val   = Bool('input);
  val req_rdy   = Bool('output);
  val req_idx   = Bits(PGIDX_BITS, 'input);
  val req_ppn   = Bits(PPN_BITS, 'input);
  val resp_data = Bits(32, 'output);
  val resp_val  = Bool('output);
}

// interface between I$ and memory (128 bits wide)
class ioIcache(view: List[String] = null) extends Bundle (view)
{
  val req_addr  = UFix(PADDR_BITS, 'input);
  val req_val   = Bool('input);
  val req_rdy   = Bool('output);
  val resp_data = Bits(128, 'output);
  val resp_val  = Bool('output);
}

class ioICacheDM extends Bundle()
{
  val cpu = new ioImem();
  val mem = new ioIcache().flip();
}

// single port SRAM i/o
class ioSRAMsp (width: Int, addrbits: Int) extends Bundle {
  val a  = UFix(addrbits, 'input);  // address
  val d  = Bits(width, 'input);     // data input
  val bweb = Bits(width, 'input);   // bit write enable mask
  val ce = Bool('input);            // chip enable
  val we = Bool('input);            // write enable
  val q  = Bits(width, 'output);    // data out
}

// single ported SRAM
class rocketSRAMsp(entries: Int, width: Int) extends Component {
  val addrbits = ceil(log10(entries)/log10(2)).toInt;
  val io = new ioSRAMsp(width, addrbits);
  val sram = Mem(entries, io.we, io.a, io.d, wrMask = io.bweb, resetVal = null);
  val rdata = Reg(Mux(io.ce, sram.read(io.a), Bits(0,width)));
  io.q := rdata;
}

// basic direct mapped instruction cache
// 32 bit wide cpu port, 128 bit wide memory port, 64 byte cachelines
// parameters :
//    lines = # cache lines
class rocketICacheDM(lines: Int) extends Component {
  val io = new ioICacheDM();

  val addrbits = PADDR_BITS;
  val indexbits = ceil(log10(lines)/log10(2)).toInt;
  val offsetbits = 6;
  val tagmsb    = addrbits - 1;
  val taglsb    = indexbits+offsetbits;
  val tagbits   = addrbits-taglsb;
  val indexmsb  = taglsb-1;
  val indexlsb  = offsetbits;
  val offsetmsb = indexlsb-1;
  val offsetlsb = 2;
  val databits = 32;
  
  val s_reset :: s_ready :: s_request :: s_refill_wait :: s_refill :: s_resolve_miss :: Nil = Enum(6) { UFix() };
  val state = Reg(resetVal = s_reset);
  
  val r_cpu_req_idx    = Reg(resetVal = Bits(0, PGIDX_BITS)); 
  val r_cpu_req_ppn    = Reg(resetVal = Bits(0, PPN_BITS)); 
  val r_cpu_req_val    = Reg(resetVal = Bool(false));
  
  when (io.cpu.req_val && io.cpu.req_rdy) {
    r_cpu_req_idx   <== io.cpu.req_idx;
  }
  when (state === s_ready && r_cpu_req_val && !io.cpu.itlb_miss) {
    r_cpu_req_ppn <== io.cpu.req_ppn;
  }
  when (io.cpu.req_rdy) {
    r_cpu_req_val <== io.cpu.req_val; 
  }
  otherwise {
    r_cpu_req_val <== Bool(false);
  }

  // refill counter
  val refill_count = Reg(resetVal = UFix(0,2));
  when (io.mem.resp_val) {
    refill_count <== refill_count + UFix(1);
  }
  
  // tag array
  val tag_array = new rocketSRAMsp(lines, tagbits);
  val tag_addr = 
    Mux((state === s_refill_wait), r_cpu_req_idx(PGIDX_BITS-1,offsetbits),
      io.cpu.req_idx(PGIDX_BITS-1,offsetbits)).toUFix;
  val tag_we = (state === s_refill_wait) && io.mem.resp_val;
  tag_array.io.a := tag_addr;
  tag_array.io.d    := r_cpu_req_ppn;
  tag_array.io.we   := tag_we;
  tag_array.io.bweb := ~Bits(0,tagbits);
  tag_array.io.ce   := (io.cpu.req_val && io.cpu.req_rdy);
  val tag_rdata = tag_array.io.q;
  
  // valid bit array
  val vb_array = Reg(resetVal = Bits(0, lines));
  when (io.cpu.invalidate) {
    vb_array <== Bits(0,lines);
  }
  when (tag_we) {
    vb_array <== vb_array.bitSet(r_cpu_req_idx(PGIDX_BITS-1,offsetbits).toUFix, UFix(1,1));
  }

  val tag_valid = Reg(vb_array(tag_addr)).toBool;
  val tag_match = (tag_rdata === io.cpu.req_ppn);
  
  // data array
  val data_array = new rocketSRAMsp(lines*4, 128);
  data_array.io.a := 
    Mux((state === s_refill_wait) || (state === s_refill),  Cat(r_cpu_req_idx(PGIDX_BITS-1, offsetbits), refill_count),
      io.cpu.req_idx(PGIDX_BITS-1, offsetmsb-1)).toUFix;
  data_array.io.d    := io.mem.resp_data;
  data_array.io.we   := io.mem.resp_val;
  data_array.io.bweb := ~Bits(0,128);
  data_array.io.ce   := (io.cpu.req_rdy && io.cpu.req_val) || (state === s_resolve_miss);
    
  val data_array_rdata = data_array.io.q;
   
  // output signals
  io.cpu.resp_val := !io.cpu.itlb_miss && (state === s_ready) && r_cpu_req_val && tag_valid && tag_match; 
  io.cpu.req_rdy  := !io.cpu.itlb_miss && (state === s_ready) && (!r_cpu_req_val || (tag_valid && tag_match));
  io.cpu.resp_data :=
    MuxLookup(r_cpu_req_idx(offsetmsb-2, offsetlsb).toUFix, data_array_rdata(127, 96), 
      Array(UFix(2) -> data_array_rdata(95,64),
      UFix(1) -> data_array_rdata(63,32),
      UFix(0) -> data_array_rdata(31,0)));

  io.mem.req_val := (state === s_request);
  io.mem.req_addr := Cat(r_cpu_req_ppn, r_cpu_req_idx(PGIDX_BITS-1, offsetbits), Bits(0,2)).toUFix;

  // control state machine
  switch (state) {
    is (s_reset) {
      state <== s_ready;
    }
    is (s_ready) {
      when (io.cpu.itlb_miss) {
        state <== s_ready;
      }
      when (r_cpu_req_val && !(tag_valid && tag_match)) {
        state <== s_request;
      }
    }
    is (s_request)
    {
      when (io.mem.req_rdy) {
        state <== s_refill_wait;
      }
    }
    is (s_refill_wait) {
      when (io.mem.resp_val) {
        state <== s_refill;
      }
    }
    is (s_refill) {
      when (io.mem.resp_val && (refill_count === UFix(3,2))) {
        state <== s_resolve_miss;
      }
    }
    is (s_resolve_miss) {
      state <== s_ready;
    }
  }  
}

}