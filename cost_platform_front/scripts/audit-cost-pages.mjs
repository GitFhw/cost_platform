import fs from 'node:fs'
import path from 'node:path'
import process from 'node:process'

const projectRoot = process.cwd()
const costViewsDir = path.resolve(projectRoot, 'src/views/cost')
const reportPath = path.resolve(projectRoot, 'dist/cost-page-size-report.json')
const warningLineThreshold = 1000
const largeLineThreshold = 1500

function walkVueFiles(dir) {
  if (!fs.existsSync(dir)) {
    return []
  }

  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const fullPath = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      return walkVueFiles(fullPath)
    }
    return entry.isFile() && entry.name.endsWith('.vue') ? [fullPath] : []
  })
}

function countLines(file) {
  const content = fs.readFileSync(file, 'utf8')
  if (!content) {
    return 0
  }
  return content.split(/\r\n|\r|\n/).length
}

const pages = walkVueFiles(costViewsDir)
  .map(file => ({
    file: path.relative(projectRoot, file).replace(/\\/g, '/'),
    lines: countLines(file)
  }))
  .sort((left, right) => right.lines - left.lines)

const oversizedPages = pages.filter(page => page.lines >= warningLineThreshold)
const report = {
  generatedAt: new Date().toISOString(),
  warningLineThreshold,
  largeLineThreshold,
  pageCount: pages.length,
  oversizedPageCount: oversizedPages.length,
  largestPages: pages.slice(0, 20),
  oversizedPages
}

fs.mkdirSync(path.dirname(reportPath), { recursive: true })
fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`)

console.log(`Cost page audit written to ${path.relative(projectRoot, reportPath)}`)
console.log(`Cost pages: ${pages.length}, over ${warningLineThreshold} lines: ${oversizedPages.length}`)
pages.slice(0, 12).forEach(page => {
  const label = page.lines >= largeLineThreshold ? 'large' : page.lines >= warningLineThreshold ? 'watch' : 'ok'
  console.log(`- ${page.file}: ${page.lines} lines (${label})`)
})
