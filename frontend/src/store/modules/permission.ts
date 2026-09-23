import { defineStore } from 'pinia'
import { store } from '@/store'
import { cloneDeep } from 'lodash-es'
import remainingRouter from '@/router/modules/remaining'
import { flatMultiLevelRoutes, generateRoute } from '@/utils/routerHelper'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'

const { wsCache } = useCache()

export interface PermissionState {
  routers: AppRouteRecordRaw[]
  addRouters: AppRouteRecordRaw[]
  menuTabRouters: AppRouteRecordRaw[]
}

// 需要从菜单中移除的路径关键词
const HIDDEN_MENU_PATHS = ['firewatch/device', 'firewatch/user', 'duty-screen', 'report']

// 在消防值守（firewatch）父级下需要隐藏的子菜单标题
const FIREWATCH_HIDDEN_TITLES = ['设备管理', '用户管理', '操作日志', '值守端大屏']

// 需要从顶级菜单中隐藏的菜单标题（如首页）
const HIDDEN_TOP_MENU_TITLES = ['首页']

// 需要注入到消防值守模块的额外菜单项
const FIREWATCH_EXTRA_MENUS: AppRouteRecordRaw[] = [
  // {
  //   path: 'alarm-record',
  //   name: 'FireWatchAlarmRecord',
  //   component: () => import('@/views/eiot/firewatch/alarm-record/index.vue'),
  //   meta: {
  //     title: '报警记录',
  //     icon: 'ep:document',
  //     noCache: true
  //   }
  // },
  // {
  //   path: 'maintenance',
  //   name: 'FireWatchMaintenance',
  //   component: () => import('@/views/eiot/firewatch/maintenance/index.vue'),
  //   meta: {
  //     title: '维护保养',
  //     icon: 'ep:tools',
  //     noCache: true
  //   }
  // },
  // {
  //   path: 'hazard',
  //   name: 'FireWatchHazard',
  //   component: () => import('@/views/eiot/firewatch/hazard/index.vue'),
  //   meta: {
  //     title: '隐患管理',
  //     icon: 'ep:warning',
  //     noCache: true
  //   }
  // }
]

// 递归过滤路由，parentIsFirewatch 标记当前层级是否属于消防值守的子菜单
const filterHiddenRoutes = (routes: AppRouteRecordRaw[], parentIsFirewatch = false): AppRouteRecordRaw[] => {
  return routes
    .filter((route) => {
      const path = route.path || ''
      const title = (route.meta?.title as string) || ''
      // 通过路径匹配（适用于所有层级）
      if (HIDDEN_MENU_PATHS.some((hp) => path.includes(hp))) return false
      // 过滤顶级菜单中不需要的菜单项（如首页）
      if (!parentIsFirewatch && HIDDEN_TOP_MENU_TITLES.includes(title)) return false
      // 仅在消防值守的子菜单下按标题过滤，避免误删设备中心等其他模块下的同名菜单
      if (parentIsFirewatch && FIREWATCH_HIDDEN_TITLES.includes(title)) return false
      return true
    })
    .map((route) => {
      if (route.children?.length) {
        const path = route.path || ''
        const title = (route.meta?.title as string) || ''
        // 判断当前路由是否为消防值守模块
        const isFirewatch = path.includes('firewatch') || title === '消防值守'
        return { ...route, children: filterHiddenRoutes(route.children, isFirewatch || parentIsFirewatch) }
      }
      return route
    })
}

// 向消防值守模块注入额外的菜单项（如报警记录）
const injectFirewatchMenus = (routes: AppRouteRecordRaw[]): AppRouteRecordRaw[] => {
  return routes.map((route) => {
    const path = route.path || ''
    const title = (route.meta?.title as string) || ''
    const isFirewatch = path.includes('firewatch') || title === '消防值守'
    if (isFirewatch && route.children?.length) {
      // 过滤掉已存在的路径，避免重复注入
      const existingPaths = route.children.map((c) => c.path)
      const newMenus = FIREWATCH_EXTRA_MENUS.filter((m) => !existingPaths.includes(m.path))
      if (newMenus.length > 0) {
        return { ...route, children: [...route.children, ...newMenus] }
      }
    }
    // 递归处理子路由中可能嵌套的消防值守模块
    if (route.children?.length) {
      return { ...route, children: injectFirewatchMenus(route.children) }
    }
    return route
  })
}

export const usePermissionStore = defineStore('permission', {
  state: (): PermissionState => ({
    routers: [],
    addRouters: [],
    menuTabRouters: []
  }),
  getters: {
    getRouters(): AppRouteRecordRaw[] {
      return this.routers
    },
    getAddRouters(): AppRouteRecordRaw[] {
      return flatMultiLevelRoutes(cloneDeep(this.addRouters))
    },
    getMenuTabRouters(): AppRouteRecordRaw[] {
      return this.menuTabRouters
    }
  },
  actions: {
    async generateRoutes(): Promise<unknown> {
      return new Promise<void>(async (resolve) => {
        // 获得菜单列表，它在登录的时候，setUserInfoAction 方法中已经进行获取
        let res: AppCustomRouteRecordRaw[] = []
        const roleRouters = wsCache.get(CACHE_KEY.ROLE_ROUTERS)
        if (roleRouters) {
          res = roleRouters as AppCustomRouteRecordRaw[]
        }
        const routerMap: AppRouteRecordRaw[] = injectFirewatchMenus(filterHiddenRoutes(generateRoute(res)))
        // 动态路由，404一定要放到最后面
        // preschooler：vue-router@4以后已支持静态404路由，此处可不再追加
        this.addRouters = routerMap.concat([
          {
            path: '/:path(.*)*',
            // redirect: '/404',
            component: () => import('@/views/Error/404.vue'),
            name: '404Page',
            meta: {
              hidden: true,
              breadcrumb: false
            }
          }
        ])
        // 渲染菜单的所有路由
        this.routers = cloneDeep(remainingRouter).concat(routerMap)
        resolve()
      })
    },
    setMenuTabRouters(routers: AppRouteRecordRaw[]): void {
      this.menuTabRouters = routers
    }
  },
  persist: false
})

export const usePermissionStoreWithOut = () => {
  return usePermissionStore(store)
}
