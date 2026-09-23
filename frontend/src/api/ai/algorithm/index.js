import request from '@/config/axios'

// 查询算法列表
export function listAlgorithm(query) {
  return request.get({
    url: '/ai/algorithm/page',
    method: 'get',
    params: query
  })
}

// 查询算法详细
export function getAlgorithm(algorithmId) {
  return request.get({
    url: '/ai/algorithm/get',
    method: 'get',
    params:{
      id:algorithmId
    }
  })
}

// 新增算法
export function addAlgorithm(data) {
  return request.post({
    url: '/ai/algorithm/create',
    method: 'post',
    data: data
  })
}

// 修改算法
export function updateAlgorithm(data) {
  return request.put({
    url: '/ai/algorithm/update',
    method: 'put',
    data: data
  })
}

// 删除算法
export function delAlgorithm(algorithmId) {
  return request.delete({
    url: '/ai/algorithm/delete',
    method: 'delete',
    params:{
      id:algorithmId
    }
  })
}

// 下载模型文件
export function downloadModelFile(fileName) {
  return request.download({
    url: '/ai/algorithm/download/' + fileName,
    method: 'get',
    responseType: 'blob'
  })
}
