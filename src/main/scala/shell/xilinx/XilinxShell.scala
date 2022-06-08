// See LICENSE for license details.
package sifive.fpgashells.shell.xilinx

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import sifive.fpgashells.clocks._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.shell._

class XDC(val name: String)
{
  private var constraints: Seq[() => String] = Nil
  protected def addConstraint(command: => String) { constraints = (() => command) +: constraints }
  ElaborationArtefacts.add(name, constraints.map(_()).reverse.mkString("\n") + "\n")

  def addBoardPin(io: IOPin, pin: String) {
    addConstraint(s"set_property BOARD_PIN {${pin}} ${io.sdcPin}")
  }
  def addPackagePin(io: IOPin, pin: String) {
    addConstraint(s"set_property PACKAGE_PIN {${pin}} ${io.sdcPin}")
  }
  def addIOStandard(io: IOPin, standard: String) {
    addConstraint(s"set_property IOSTANDARD {${standard}} ${io.sdcPin}")
  }
  def addPullup(io: IOPin) {
    addConstraint(s"set_property PULLUP {TRUE} ${io.sdcPin}")
  }
  def addIOB(io: IOPin) {
    //ERROR: [Common 17-679] Invalid object type, 'port', used with '-of_objects' switch. Supported types are: 'net, pblock, pin, timing path, constant path, bel, site, slr'.
    //GND VCC a2b a2b_1 a2b_2 a2b_3 a2b_4 bundleIn_0_dq_1_i_REG_1_reg bundleIn_0_dq_1_i_REG_reg bundleIn_0_rxd_a2b bundleOut_0_reset_i chiptop
    // dutGroup
    //dutGroup_auto_in_member_0_reset_i dutWrangler dutWrangler_auto_in_1_reset_i fpga_power_on harnessSysPLL mig mig_io_port_aresetn_i
    // plusarg_reader
    // resetIBUF sys_clock_ibufds
    if (io.isOutput) {
       addConstraint(s"set_property IOB {TRUE} [ get_cells -of_objects [ all_fanin -flat -startpoints_only ${io.sdcPin}]]")
     } else {
       addConstraint(s"set_property IOB {TRUE} [ get_cells -of_objects [ all_fanout -flat -endpoints_only ${io.sdcPin}]]")
    }

    //addConstraint(s"set_property IOB {TRUE} ${io.sdcPin}")
  }
  def addSlew(io: IOPin, speed: String) {
    addConstraint(s"set_property SLEW {${speed}} ${io.sdcPin}")
  }
  def addTermination(io: IOPin, kind: String) {
    addConstraint(s"set_property OFFCHIP_TERM {${kind}} ${io.sdcPin}")
  }
  def clockDedicatedRouteFalse(io: IOPin) {
    addConstraint(s"set_property CLOCK_DEDICATED_ROUTE {FALSE} [get_nets ${io.sdcPin}]")
  }
  def addDriveStrength(io: IOPin, drive: String) {
    addConstraint(s"set_property DRIVE {${drive}} ${io.sdcPin}")
  }
  def addIbufLowPower(io: IOPin, value: String) {
    addConstraint(s"set_property IBUF_LOW_PWR ${value} ${io.sdcPin}")
  }
}

abstract class XilinxShell()(implicit p: Parameters) extends IOShell
{
  val sdc = new SDC("shell.sdc")
  val xdc = new XDC("shell.xdc")
  def pllReset: ModuleValue[Bool]

  ElaborationArtefacts.add("shell.vivado.tcl",
    """set shell_vivado_tcl [file normalize [info script]]
      |set shell_vivado_idx [string last ".shell.vivado.tcl" $shell_vivado_tcl]
      |add_files -fileset [current_fileset -constrset] [string replace $shell_vivado_tcl $shell_vivado_idx 999 ".shell.sdc"]
      |add_files -fileset [current_fileset -constrset] [string replace $shell_vivado_tcl $shell_vivado_idx 999 ".shell.xdc"]
      |set extra_constr [string replace $shell_vivado_tcl $shell_vivado_idx 999 ".extra.shell.xdc"]
      |if [file exist $extra_constr] {
      |  add_files -fileset [current_fileset -constrset] [string replace $shell_vivado_tcl $shell_vivado_idx 999 ".extra.shell.xdc"]
      |}
      |""".stripMargin)

    //Including the extra .xdc file in this way is a bit of a hack since ElaborationArtefacts can't append, and this tcl will only read specific
    // files. The long term solution is to make an overlay that does nothing but include .xdc constraints
}

abstract class Series7Shell()(implicit p: Parameters) extends XilinxShell
{
  val pllFactory = new PLLFactory(this, 7, p => Module(new Series7MMCM(p)))
  override def designParameters = super.designParameters.alterPartial {
    case PLLFactoryKey => pllFactory
  }
}

abstract class UltraScaleShell()(implicit p: Parameters) extends XilinxShell
{
  val pllFactory = new PLLFactory(this, 7, p => Module(new Series7MMCM(p)))
  override def designParameters = super.designParameters.alterPartial {
    case PLLFactoryKey => pllFactory
  }
}
