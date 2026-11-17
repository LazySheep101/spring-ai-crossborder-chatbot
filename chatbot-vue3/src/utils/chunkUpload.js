import SparkMD5 from 'spark-md5'
import { checkFile, mergeFileChunk, uploadFileChunk } from '@/api/customerService'

const CHUNK_SIZE = 2 * 1024 * 1024

function md5Of(file) {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    const total = Math.max(1, Math.ceil(file.size / CHUNK_SIZE))
    let index = 0
    reader.onerror = () => reject(new Error('MD5 计算失败'))
    reader.onload = (e) => {
      spark.append(e.target.result)
      index += 1
      if (index < total) {
        readNext()
      } else {
        resolve(spark.end())
      }
    }
    const readNext = () => {
      const start = index * CHUNK_SIZE
      reader.readAsArrayBuffer(file.slice(start, Math.min(start + CHUNK_SIZE, file.size)))
    }
    readNext()
  })
}

/**
 * 复用客服知识库的分片上传：校验、分片、合并，合并后由后端异步向量化。
 */
export async function uploadKnowledgeFile(file, onProgress) {
  const fileMd5 = await md5Of(file)
  const totalChunks = Math.max(1, Math.ceil(file.size / CHUNK_SIZE))
  const check = await checkFile(fileMd5)
  const data = check.data?.data || {}
  if (check.data?.success && data.exists && !data.needUpload) {
    onProgress?.(100, '文件已存在，秒传成功')
    return
  }
  const uploaded = new Set(data.uploadedChunks || [])
  let done = uploaded.size
  for (let i = 0; i < totalChunks; i++) {
    if (uploaded.has(i)) continue
    const start = i * CHUNK_SIZE
    const chunk = file.slice(start, Math.min(start + CHUNK_SIZE, file.size))
    const form = new FormData()
    form.append('chunk', chunk)
    form.append('fileMd5', fileMd5)
    form.append('fileName', file.name)
    form.append('fileSize', file.size)
    form.append('chunkNumber', i)
    form.append('totalChunks', totalChunks)
    const res = await uploadFileChunk(form, 60000)
    if (!res.data?.success) {
      throw new Error(res.data?.message || `分片 ${i} 上传失败`)
    }
    done += 1
    onProgress?.(Math.floor((done / totalChunks) * 90), `上传中 ${done}/${totalChunks}`)
  }
  onProgress?.(95, '合并并排队向量化')
  const merged = await mergeFileChunk(fileMd5, 120000)
  if (!merged.data?.success) {
    throw new Error(merged.data?.message || '合并失败')
  }
  onProgress?.(100, '已提交向量化')
}
