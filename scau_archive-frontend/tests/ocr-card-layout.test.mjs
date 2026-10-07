import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse } from '@vue/compiler-sfc'

const source = readFileSync(new URL('../src/views/ocr/OCRProcess.vue', import.meta.url), 'utf8')
const { descriptor } = parse(source)
const hasClass = (node, name) => node.props?.some(prop =>
  prop.name === 'class' && prop.value?.content.split(/\s+/).includes(name))
function find(node, predicate) {
  if (predicate(node)) return node
  for (const child of node.children || []) {
    const found = find(child, predicate)
    if (found) return found
  }
}
const grid = find(descriptor.template.ast, node => hasClass(node, 'stat-cards'))
const cards = grid.children.filter(node => hasClass(node, 'stat-card'))
const style = descriptor.styles[0].content

test('五张状态卡片都具有图标、数字和标签，待审查使用独立配色', () => {
  assert.equal(cards.length, 5)
  for (const card of cards) {
    assert.ok(find(card, node => hasClass(node, 'sc-icon')), '每张卡片都应具有图标')
    assert.ok(find(card, node => hasClass(node, 'sc-num')))
    assert.ok(find(card, node => hasClass(node, 'sc-label')))
  }
  assert.ok(hasClass(cards[0], 'sc-review'))
  assert.match(style, /\.sc-review\s*\{[^}]*--sc1:/)
})

test('宽屏五列等宽，窄屏两列，手机单列', () => {
  assert.match(style, /\.stat-cards\s*\{[^}]*grid-template-columns:\s*repeat\(5,\s*minmax\(0,\s*1fr\)\)/)
  assert.match(style, /@media\s*\(max-width:\s*900px\)\s*\{\s*\.stat-cards\s*\{[^}]*repeat\(2,/)
  assert.match(style, /@media\s*\(max-width:\s*480px\)\s*\{\s*\.stat-cards\s*\{[^}]*grid-template-columns:\s*1fr/)
})

test('今日和历史状态列为标签留出空间，标签含边框尺寸并居中', () => {
  assert.equal((source.match(/prop="recognizeStatus" label="状态" width="120" align="center"/g) || []).length, 2)
  const pill = style.match(/\.status-pill\s*\{([^}]+)\}/)[1]
  assert.match(pill, /box-sizing:\s*border-box/)
  assert.match(pill, /display:\s*inline-flex/)
  assert.match(pill, /justify-content:\s*center/)
  assert.match(pill, /align-items:\s*center/)
  assert.match(pill, /min-height:\s*30px/)
})
