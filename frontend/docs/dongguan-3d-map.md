# 东莞三维地图发布说明

值守大屏 `/duty-screen` 使用 `public/dongguan-map/` 下的独立静态地图包。生产构建时 Vite 会将该目录原样复制到站点根目录，因此不需要部署 `D:\sc-datav`，也不依赖开发机的 `127.0.0.1` 地址。

更新地图时，在本项目根目录执行：

```powershell
$env:DONGGUAN_MAP_SOURCE = 'D:\sc-datav' # 默认值就是这个路径，可省略
corepack pnpm run sync:dongguan-map
```

该命令会以 `/dongguan-map/` 为资源前缀重新构建地图，并更新 `public/dongguan-map/`。随后按原有流程执行 `corepack pnpm run build:prod` 或对应环境的构建命令即可。

值守大屏通过同源 iframe 加载地图。顶部定位按钮提供“全市总览、返回全市、放大、缩小”控制；地图的滚轮、拖动、镇街悬停和点击交互均在中央地图区域内生效。地图向父页面发送 `dongguan-map-selection` 事件，并接收 `dongguan-map-command` 指令，为后续按镇街联动设备、告警和指标卡保留接口。

企业和设备数据中带有经纬度时，值守页会将其作为状态色三维点位传给地图；市界外或无坐标的数据不会生成地图标记。
