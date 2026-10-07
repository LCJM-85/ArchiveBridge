import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
for (const name of ['AdmissionData', 'GraduationData', 'StudentStatusData']) {
  const source = readFileSync(new URL(`../src/views/data/${name}.vue`, import.meta.url), 'utf8')
  test(`${name}选择和操作共用一列，使用编辑图标和点击菜单`, () => {
    const column = source.match(/<el-table-column type="selection"[\s\S]*?<\/el-table-column>/)[0]
    assert.ok(!column.includes('fixed="right"'))
    assert.ok(!source.includes('label="操作"'))
    assert.match(column, /width="88"/)
    assert.match(column, /<el-checkbox/)
    assert.match(column, /toggleRowSelection\(row, checked\)/)
    assert.match(column, /selectedRows.some/)
    assert.match(column, /:disabled="loading \|\| batchDeleting"/)
    assert.match(column, /:icon="Edit"/)
    assert.ok(!column.includes('操作 ▾'))
    assert.match(column, /<el-dropdown trigger="click"/)
    assert.match(column, /openEditDialog\(row\)/)
    assert.match(column, /handleDelete\(row.id\)/)
    assert.match(source, /label="来源文件"[^>]*show-overflow-tooltip/)
  })
  test(`${name}合并列位于身份信息之前`, () => {
    const selection = source.indexOf('type="selection"')
    const action = source.indexOf('class="data-row-tools"')
    const student = source.indexOf('prop="studentNo"')
    assert.ok(selection < action && action < student)
  })
}
