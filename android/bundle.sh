#!/bin/bash
# 生成 Termux 完整环境包（Node.js + Claude Code + 依赖库）
# 用法: bash bundle.sh
# 输出: android/assets/termux-bundle.tar.gz

PREFIX=/data/data/com.termux/files/usr
OUTPUT="$(dirname "$0")/assets/termux-bundle.tar.gz"

echo "打包 Termux 环境..."
tar -c -C "$PREFIX" \
  bin/bash bin/node bin/npm bin/npx bin/claude bin/env bin/ln bin/cp bin/mv bin/rm \
  bin/mkdir bin/ls bin/cat bin/grep bin/which bin/id bin/whoami bin/uname \
  bin/clear bin/apt* bin/dpkg* bin/sh \
  lib/node_modules \
  lib/libstdc++.so* lib/libc++_shared.so lib/libcrypto.so* lib/libssl.so* \
  lib/libz.so* lib/libicu* lib/libuv.so* lib/libnghttp2.so* \
  lib/libc.so* lib/libdl.so* lib/libm.so* lib/libpthread.so* \
  lib/librt.so* lib/libresolv.so* lib/libcrypt.so* lib/libutil.so* \
  etc/ssl etc/apt etc/terminfo etc/profile etc/bash.bashrc \
  tmp 2>/dev/null | gzip -9 > "$OUTPUT"

echo "完成: $(ls -lh $OUTPUT | awk '{print $5}')"
