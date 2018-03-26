#!/usr/bin/env bash
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

# This file has been largely copy/pasted from Kubernetes, published under the Apache License 2.0.
# Cf. https://github.com/kubernetes/kubernetes

set -o errexit
set -o nounset
set -o pipefail

ROOTDIR=$(dirname "${BASH_SOURCE}")/..

git=(git --work-tree "${ROOTDIR}")

if GIT_COMMIT=$("${git[@]}" rev-parse "HEAD^{commit}" 2>/dev/null); then
  # Check if the tree is dirty. Default to dirty
  if git_status=$("${git[@]}" status --porcelain 2>/dev/null) && [[ -z ${git_status} ]]; then
    GIT_TREE_STATE="clean"
  else
    GIT_TREE_STATE="dirty"
  fi

  # Use git describe to find the version based on annotated tags.
  if GIT_VERSION=$("${git[@]}" describe --tags --abbrev=14 "${GIT_COMMIT}^{commit}" 2>/dev/null); then
    # This translates the "git describe" to an actual semver.org compatible semantic version that
    # looks something like this: v1.1.0-alpha.0.6+84c76d1142ea4d
    DASHES_IN_VERSION=$(echo "${GIT_VERSION}" | sed "s/[^-]//g")
    if [[ "${DASHES_IN_VERSION}" == "---" ]] ; then
      # We have distance to subversion (v1.1.0-subversion-1-gCommitHash)
      GIT_VERSION=$(echo "${GIT_VERSION}" | sed "s/-\([0-9]\{1,\}\)-g\([0-9a-f]\{14\}\)$/.\1\+\2/")
    elif [[ "${DASHES_IN_VERSION}" == "--" ]] ; then
      # We have distance to base tag (v1.1.0-1-gCommitHash)
      GIT_VERSION=$(echo "${GIT_VERSION}" | sed "s/-g\([0-9a-f]\{14\}\)$/+\1/")
    fi
    if [[ "${GIT_TREE_STATE}" == "dirty" ]]; then
      # git describe --dirty only considers changes to existing files, but
      # that is problematic since new untracked .go files affect the build,
      # so use our idea of "dirty" from git status instead.
      GIT_VERSION+="-dirty"
    fi

    # Try to match the "git describe" output to a regex to try to extract
    # the "major" and "minor" versions and whether this is the exact tagged
    # version or whether the tree is between two tagged versions.
    if [[ "${GIT_VERSION}" =~ ^v([0-9]+)\.([0-9]+)(\.[0-9]+)?([-].*)?$ ]]; then
      GIT_MAJOR=${BASH_REMATCH[1]}
      GIT_MINOR=${BASH_REMATCH[2]}
      if [[ -n "${BASH_REMATCH[4]}" ]]; then
        GIT_MINOR+="+"
      fi
    fi
  fi
fi

echo GIT_COMMIT ${GIT_COMMIT-}
echo GIT_TREE_STATE ${GIT_TREE_STATE-}
echo GIT_VERSION ${GIT_VERSION-}
echo GIT_MAJOR ${GIT_MAJOR-}
echo GIT_MINOR ${GIT_MINOR-}
