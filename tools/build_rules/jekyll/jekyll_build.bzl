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

# This is a non-hermetic rule to build and serve a Jekyll website. Unfortunately, Jekyll is only
# distributed as a Gem which means we do not have a standalone binary easily available.
# Note: We loose here the native possibility of Jekyll to watch the directory.

def _jekyll_build_impl(ctx):
  # 1. Copy every source file into a single directory. We mimic the exact structure, and hence
  # have to take care of creating all parent directories
  source_dir = ctx.actions.declare_directory(ctx.attr.name + "-src")
  commands = []
  prefix_len = len(ctx.label.package) + 1
  for file in ctx.files.srcs:
    command = "mkdir -p {}/{}".format(source_dir.path, file.dirname[prefix_len:])
    if command not in commands:
      commands.append(command)
    commands.append("cp {} {}/{}".format(file.path, source_dir.path, file.path[prefix_len:]))
  ctx.actions.run_shell(
    command = " && ".join(commands),
    inputs = depset(items = ctx.files.srcs),
    outputs = [source_dir],
    progress_message = "Copying sources",
    mnemonic = "CopySrcs",
  )

  # 2. Use a non-hermetic Jekyll executable to build the website. This output will be used as
  # runfiles when executing the target, and packaged as a deployable archive.
  dist_dir = ctx.actions.declare_directory(ctx.attr.name + "-dist")
  ctx.actions.run(
    executable = "jekyll",
    arguments = ["build", "-q", "-s", source_dir.path, "-d", dist_dir.path],
    inputs = [source_dir],
    outputs = [dist_dir],
    use_default_shell_env = True,
    progress_message = "Building with jekyll",
    mnemonic = "JekyllBuild",
  )

  # 3. Create an executable script serving the previously built website using our minimalist
  # Go HTTP server.
  ctx.actions.expand_template(
    template = ctx.file._serve_tpl,
    output = ctx.outputs.executable,
    substitutions = {
      "TEMPLATED_dist_dir": dist_dir.short_path,
      "TEMPLATED_httpserver": ctx.file._httpserver.path,
    },
    is_executable = True,
  )

  # Build a deployable .tar.gz archive. This is *not* part of the default outputs.
  ctx.actions.run(
    executable = "tar",
    arguments = ["-czhf", ctx.outputs.targz.path, "-C", dist_dir.path, "."],
    inputs = [dist_dir],
    outputs = [ctx.outputs.targz],
    progress_message = "Creating deployable .tar.gz archive",
    mnemonic = "TarGz",
  )

  return [DefaultInfo(
    files=depset(),
    runfiles=ctx.runfiles(files=[dist_dir, ctx.file._httpserver]),
  )]

jekyll_build = rule(
  implementation = _jekyll_build_impl,
  executable = True,
  attrs = {
    "srcs": attr.label_list(allow_files=True, allow_empty=False),
    "_serve_tpl": attr.label(default=":serve.sh", allow_files=True, single_file=True),
    "_httpserver": attr.label(default=":httpserver", allow_files=True, single_file=True),
  },
  outputs = {
    "targz": "%{name}_deploy.tar.gz",
  },
)
"""Build a Jekyll website.

Args:
  srcs: Source files the website is composed of. It should include the _config.yml file.

Outputs:
  %{name}: Locally served website.
  %{name}_deploy.tar.gz: Archive containing a deployable website.
"""
