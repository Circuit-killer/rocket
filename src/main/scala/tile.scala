// See LICENSE for license details.

package rocket

import Chisel._
import uncore._
import Util._

case object CoreName extends Field[String]
case object NDCachePorts extends Field[Int]
case object NPTWPorts extends Field[Int]
case object BuildRoCC extends Field[Option[() => RoCC]]

abstract class Tile(resetSignal: Bool = null) extends Module(_reset = resetSignal) {
  val io = new Bundle {
    val cached = new ClientTileLinkIO
    val uncached = new ClientUncachedTileLinkIO
    val net = new TileLinkNetworkIO
    val host = new HTIFIO
  }
}

class RocketTile(resetSignal: Bool = null) extends Tile(resetSignal) {
  val icache = Module(new Frontend, { case CacheName => "L1I"; case CoreName => "Rocket" })
  val dcache = Module(new HellaCache, { case CacheName => "L1D" })
  val ptw = Module(new PTW(params(NPTWPorts)))
  val core = Module(new Core, { case CoreName => "Rocket" })

  dcache.io.cpu.invalidate_lr := core.io.dmem.invalidate_lr // Bypass signal to dcache
  val dcArb = Module(new HellaCacheArbiter(params(NDCachePorts)))
  dcArb.io.requestor(0) <> ptw.io.mem
  dcArb.io.requestor(1) <> core.io.dmem
  dcArb.io.mem <> dcache.io.cpu

  ptw.io.requestor(0) <> icache.io.ptw
  ptw.io.requestor(1) <> dcache.io.ptw

  core.io.host <> io.host
  core.io.imem <> icache.io.cpu
  core.io.ptw(0) <> ptw.io.dpath

  // Connect the caches and ROCC to the outer memory system
  io.cached <> dcache.io.mem
  // If so specified, build an RoCC module and wire it in
  // otherwise, just hookup the icache
  io.uncached <> params(BuildRoCC).map { buildItHere =>
    val rocc = buildItHere()
    val roccPtw = Module(new PTW(3))
    val memArb = Module(new ClientTileLinkIOArbiter(3))
    val dcIF = Module(new SimpleHellaCacheIF)
    core.io.rocc <> rocc.io
    dcIF.io.requestor <> rocc.io.mem
    dcArb.io.requestor(2) <> dcIF.io.cache
    dcArb.io.requestor(3) <> roccPtw.io.mem
    core.io.ptw(1) <> roccPtw.io.dpath
    memArb.io.in(0) <> icache.io.mem
    memArb.io.in(1) <> rocc.io.imem
    memArb.io.in(2) <> rocc.io.dmem
    roccPtw.io.requestor(0) <> rocc.io.iptw
    roccPtw.io.requestor(1) <> rocc.io.dptw
    roccPtw.io.requestor(2) <> rocc.io.pptw
    rocc.io.net <> io.net
    rocc.io.host_id := io.host.id
    memArb.io.out
  }.getOrElse(icache.io.mem)
}
