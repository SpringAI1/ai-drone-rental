#!/bin/bash
# 修补 pinia 2.1.7 + vue-demi 0.14.x 在 uniapp 3.0.0-3081220230817001 下的构建兼容问题。
# 两个问题：
# 1. rollup 静态分析时无法解析 vue-demi 通过 `export *` re-export 的 hasInjectionContext
#    → 让 pinia 从 @vue/runtime-core 导入 hasInjectionContext
# 2. --legacy-peer-deps 导致 @vue/shared 被锁定到 3.2.47，与 @vue/runtime-core 3.3.13 不一致
#    → 把 @vue/shared 升级到 3.3.13（与 vue 主版本对齐）
set -e

# 问题 1：patch pinia.mjs
PINIA_TARGET="node_modules/pinia/dist/pinia.mjs"
if [ -f "$PINIA_TARGET" ] && grep -q "hasInjectionContext" "$PINIA_TARGET"; then
  if ! grep -q "from '@vue/runtime-core'" "$PINIA_TARGET"; then
    # 兼容 macOS（BSD sed）和 Linux（GNU sed）：用临时文件方式替换
    NEW_IMPORT="import { hasInjectionContext } from '@vue/runtime-core';\\nimport { inject, toRaw, watch, unref, markRaw, effectScope, ref, isVue2, isRef, isReactive, set, getCurrentScope, onScopeDispose, getCurrentInstance, reactive, toRef, del, nextTick, computed, toRefs } from 'vue-demi';"
    OLD_IMPORT="import { hasInjectionContext, inject, toRaw, watch, unref, markRaw, effectScope, ref, isVue2, isRef, isReactive, set, getCurrentScope, onScopeDispose, getCurrentInstance, reactive, toRef, del, nextTick, computed, toRefs } from 'vue-demi';"
    # 用 node 做替换，避免 sed 跨平台问题
    node -e "
      const fs = require('fs');
      const f = process.argv[1];
      let s = fs.readFileSync(f, 'utf8');
      s = s.replace(process.argv[2], process.argv[3]);
      fs.writeFileSync(f, s);
    " "$PINIA_TARGET" "$OLD_IMPORT" "$(printf '%s\n%s' "import { hasInjectionContext } from '@vue/runtime-core';" "import { inject, toRaw, watch, unref, markRaw, effectScope, ref, isVue2, isRef, isReactive, set, getCurrentScope, onScopeDispose, getCurrentInstance, reactive, toRef, del, nextTick, computed, toRefs } from 'vue-demi';")"
    echo "[patch] pinia.mjs patched: hasInjectionContext imported from @vue/runtime-core"
  fi
fi

# 问题 2：把 @vue/shared 升级到与 vue 主版本对齐
# 注意：@dcloudio/uni-cli-shared 下可能嵌套了一份老版本 @vue/shared，也要替换
VUE_VERSION=$(cat node_modules/vue/package.json 2>/dev/null | grep '"version"' | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
if [ -n "$VUE_VERSION" ]; then
  for SHARED_PKG in node_modules/@vue/shared/package.json node_modules/@dcloudio/uni-cli-shared/node_modules/@vue/shared/package.json; do
    if [ -f "$SHARED_PKG" ]; then
      SHARED_VERSION=$(cat "$SHARED_PKG" | grep '"version"' | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')
      if [ -n "$SHARED_VERSION" ] && [ "$SHARED_VERSION" != "$VUE_VERSION" ]; then
        SHARED_DIR=$(dirname "$SHARED_PKG")
        echo "[patch] $SHARED_DIR @vue/shared $SHARED_VERSION → $VUE_VERSION"
        rm -rf "$SHARED_DIR"
        # 用 npm pack 下载对应版本然后解压到目标位置
        TMP_DIR=$(mktemp -d)
        (cd "$TMP_DIR" && npm pack "@vue/shared@$VUE_VERSION" 2>/dev/null && tar xzf *.tgz)
        cp -R "$TMP_DIR/package/." "$SHARED_DIR"
        rm -rf "$TMP_DIR"
      fi
    fi
  done
fi
