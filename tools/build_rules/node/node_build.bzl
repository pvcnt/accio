# PDD is a platform for privacy-preserving Web searches collection.
# Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
#
# PDD is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# PDD is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with PDD.  If not, see <http://www.gnu.org/licenses/>.

def _get_package_dir(ctx):
  return ctx.label.package

def _get_output_dir(ctx):
  # If it's an external label, output to workspace_root.
  if ctx.label.workspace_root:
    return ctx.bin_dir.path + '/' + ctx.label.workspace_root + '/' + _get_package_dir(ctx)
  return ctx.bin_dir.path + '/' + _get_package_dir(ctx)

def _get_input_dir(ctx):
    # If it's an external label, input is in workspace_root.
    if ctx.label.workspace_root:
        return ctx.label.workspace_root + '/' + _get_package_dir(ctx)
    return _get_package_dir(ctx)

def _node_build_impl(ctx):
  inputs = depset(
    items = ctx.files.srcs + [ctx.file._webpack_config, ctx.executable._webpack],
    transitive = [ctx.attr._node_modules[DefaultInfo].files] + [dep[DefaultInfo].files for dep in ctx.attr.deps],
  )

  # Dependencies listed under `deps` are treated differently than those listed under `srcs`,
  # because they are considered as modules of their own. The common node_modules are always
  # registered.
  node_path = ["3rdparty/node_modules"]
  for dep in ctx.attr.deps:
    if dep.label.package not in node_path:
      node_path.append(dep.label.package)

  # '_' prefixed environment variables are private, i.e., not exported to the application.
  env = dict(
    ctx.attr.env,
    _INPUT_DIR = _get_input_dir(ctx),
    _OUTPUT_DIR = _get_output_dir(ctx),
    _ENTRY = ",".join([file.path for file in ctx.files.entry]),
    _PATH = ",".join(node_path),
  )

  outputs = [getattr(ctx.outputs, _get_output_key(entry.label)) for entry in ctx.attr.entry]
  ctx.actions.run_shell(
    command = ctx.executable._webpack.path + " --config " + ctx.file._webpack_config.path,
    inputs = inputs,
    outputs = outputs,
    progress_message = "Building with webpack",
    mnemonic = "WebpackBuild",
    env = env,
  )

def _get_output_key(label):
  basename = label.name.split('/')[-1]
  return basename[:-3]

def _get_outputs(entry):
  return {_get_output_key(label): label.name[:-3] + ".bundle.js" for label in entry}

node_build = rule(
  implementation = _node_build_impl,
  attrs = {
    "srcs": attr.label_list(allow_files=[".js", ".jsx", ".css"]),
    "deps": attr.label_list(providers=[DefaultInfo]),
    "entry": attr.label_list(allow_files=[".js"], allow_empty=False),
    "env": attr.string_dict(),
    "_node_modules": attr.label(default="//3rdparty:node_modules"),
    "_webpack": attr.label(
      default="//tools/bin:webpack",
      executable=True,
      cfg="host"
    ),
    "_webpack_config": attr.label(
      default="//tools/build_rules/node:webpack.config.js",
      allow_files=True,
      single_file=True,
    ),
  },
  outputs = _get_outputs,
)
