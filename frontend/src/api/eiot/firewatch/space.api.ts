import request from '@/config/axios'

// 空间 VO（楼栋/楼层/区域/详细位置）
export interface SpaceVO {
  id: number // 主键ID
  tenantId: number // 租户编号（园区）
  tenantName: string // 租户名称（园区名称）
  building: string // 楼栋（几号楼）
  floor: string // 楼层（几层）
  area: string // 区域
  position: string // 详细位置（区域内具体点位，如：东侧走廊北端）
  remark: string // 备注
}

// 空间管理 API
export const SpaceApi = {
  // 查询空间列表（不分页）：系统租户管理员不传 tenantId 查全部、传则查指定租户；企业租户仅本租户
  getSpaceList: async (tenantId?: number) => {
    return await request.get({ url: `/eiot/firewatch/space/list`, params: { tenantId } })
  },
  // 创建空间：创建在当前登录租户下，同租户内 楼栋/楼层/区域/详细位置 组合不可重复
  createSpace: async (data: { building: string; floor: string; area?: string; position?: string; remark?: string }) => {
    return await request.post({ url: `/eiot/firewatch/space/create`, data })
  },
  // 更新空间：保持原租户不变，同租户内 楼栋/楼层/区域/详细位置 组合不可重复
  updateSpace: async (data: { id: number; building: string; floor: string; area?: string; position?: string; remark?: string }) => {
    return await request.put({ url: `/eiot/firewatch/space/update`, data })
  },
  // 删除空间：存在设备引用时后端会拒绝删除
  deleteSpace: async (id: number) => {
    return await request.delete({ url: `/eiot/firewatch/space/delete`, params: { id } })
  }
}
