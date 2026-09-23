<template>
  <div>
    <el-table size="small" :data="properties" highlight-current-row style="width: 100%">
      <el-table-column>
        <template #default="fun">
          <el-form v-model="fun.row">
            <el-form-item label="值">
              <el-input v-model="fun.row.value" size="small" />
            </el-form-item>
          </el-form>
        </template>
      </el-table-column>

      <el-table-column sortable prop="name" label="功能名称" width="100" />
      <el-table-column sortable prop="identifier" label="标识符" width="80" />
      <el-table-column sortable prop="dataTypeName" label="数据类型" width="100" />
      <el-table-column sortable prop="params" label="数据定义" width="200" />
    </el-table>
    <el-button type="primary" @click="sendThingModelMsg()" v-if="this.propertiesTyp == '属性上报'">发送</el-button>
    <el-button type="primary" @click="sendAllPropertiesGet()" v-if="this.propertiesTyp == '属性调试'">获取</el-button>
  </div>
</template>

<script>
import { deviceSimulateSend, propertyGet, propertySet } from "@/api/eiot/deviceinfo/devices.api";

export default {
  name: 'deviceSimulatorProperties',
  props: {
    properties: {
      type: Array,
    },
    propertiesTyp: {
      type: String,
    },
    deviceDetail: {
      type: Object,
    },
  },

  data() {
    return {}
  },
  computed: {
    deviceId() {
      return this.deviceDetail.id
    },
  },
  methods: {
    sendPropertiesSet(row) {
      let prop = {}
      prop[row.identifier] = row.value
      propertySet({
        deviceId: this.deviceId,
        args: prop,
      }).then((res) => {
        if (res.code === 200) {
          ElMessage({
            type: 'success',
            message: '操作成功',
          })
        } else {
          ElMessage({
            type: 'error',
            message: res.message,
          })
        }
      })
    },

    sendPropertiesGet(row) {
      let prop = []
      prop.push(row.identifier)
      propertyGet({
        deviceId: this.deviceId,
        propertyNames: prop,
      }).then((res) => {
        if (res.code === 200) {
          ElMessage({
            type: 'success',
            message: '操作成功',
          })
        } else {
          ElMessage({
            type: 'error',
            message: res.message,
          })
        }
      })
    },
    sendAllPropertiesGet() {
      let prop = []
      let allProperties = this.properties.filter((o) => o.type && o.type == 'property')
      allProperties.forEach((fun) => {
        prop.push(fun.identifier)
      })
      propertyGet({
        deviceId: this.deviceId,
        propertyNames: prop,
      }).then((res) => {
        if (res.code === 200) {
          ElMessage({
            type: 'success',
            message: '操作成功',
          })
        } else {
          ElMessage({
            type: 'error',
            message: res.message,
          })
        }
      })
    },
    sendSimpleThingModelMsg(fun) {
      let data = {}
      if (fun.type == 'property') {
        let val = fun.value
        switch (fun.dataTypeName) {
          case 'int32':
          if(val<fun.raw.dataType.specs.min||val>fun.raw.dataType.specs.max){
            ElMessage({
                type: 'info',
                message: '数据类型错误',
              })
              return
            }
            break
          case 'bool':
            break
          case 'enum':
          if (!(val in fun.raw.dataType.specs)) {
            ElMessage({
              type: 'info',
              message: '数据类型错误',
            })
            return
          }
            break
          case 'float':
            val = parseFloat(val)
            break
        }
        data[fun.identifier] = val
      } else {
        data = JSON.parse(fun.content)
      }

      deviceSimulateSend({
        deviceId: this.deviceDetail.id,
        productKey: this.deviceDetail.productKey,
        dn: this.deviceDetail.dn,
        type: fun.type,
        identifier: fun.type == 'property' ? 'report' : fun.identifier,
        data: data,
      }).then((res) => {
        if (res.code === 200) {
          ElMessage({
            type: 'success',
            message: '操作成功',
          })
        } else {
          ElMessage({
            type: 'error',
            message: res.message,
          })
        }
      })
    },
    sendThingModelMsg() {
      let data = {}
      let hasValProperties = this.properties.filter((o) => o.value && o.value != '')
      let flag=false
      hasValProperties.forEach((fun) => {
        if (fun.type == 'property') {
          let val = fun.value
          switch (fun.dataTypeName) {
            case 'int32':
              break
            case 'bool':
              break
            case 'enum':
              if (!(val in fun.raw.dataType.specs)) {
                flag=true
              }
              break
            case 'float':
              val = parseFloat(val)
              break
          }
          data[fun.identifier] = val
        }
      })
      if(flag){
        ElMessage({
              type: 'info',
              message: '数据类型错误',
            })
        return
      }
      deviceSimulateSend({
        deviceId: this.deviceDetail.id,
        productKey: this.deviceDetail.productKey,
        dn: this.deviceDetail.dn,
        type: 'property',
        identifier: 'report',
        data: data,
      }).then((res) => {
        if (res.code === 200) {
          ElMessage({
            type: 'success',
            message: '操作成功',
          })
        } else {
          ElMessage({
            type: 'error',
            message: res.message,
          })
        }
      })
    },
  },
}
</script>
