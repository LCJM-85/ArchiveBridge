import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
const read = path => readFileSync(new URL(path, import.meta.url), 'utf8')
const review = read('../src/views/review/ArchiveReview.vue')
const student = read('../src/views/data/StudentStatusData.vue')
const styles = read('../src/styles/index.css')

test('审查与学籍页使用相同的主标题结构和字体类', () => {
  for (const page of [review, student]) {
    assert.match(page, /class="ph-left"/)
    assert.match(page, /<h2 class="font-display">/)
  }
})
test('中文页面标题优先使用完整本机宋体，保持28px和常规字重', () => {
  const heading = styles.match(/\.content (?:\.page-hero )?\.ph-left h2\s*\{([^}]+)\}/)[1]
  assert.match(heading, /font-family:\s*'SimSun'/)
  assert.match(heading, /font-size:\s*28px/)
  assert.match(heading, /font-weight:\s*400/)
})
