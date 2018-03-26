# Accio is a platform to launch computer science experiments.
# Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
#
# Accio is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Accio is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Accio.  If not, see <http://www.gnu.org/licenses/>.

def _pkg_zip_impl(ctx):
  dir = ctx.actions.declare_directory(ctx.label.name, sibling=ctx.outputs.out)

  commands = []
  commands.append("mkdir -p " + dir.path)
  commands.extend(["cp " + dep.path + " " + dir.path + "/" + dep.basename for dep in ctx.files.srcs])
  commands.append("zip -rj " + ctx.outputs.out.path + " " + dir.path)
  ctx.actions.run_shell(
    #command = "zip -j " + ctx.outputs.out.path + " " + " ".join([dep.path for dep in ctx.files.srcs]),
    command = " && ".join(commands),
    inputs = ctx.files.srcs,
    outputs = [dir, ctx.outputs.out],
    progress_message = "Creating .zip archive",
    mnemonic = "Zip",
  )

pkg_zip = rule(
  implementation = _pkg_zip_impl,
  attrs = {
    "srcs": attr.label_list(allow_files=True),
  },
  outputs = {
    "out": "%{name}.zip",
  },
)
"""Create a zip archive. The archive will be created flat, i.e., all specified
files will be at the root of the archive, and the hierarchy will be lost.

Args:
  srcs: List of files to include within the archive. All files will be placed at the root of the
        generated archive, meaning any hierarchy will be lost (and files will the same name will
        be overwritten).

Outputs:
  %{name}.zip: A zip archive.
"""
