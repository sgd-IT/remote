import { Layout } from '@/utils/routerHelper'

const { t } = useI18n()
/**
 * redirect: noredirect        当设置 noredirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'          设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * meta : {
 hidden: true              当设置 true 的时候该路由不会再侧边栏出现 如404，login等页面(默认 false)

 alwaysShow: true          当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式，
 只有一个时，会将那个子路由当做根路由显示在侧边栏，
 若你想不管路由下面的 children 声明的个数都显示你的根路由，
 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，
 一直显示根路由(默认 false)

 title: 'title'            设置该路由在侧边栏和面包屑中展示的名字

 icon: 'svg-name'          设置该路由的图标

 noCache: true             如果设置为true，则不会被 <keep-alive> 缓存(默认 false)

 breadcrumb: false         如果设置为false，则不会在breadcrumb面包屑中显示(默认 true)

 affix: true               如果设置为true，则会一直固定在tag项中(默认 false)

 noTagsView: true          如果设置为true，则不会出现在tag中(默认 false)

 activeMenu: '/dashboard'  显示高亮的路由路径

 followAuth: '/dashboard'  跟随哪个路由进行权限过滤

 canTo: true               设置为true即使hidden为true，也依然可以进行路由跳转(默认 false)
 }
 **/
const remainingRouter: AppRouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/views/Redirect/HomeRedirect.vue'),
    name: 'Root',
    meta: {
      hidden: true,
      noTagsView: true
    }
  },
  {
    path: '/redirect',
    component: Layout,
    name: 'Redirect',
    children: [
      {
        path: '/redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/Redirect/Redirect.vue'),
        meta: {}
      }
    ],
    meta: {
      hidden: true,
      noTagsView: true
    }
  },
  // 监控中心大屏 - 企业账号专用（企业账号登录后的主页，仅查看本企业数据）
  {
    path: '/monitoring-center',
    component: () => import('@/views/eiot/firewatch/monitoring-center/index.vue'),
    name: 'MonitoringCenter',
    meta: {
      hidden: true,
      title: '监控中心大屏',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 值守端大屏 - 系统管理员专用（admin登录后的主页，可查看所有企业数据）
  {
    path: '/duty-screen',
    component: () => import('@/views/eiot/firewatch/duty-screen/index.vue'),
    name: 'DutyScreen',
    meta: {
      hidden: true,
      title: '值守端大屏',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 告警大屏
  {
    path: '/alarm-screen',
    component: () => import('@/views/eiot/firewatch/alarm-screen/index.vue'),
    name: 'AlarmScreen',
    meta: {
      hidden: true,
      title: '告警大屏',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 设备总览（从值守大屏“设备状态统计”进入）
  {
    path: '/device-screen',
    component: () => import('@/views/eiot/firewatch/device-screen/index.vue'),
    name: 'DeviceScreen',
    meta: {
      hidden: true,
      title: '设备总览',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 监控大屏
  {
    path: '/eiot/firewatch/large-screen',
    component: () => import('@/views/eiot/firewatch/large-screen/index.vue'),
    name: 'FireWatchLargeScreen',
    meta: {
      hidden: true,
      title: '监控大屏',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 远程控制台
  {
    path: '/remote-console',
    component: () => import('@/views/eiot/firewatch/remote-console/index.vue'),
    name: 'RemoteConsole',
    meta: {
      hidden: true,
      title: '远程控制台',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 运维工作台
  {
    path: '/ops-workbench',
    component: () => import('@/views/eiot/firewatch/ops-workbench/index.vue'),
    name: 'OpsWorkbench',
    meta: {
      hidden: true,
      title: '运维工作台',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 数据中心
  {
    path: '/data-center',
    component: () => import('@/views/eiot/firewatch/data-center/index.vue'),
    name: 'DataCenter',
    meta: {
      hidden: true,
      title: '数据中心',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 视频监控
  {
    path: '/video-surveillance',
    component: () => import('@/views/eiot/firewatch/video-surveillance/index.vue'),
    name: 'VideoSurveillance',
    meta: {
      hidden: true,
      title: '视频监控',
      noTagsView: true,
      darkBlue: true
    }
  },
  // AI分析
  {
    path: '/eiot/firewatch/ai-analysis',
    component: () => import('@/views/eiot/firewatch/ai-analysis/index.vue'),
    name: 'AiAnalysis',
    meta: {
      hidden: true,
      title: 'AI分析',
      noTagsView: true,
      darkBlue: true
    }
  },
  // 更新日志：登录弹窗、用户菜单与侧边栏共用此页面，静态注册保证所有登录用户可达
  {
    path: '/changelog',
    component: Layout,
    name: 'Changelog',
    meta: {},
    children: [
      {
        path: '',
        component: () => import('@/views/system/changelog/index.vue'),
        name: 'SystemChangelog',
        meta: {
          canTo: true,
          noTagsView: false,
          icon: 'ep:document',
          title: '更新日志'
        }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    name: 'UserInfo',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'profile',
        component: () => import('@/views/Profile/Index.vue'),
        name: 'Profile',
        meta: {
          canTo: true,
          hidden: true,
          noTagsView: false,
          icon: 'ep:user',
          title: t('common.profile')
        }
      },
      {
        path: 'notify-message',
        component: () => import('@/views/system/notify/my/index.vue'),
        name: 'MyNotifyMessage',
        meta: {
          canTo: true,
          hidden: true,
          noTagsView: false,
          icon: 'ep:message',
          title: '我的站内信'
        }
      }
    ]
  },
  {
    path: '/dict',
    component: Layout,
    name: 'dict',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'type/data/:dictType',
        component: () => import('@/views/system/dict/data/index.vue'),
        name: 'SystemDictData',
        meta: {
          title: '字典数据',
          noCache: true,
          hidden: true,
          canTo: true,
          icon: '',
          activeMenu: '/system/dict'
        }
      }
    ]
  },

  {
    path: '/codegen',
    component: Layout,
    name: 'CodegenEdit',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'edit',
        component: () => import('@/views/infra/codegen/EditTable.vue'),
        name: 'InfraCodegenEditTable',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '修改生成配置',
          activeMenu: 'infra/codegen/index'
        }
      }
    ]
  },
  {
    path: '/job',
    component: Layout,
    name: 'JobL',
    meta: {
      hidden: true
    },
    children: [
      {
        path: 'job-log',
        component: () => import('@/views/infra/job/logger/index.vue'),
        name: 'InfraJobLog',
        meta: {
          noCache: true,
          hidden: true,
          canTo: true,
          icon: 'ep:edit',
          title: '调度日志',
          activeMenu: 'infra/job/index'
        }
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/Login/Login.vue'),
    name: 'Login',
    meta: {
      hidden: true,
      title: t('router.login'),
      noTagsView: true
    }
  },
  {
    path: '/sso',
    component: () => import('@/views/Login/Login.vue'),
    name: 'SSOLogin',
    meta: {
      hidden: true,
      title: t('router.login'),
      noTagsView: true
    }
  },
  {
    path: '/social-login',
    component: () => import('@/views/Login/SocialLogin.vue'),
    name: 'SocialLogin',
    meta: {
      hidden: true,
      title: t('router.socialLogin'),
      noTagsView: true
    }
  },
  {
    path: '/403',
    component: () => import('@/views/Error/403.vue'),
    name: 'NoAccess',
    meta: {
      hidden: true,
      title: '403',
      noTagsView: true
    }
  },
  {
    path: '/404',
    component: () => import('@/views/Error/404.vue'),
    name: 'NoFound',
    meta: {
      hidden: true,
      title: '404',
      noTagsView: true
    }
  },
  {
    path: '/500',
    component: () => import('@/views/Error/500.vue'),
    name: 'Error',
    meta: {
      hidden: true,
      title: '500',
      noTagsView: true
    }
  },
  {
    path: '/eiot',
    component: Layout,
    name: 'YIOT',
    meta: {
      hidden: true
    },
    children: [
      {
        path: '/eiot/products/productEdit/:id',
        name: 'EIoTProductEdit',
        meta: {
          title: '产品详情',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/product'
        },
        component: () => import('@/views/eiot/product/productEdit.vue')
      },
      {
        path: '/eiot/device/deviceDetail/:id',
        name: 'EIoTDeviceDetail',
        meta: {
          title: '设备详情',
          noCache: true,
          hidden: true,
        },
        component: () => import('@/views/eiot/deviceinfo/detail.vue')
      },
      // 消防值守模块路由
      {
        path: '/eiot/firewatch/enterprise',
        name: 'FireWatchEnterprise',
        meta: {
          title: '企业信息',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/firewatch'
        },
        component: () => import('@/views/eiot/firewatch/enterprise/index.vue')
      },
      // {
      //   path: '/eiot/firewatch/dashboard',
      //   name: 'FireWatchDashboard',
      //   meta: {
      //     title: '监控中心',
      //     noCache: true,
      //     hidden: true,
      //     activeMenu: '/eiot/firewatch'
      //   },
      //   component: () => import('@/views/eiot/firewatch/dashboard/index.vue')
      // },
      // 处置中心：大屏/值守大屏跳转入口，静态注册保证路由可达
      {
        path: '/eiot/firewatch/alarm-order',
        name: 'FireWatchAlarmOrder',
        meta: {
          title: '处置中心',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/firewatch'
        },
        component: () => import('@/views/eiot/firewatch/alarm-order/index.vue')
      },
      // 值班管理：大屏跳转入口，静态注册保证路由可达
      {
        path: '/eiot/firewatch/duty',
        name: 'FireWatchDuty',
        meta: {
          title: '值班管理',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/firewatch'
        },
        component: () => import('@/views/eiot/firewatch/duty/index.vue')
      },
      // 查岗/巡检管理：大屏跳转入口，静态注册保证路由可达
      {
        path: '/eiot/firewatch/patrol',
        name: 'FireWatchPatrol',
        meta: {
          title: '巡检任务',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/firewatch'
        },
        component: () => import('@/views/eiot/firewatch/patrol/index.vue')
      },
      {
        path: '/eiot/firewatch/property',
        name: 'FireWatchProperty',
        meta: {
          title: '单位管理',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/firewatch'
        },
        component: () => import('@/views/eiot/firewatch/property/index.vue')
      },

      // 报警记录：大屏/值守大屏跳转入口，静态注册保证路由可达
      {
        path: '/eiot/firewatch/alarm-record',
        name: 'FireWatchAlarmRecord',
        meta: {
          title: '报警记录',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/firewatch'
        },
        component: () => import('@/views/eiot/firewatch/alarm-record/index.vue')
      },
      // {
      //   path: '/eiot/firewatch/maintenance',
      //   name: 'FireWatchMaintenance',
      //   meta: {
      //     title: '维护保养',
      //     noCache: true,
      //     hidden: true,
      //     activeMenu: '/eiot/firewatch'
      //   },
      //   component: () => import('@/views/eiot/firewatch/maintenance/index.vue')
      // },
      // {
      //   path: '/eiot/firewatch/duty-record',
      //   name: 'FireWatchDutyRecord',
      //   meta: {
      //     title: '值班记录',
      //     noCache: true,
      //     hidden: true,
      //     activeMenu: '/eiot/firewatch'
      //   },
      //   component: () => import('@/views/eiot/firewatch/duty-record/index.vue')
      // },
      // {
      //   path: '/eiot/firewatch/hazard',
      //   name: 'FireWatchHazard',
      //   meta: {
      //     title: '隐患管理',
      //     noCache: true,
      //     hidden: true,
      //     activeMenu: '/eiot/firewatch'
      //   },
      //   component: () => import('@/views/eiot/firewatch/hazard/index.vue')
      // },
      {
        path: '/eiot/remote/operation-log',
        name: 'RemoteOperationLog',
        meta: {
          title: '远程控制日志',
          noCache: true,
          hidden: true,
          activeMenu: '/eiot/remote'
        },
        component: () => import('@/views/eiot/remote/operation-log/index.vue')
      }

    ]
  },
  // 百度地图测试页
  {
    path: '/test/baidu-map',
    component: () => import('@/test/baidu-map/index.vue'),
    name: 'TestBaiduMap',
    meta: {
      hidden: true,
      title: '百度地图测试',
      noTagsView: true
    }
  },
]

export default remainingRouter
