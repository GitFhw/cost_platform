import fs from 'node:fs'
import path from 'node:path'
import zlib from 'node:zlib'
import process from 'node:process'

const projectRoot = process.cwd()
const distDir = path.resolve(projectRoot, 'dist')
const reportPath = path.resolve(distDir, 'bundle-size-report.json')
const trackedExtensions = new Set(['.js', '.css'])
const largeAssetThreshold = 900 * 1024

function walkFiles(dir) {
  if (!fs.existsSync(dir)) {
    return []
  }

  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const fullPath = path.join(dir, entry.name)
    return entry.isDirectory() ? walkFiles(fullPath) : [fullPath]
  })
}

function formatKb(bytes) {
  return `${(bytes / 1024).toFixed(1)} KiB`
}

if (!fs.existsSync(distDir)) {
  console.error('dist directory not found. Run npm run build:prod first.')
  process.exit(1)
}

const assets = walkFiles(distDir)
  .filter(file => trackedExtensions.has(path.extname(file)))
  .map(file => {
    const content = fs.readFileSync(file)
    const size = content.length
    const gzipSize = zlib.gzipSync(content).length
    return {
      file: path.relative(distDir, file).replace(/\\/g, '/'),
      type: path.extname(file).slice(1),
      size,
      gzipSize
    }
  })
  .sort((left, right) => right.size - left.size)

const totals = assets.reduce((acc, asset) => {
  acc[asset.type] = acc[asset.type] || { size: 0, gzipSize: 0, count: 0 }
  acc[asset.type].size += asset.size
  acc[asset.type].gzipSize += asset.gzipSize
  acc[asset.type].count += 1
  return acc
}, {})

const warnings = assets
  .filter(asset => asset.size > largeAssetThreshold)
  .map(asset => `${asset.file} is ${formatKb(asset.size)}`)

const report = {
  generatedAt: new Date().toISOString(),
  totals,
  largestAssets: assets.slice(0, 20),
  warnings
}

fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`)

console.log(`Bundle audit written to ${path.relative(projectRoot, reportPath)}`)
Object.entries(totals).forEach(([type, total]) => {
  console.log(`${type}: ${total.count} files, ${formatKb(total.size)}, gzip ${formatKb(total.gzipSize)}`)
})

console.log('Largest assets:')
assets.slice(0, 10).forEach(asset => {
  console.log(`- ${asset.file}: ${formatKb(asset.size)}, gzip ${formatKb(asset.gzipSize)}`)
})

if (warnings.length) {
  console.warn('Large asset warnings:')
  warnings.forEach(warning => console.warn(`- ${warning}`))
}
