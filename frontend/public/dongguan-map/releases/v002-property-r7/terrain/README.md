# 珠三角与东莞本地地形资产

该目录内的运行时资源全部为本地文件，页面无需连接外部地形服务。

- `prd-dem.png`：珠三角核心八市范围的灰度高度图，长边 2048。
- `prd-relief.webp`、`prd-normal.png`、`prd-roughness.png`：区域背景的山体阴影、法线和粗糙度。
- `dongguan-dem.png`：由 32 镇街边界自动计算范围的东莞灰度高度图，长边 1024。
- `dongguan-relief.webp`、`dongguan-normal.png`、`dongguan-roughness.png`：东莞英雄层的 PBR 表面贴图。
- `manifest.json`：两套纹理的经纬度范围、尺寸、最大高程、编码、来源和许可证信息。
- `regional-land-mask.png`：4096×3670 海陆遮罩，与 PRD 范围一致；白陆黑水，运行时使用单通道 R8（14.34 MiB）。
- `regional-water-level.png`：只供 CPU 采样的水面高程，海洋为零、湖塘为各自高程中位数；陆地侧的采样余量不改变海岸形状。
- `regional-water-manifest.json`：水域来源、固定快照、范围、哈希和地理样点检查。
- `dongguan-map-fallback.webp`：当前东莞主体机位的实际渲染回退图。

灰度 DEM 的 `0..255` 线性映射到清单内各对象的 `0..maxElevationMeters`。所有派生贴图由同一高程矩阵确定性生成。

重新生成全部资产：

```powershell
python scripts/build-prd-terrain.py
```

只更新东莞资产：

```powershell
python scripts/build-dongguan-dem.py
```

高程来自 AWS Open Data 托管的 Mapzen Terrain Tiles。该汇编包含多个原始高程数据源，使用时需保留 Mapzen 及其底层提供方归属；详细条款见：

- https://registry.opendata.aws/terrain-tiles/
- https://github.com/tilezen/joerd/blob/master/docs/attribution.md
- https://www.mapzen.com/rights/

`dongguan-relief.jpg` 是旧版兼容文件，新代码应使用清单声明的 WebP 文件。

从仓库内水域 GeoJSON 离线重建海陆遮罩与水位图：

```powershell
python scripts/build-dongguan-water.py --from-local --scale 2
```

水域来自 OpenFreeMap 固定快照中的 OpenMapTiles/OSM 水域多边形。原始审计数据位于 `public/geo/regional-water.geojson`，不由页面运行时加载。
