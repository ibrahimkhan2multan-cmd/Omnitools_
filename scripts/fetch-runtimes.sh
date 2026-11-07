#!/usr/bin/env bash
# Downloads the offline language runtimes used by the Code Runner and puts them in app/src/main/assets/rt.
# They are bundled into the APK, so the app itself never needs the internet. Run by CI before the build.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets/rt"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

PYODIDE=314.0.7
SQLJS=1.15.2
FENGARI=0.1.4
SUCRASE=3.35.1
ESBUILD=0.28.2

mkdir -p "$OUT"
cd "$WORK"
npm pack --silent "pyodide@$PYODIDE" "sql.js@$SQLJS" "fengari-web@$FENGARI" >/dev/null
for f in pyodide sql.js fengari-web; do
  mkdir -p "x/$f"
  tar xzf "$f"-*.tgz -C "x/$f"
done

cp x/pyodide/package/{pyodide.js,pyodide.asm.mjs,pyodide.asm.wasm,python_stdlib.zip,pyodide-lock.json} "$OUT/"
cp x/sql.js/package/dist/{sql-wasm.js,sql-wasm.wasm} "$OUT/"
cp x/fengari-web/package/dist/fengari-web.js "$OUT/"

mkdir -p tsb && cd tsb
npm init -y >/dev/null
npm install --silent "sucrase@$SUCRASE" "esbuild@$ESBUILD"
npx esbuild node_modules/sucrase/dist/esm/index.js --bundle --format=iife --global-name=sucrase --minify --platform=browser --outfile="$OUT/sucrase.js"

for f in pyodide.js pyodide.asm.mjs pyodide.asm.wasm python_stdlib.zip pyodide-lock.json sql-wasm.js sql-wasm.wasm fengari-web.js sucrase.js; do
  test -s "$OUT/$f" || { echo "Missing runtime file: $f" >&2; exit 1; }
done
du -sh "$OUT"
