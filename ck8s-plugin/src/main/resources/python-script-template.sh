#!/usr/bin/env bash
set -eEuo pipefail

REQUIREMENTS=requirements.txt
PYPROJECT_TOML=pyproject.toml
# Set to non-empty string to use venv
VENV=%VENV%
# Set to non-empty string to reuse existing venv based on a script name
REUSE_VENV=%REUSE_VENV%
# Name of the venv to reuse
VENV_NAME=%VENV_NAME%
# Path to Python script to run
SCRIPT=%SCRIPT%
declare -a ARGS
# Add arguments to ARGS with ARGS+=('some argument')
%ARGS%
SCRIPT_DIR=$( cd -- "$( dirname -- "${SCRIPT}" )" &> /dev/null && pwd )

cd %WORK_DIR%

if [[ -n "${VENV}" ]]; then
  if [[ -n "${REUSE_VENV}" ]]; then
    if [[ -n "${VENV_NAME}" ]]; then
      VENV_DIR=${TMPDIR:-/tmp/}ck8s-plugin-venv/${VENV_NAME}
    else
      VENV_DIR=${SCRIPT_DIR}/.venv
    fi
    {
      echo "Waiting for lock on ${VENV_DIR}.lock"
      flock -x 3
      echo "Obtained lock on ${VENV_DIR}.lock"
      if [[ -d "${VENV_DIR}" ]]; then
        echo "Found existing venv at ${VENV_DIR}"
      else
        echo >&2 "Creating new reusable venv at ${VENV_DIR}"
        python3 -m venv "${VENV_DIR}"
      fi
    } 3>"${VENV_DIR}.lock"
    echo "Released lock on ${VENV_DIR}.lock"
  else
    VENV_DIR=$(mktemp -d)
    echo >&2 "Creating new venv at ${VENV_DIR}"
    trap 'rm -rf "$VENV_DIR"' EXIT
    python3 -m venv "${VENV_DIR}"
  fi
  . "${VENV_DIR}"/bin/activate
fi

python3 --version

if [[ -r "${SCRIPT_DIR}/${REQUIREMENTS}" || -r "${SCRIPT_DIR}/${PYPROJECT_TOML}" ]]; then
  if [[ -z "${VENV}" ]]; then
    echo >&2 "WARN: installing dependencies without VENV set"
  fi
  # Go into script dir to handle relative dependencies
  pushd "${SCRIPT_DIR}"
  if [[ -r "${SCRIPT_DIR}/${REQUIREMENTS}" ]]; then
    echo "Installing dependencies from ${SCRIPT_DIR}/${REQUIREMENTS}"
    if [[ -n "${REUSE_VENV}" ]]; then
      {
        echo "Waiting for lock on ${SCRIPT_DIR}.deps-lock"
        flock -x 3
        echo "Obtained lock on ${SCRIPT_DIR}.deps-lock"
        python3 -m pip install --quiet --requirement "${SCRIPT_DIR}/${REQUIREMENTS}"
      } 3>"${SCRIPT_DIR}.deps-lock"
      echo "Released lock on ${SCRIPT_DIR}.deps-lock"
    else
      python3 -m pip install --quiet --requirement "${SCRIPT_DIR}/${REQUIREMENTS}"
    fi
  else
    echo "Installing dependencies from ${SCRIPT_DIR}/${PYPROJECT_TOML}"
    if [[ -n "${REUSE_VENV}" ]]; then
      {
        echo "Waiting for lock on ${SCRIPT_DIR}.deps-lock"
        flock -x 3
        echo "Obtained lock on ${SCRIPT_DIR}.deps-lock"
        python3 -m pip install --quiet .
      } 3>"${SCRIPT_DIR}.deps-lock"
      echo "Released lock on ${SCRIPT_DIR}.deps-lock"
    else
      python3 -m pip install --quiet .
    fi
  fi
  popd
fi

python3 "${SCRIPT}" "${ARGS[@]}"
