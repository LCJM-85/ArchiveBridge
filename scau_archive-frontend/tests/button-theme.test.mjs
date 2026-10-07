import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const css = readFileSync(new URL('../src/styles/index.css', import.meta.url), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '')

test('浅色主按钮文字仅用于实心按钮，不能覆盖透明或描边按钮', () => {
  const rules = [...css.matchAll(/([^{}]+)\{([^{}]*)\}/g)]
    .filter(([, selector, body]) => selector.includes('.el-button--primary') &&
      /--el-button-text-color:\s*#fffaf0/.test(body))
  assert.ok(rules.length > 0)
  for (const [, selectors] of rules) {
    for (const selector of selectors.split(',')) {
      for (const variant of ['is-link', 'is-text', 'is-plain', 'is-dashed']) {
        assert.ok(selector.includes(`:not(.${variant})`), `${selector.trim()} 必须排除 ${variant}`)
      }
    }
  }
})
