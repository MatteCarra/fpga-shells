// See LICENSE for license details.
package sifive.fpgashells.shell

import chisel3._
import chisel3.experimental.Analog

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.{BaseSubsystem, PeripheryBus, PeripheryBusKey}
import freechips.rocketchip.tilelink.TLBusWrapper
import freechips.rocketchip.interrupts.IntInwardNode
import freechips.rocketchip.diplomaticobjectmodel.logicaltree.LogicalTreeNode

import sifive.blocks.devices.i2c._
import sifive.fpgashells.shell.xilinx._

case class I2CShellInput(index: Int = 0) extends ShellInput
case class I2CDesignInput(node: BundleBridgeSource[I2CPort])(implicit val p: Parameters) extends DesignInput
case class I2COverlayOutput() extends OverlayOutput
trait I2CShellPlacer[Shell] extends ShellPlacer[I2CDesignInput, I2CShellInput, I2COverlayOutput]

case object I2COverlayKey extends Field[Seq[DesignPlacer[I2CDesignInput, I2CShellInput, I2COverlayOutput]]](Nil)

class ShellI2CPortIO extends Bundle {
  val scl = Analog(1.W)
  val sda = Analog(1.W)
}

abstract class I2CPlacedOverlay(
  val name: String, val di: I2CDesignInput, val si: I2CShellInput)
    extends IOPlacedOverlay[ShellI2CPortIO, I2CDesignInput, I2CShellInput, I2COverlayOutput]
{
  implicit val p = di.p

  def ioFactory = new ShellI2CPortIO

  val tli2cSink = shell { di.node.makeSink }

  def overlayOutput = I2COverlayOutput()
}
