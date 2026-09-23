// 贡献人员Api


export const contributorList: any[] = [
  {
    id: "437355835748421",
    contributor: 'tangtang',
    avatar: '',
    post: 1,
    intro: '来自于一个切图仔的自我修养',
    tags: 'vue,react,node,uniapp,flutter',
    title: '前端精神小伙',
    context: '<p><br></p>',
    score: null,
    status: null,
  },
  {
    id: "437670919086149",
    contributor: 'regan',
    avatar: '',
    post: 2,
    intro: '物联网技术爱好者。',
    tags: '物联网,大数据,边缘计算',
    title: null,
    context: null,
    score: null,
    status: null,
  },
  {
    id: "437687202414661",
    contributor: '闹腾',
    avatar: '',
    post: 2,
    intro: '早日实现金钱自由',
    tags: 'java ,go,python',
    title: null,
    context: null,
    score: null,
    status: null,
  },
]
// 获取贡献人员列表
export const getContributorList = () => {
  return contributorList
}

// 获取贡献人员详情
export const getContributorDetail = (id) => {
 return contributorList.find(item => item.id === id)
}
