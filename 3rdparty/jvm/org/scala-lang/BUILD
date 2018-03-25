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

VERSION = '2.11.8'

jar_library(name='reflect', jars=[jar('org.scala-lang', 'scala-reflect', VERSION)])
jar_library(name='compiler', jars=[jar('org.scala-lang', 'scala-compiler', VERSION)])
jar_library(name='scalap', jars=[jar('org.scala-lang', 'scalap', VERSION)])
jar_library(name='scalap-without-compiler', jars=[jar('org.scala-lang', 'scalap', VERSION, excludes=[exclude('org.scala-lang', 'scala-compiler')])])