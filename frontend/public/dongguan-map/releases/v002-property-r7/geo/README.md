# 本地地理数据

页面运行时只读取本目录中的 GeoJSON，不访问 GitHub、Nominatim 或 Overpass。

- `prd-core-cities.geojson`：广州、深圳、珠海、佛山、江门、肇庆、惠州、中山八市行政边界；OpenStreetMap 数据，ODbL-1.0。
- `dongguan-towns.geojson`：东莞 32 镇街；来自 `eddiehe99/dongguan-map`。仓库采用 MIT，但其 GeoJSON 上游数据许可证未单独声明，仅用于原型演示，商用前须复核来源。
- `dongguan-town-display.geojson`：上述镇界的交互展示派生数据。通过 `python scripts/build-dongguan-town-display.py` 清除退化环、按原市界裁剪、按来源 ID 数字升序分配重叠区域，并生成面内名称锚点。市级原始边界和镇级原始文件保持原样；缺失归属的市域仍为市级底图。`dongguan-town-display-manifest.json` 记录输入输出哈希、投影、逐镇修复面积和来源；ID 不是行政区划代码，派生文件仅用于显示与拾取。
- `dongguan-waterways.geojson`：东莞范围河流和运河；OpenStreetMap 数据，ODbL-1.0。
- `manifest.json`：每套数据的来源、许可证、要素数和状态。
- `regional-water.geojson`、`regional-coastline.geojson`：OpenFreeMap 固定快照 `20260830_080001_pt` 的 OpenMapTiles/OSM 水域及岸线，包含海湾、河流和湖塘；ODbL-1.0。用于审计和离线栅格化，页面只读取生成后的本地遮罩与水位 PNG，不下载这两份大型 GeoJSON。来源和哈希见 `../terrain/regional-water-manifest.json`。

使用 OSM 数据的界面必须展示：`© OpenStreetMap contributors`，并链接至 https://www.openstreetmap.org/copyright 。
