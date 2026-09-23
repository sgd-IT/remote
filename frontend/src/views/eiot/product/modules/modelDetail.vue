<template>
  <div class="bg-[#eef0f3] p-10px">
    <div class="flex bosxe mb-20px py-10px px-20px bg-[#fff] rounded-3px">
      <div class="flex justify-center items-center">UI设计</div>
      <el-select v-model="showModelType" :placeholder="$t('请选择设备类型')" >
        <el-option
          v-for="dict in showModelOptions"
          :key="dict.value"
          :label="dict.label"
          :value="dict.value"
        />
      </el-select>
      <div class="ml-auto"><el-button @click="submit" type="primary">保存</el-button></div>
    </div>

    <div class="flex">
      <div class="mx-20px">
        <div class="w-[400px] bg-[#fff] px-20px py-10px heightu rounded-5px">
          <div class="guid3 text-center py-20px">
            <div v-for="(i, index) in assembly" :key="index" class="items cursor-pointer w-100px rounded-4px py-10px" @click="addassembly(i)">
              <el-icon v-if="i.type == 'parameter'" size="20"><ElementPlus /></el-icon>
              <el-icon v-if="i.type == 'realtime'" size="20"><Tools /></el-icon>
              <el-icon v-if="i.type == 'Progress'" size="20"><House /></el-icon>
              <el-icon v-if="i.type == 'instrumentPanel'" size="20"><Odometer /></el-icon>
              <!--              <el-icon v-if="i.type == 'deviceMode'" size="20"><Open /></el-icon>-->
              <el-icon v-if="i.type == 'deviceParameter'" size="20"><Menu /></el-icon>
              <!--              <el-icon v-if="i.type == 'otherfunctions'" size="20"><More /></el-icon>-->
              <el-icon v-if="i.type == 'histogram'" size="20"><Histogram /></el-icon>
              <div>{{ i.label }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="flex-1 overflow-y-scroll heightu text-center">
        <div class="drag-box noRepeat w-400px relative">
          <!-- <div class="w-full absolute top-0 left-0 h-21px relative z-1">
            <img src="../../../assets/map/topheade.png" alt="" />
          </div>
          <div class="bg-[red] absolute top-0 left-0 w-full">
            <div class="pt-40px">增压泵1号</div>
          </div> -->
          <Draggable
            v-model="tags"
            :list="list"
            :animation="500"
            item-key="id"
            class="list-group bg-[#fbfcff] min-heights"
            :forceFallback="true"
            ghost-class="ghost"
            @change="onMoveCallback"
            :move="getdata"
          >
            <template #item="{ element, index }">
              <div class="items cursor-pointer relative" :class="{ active: activeindex == index }" @click="addActiv(index, element)">
                <!-- 删除，添加 -->
                <div v-if="activeindex == index" class="absolute top-0 right-[-50px] w-36px p-5px bg-[#1890ff] rounded-4px">
                  <el-icon @click="delClick(element, index)" size="20px" color="#fff"><Delete /></el-icon>
                  <el-icon @click="addClick(element, index)" class="mt-10px" size="20px" color="#fff"><Plus /></el-icon>
                </div>

                <Assembly :elements="element" :indexs="index" />
              </div>
            </template>
          </Draggable>
        </div>
      </div>
      <div class="flex-1">
        <div class="bg-[#fff] heightu p-10px rounded-5px" style="overflow-y: scroll">
          <!-- 参数 -->
          <div v-if="types?.type == 'parameter' && list.length">
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">颜色1</div>
              <div class="flex-1"><el-color-picker v-model="list[typeindex].color" /></div>
            </div>
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">颜色2</div>
              <div class="flex-1"><el-color-picker v-model="list[typeindex].color1" /></div>
            </div>
          </div>
          <!-- 进度条 -->
          <div v-if="types?.type == 'Progress' && list.length">
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">名称</div>
              <div class="flex-1"><el-input v-model="list[typeindex].title" placeholder="Please input" /></div>
            </div>
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">单位</div>
              <div class="flex-1"><el-input v-model="list[typeindex].unit" placeholder="Please input" /></div>
            </div>
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">最小值</div>
              <div class="flex-1"><el-input v-model="list[typeindex].min" placeholder="Please input" /></div>
            </div>
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">最大值</div>
              <div class="flex-1"><el-input v-model="list[typeindex].max" placeholder="Please input" /></div>
            </div>
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">颜色1</div>
              <div class="flex-1"><el-color-picker v-model="list[typeindex].color" /></div>
            </div>
            <div class="flex mb-20px">
              <div class="flex justify-center items-center mr-10px">颜色2</div>
              <div class="flex-1"><el-color-picker v-model="list[typeindex].color1" /></div>
            </div>
          </div>

          <!-- 实时参数 -->
          <div v-if="types?.type == 'realtime'">
            <div class="mb-10px">
              <div class="inline-block">组件标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].label" placeholder="请输入组件标题" /></div>
            </div>
            <div class="mb-10px borderbottom pb-20px">
              <div class="inline-block">组件英文标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].labelEn" placeholder="请输入组件英文标题" /></div>
            </div>
            <Draggable
              :list="list[typeindex].arr"
              :animation="500"
              class="list-group bg-[#fbfcff]"
              :forceFallback="true"
              ghost-class="ghost"
              :move="getdata"
              item-key="id"
            >
              <template #item="{ element, index }">
                <div class="flex borderbottom py-20px relative">
                  <div class="w-70px">
                    <UploadImg textsub width="70px" height="70px" v-model="element.url" :limit="1" />
                    <!-- <img class="w-70px" :src="element.url" alt="" /> -->
                  </div>
                  <div class="flex-1 ml-10px color-[#333] text-16px mb-10px">
                    <div class="mb-10px">
                      <div class="inline-block">标题：</div>
                      <div class="inline-block"><el-input v-model="element.label" placeholder="请输入标题" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型名称英文：</div>
                      <div class="inline-block"><el-input v-model="element.nameEn" placeholder="物模型名称英文" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型属性：</div>
                      <div class="inline-block">
                        <el-select v-model="element.identifier" @change="changeIdentifier($event, index)" placeholder="物模型属性"
                                   class="!w-180px">
                          <el-option v-for="item in thingModel.properties" :key="item.identifier" :label="item.name" :value="item.identifier" />
                        </el-select>
                      </div>
                    </div>

                    <div class="mb-10px">
                      <div class="inline-block">单位：</div>
                      <div class="inline-block"><el-input v-model="element.unit" placeholder="请输入单位" /></div>
                    </div>
                    <div class="mb-10px">
                      <el-form-item class="inline-block">
                        <div class="inline-block">值范围：</div>
                        <div class="w-250px flex">
                          <div class="flex-1">
                            <el-input v-model="element.min" placeholder="最小值" />
                          </div>
                          <div class="mx-2">-</div>
                          <div class="flex-1">
                            <el-input v-model="element.max" placeholder="最大值" />
                          </div>
                        </div>
                      </el-form-item>
                    </div>
                    <!--                    <div class="mb-10px">
                      <div class="inline-block">数据类型：</div>
                      <el-select v-model="element.type" class="w-150px" clearable placeholder="请选择数据类型">
                        <el-option v-for="item in useDict('data_type')['data_type']" :key="item.value" :label="item.label" :value="item.value" />
                      </el-select>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">触发事件：</div>
                      <el-select v-model="element.event" class="w-150px" clearable placeholder="请选择触发事件">
                        <el-option
                          v-for="item in useDict('trigger_event')['trigger_event']"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </div>-->
                    <div>
                      <div class="inline-block">背景颜色：</div>
                      <div class="inline-block"><el-color-picker v-model="element.bg" /></div>
                    </div>
                  </div>
                  <div class="absolute right-0 top-0">
                    <el-icon @click="delbansc(element, index)" color="#8e8e8e" size="25px"><CircleCloseFilled /></el-icon>
                  </div>
                </div>
              </template>
            </Draggable>
            <div class="mt-10px w-full">
              <el-button @click="addbansc('realtime')" class="w-full" size="large" type="primary">添加版块</el-button>
            </div>
          </div>

          <!-- 设备模式 -->
          <div v-if="types?.type == 'deviceMode'">
            <div class="mb-10px">
              <div class="inline-block">组件标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].label" placeholder="请输入组件标题" /></div>
            </div>
            <div class="mb-10px borderbottom pb-20px">
              <div class="inline-block">组件英文标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].labelEn" placeholder="请输入组件英文标题" /></div>
            </div>
            <Draggable
              :list="list[typeindex].arr"
              :animation="500"
              class="list-group bg-[#fbfcff]"
              :forceFallback="true"
              ghost-class="ghost"
              :move="getdata"
              item-key="id"
            >
              <template #item="{ element, index }">
                <div class="flex borderbottom py-20px relative">
                  <div class="w-70px">
                    <UploadImg textsub width="70px" height="70px" v-model="element.url" :limit="1" />
                  </div>
                  <div class="flex-1 ml-10px color-[#333] text-16px mb-10px">
                    <div class="mb-10px">
                      <div class="inline-block">标题：</div>
                      <div class="inline-block"><el-input v-model="element.label" placeholder="请输入标题" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型名称英文：</div>
                      <div class="inline-block"><el-input v-model="element.nameEn" placeholder="物模型名称英文" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型属性：</div>
                      <div class="inline-block"><el-input v-model="element.identifier" placeholder="物模型属性" /></div>
                    </div>
                  </div>

                  <div class="absolute right-0 top-0">
                    <el-icon @click="delbansc(element, index)" color="#8e8e8e" size="25px"><CircleCloseFilled /></el-icon>
                  </div>
                </div>
              </template>
            </Draggable>
            <div class="mt-10px w-full">
              <el-button @click="addbansc('deviceMode')" class="w-full" size="large" type="primary">添加版块</el-button>
            </div>
          </div>
          <!-- 设置参数 -->
          <div v-if="types?.type == 'deviceParameter'">
            <div class="mb-10px">
              <div class="inline-block">组件标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].label" placeholder="请输入组件标题" /></div>
            </div>
            <div class="mb-10px borderbottom pb-20px">
              <div class="inline-block">组件英文标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].labelEn" placeholder="请输入组件英文标题" /></div>
            </div>
            <Draggable
              :list="list[typeindex].arr"
              :animation="500"
              class="list-group bg-[#fbfcff]"
              :forceFallback="true"
              ghost-class="ghost"
              :move="getdata"
              item-key="id"
            >
              <template #item="{ element, index }">
                <div class="flex borderbottom py-20px relative">
                  <div class="flex-1 ml-10px color-[#333] text-16px mb-10px">
                    <div class="mb-10px">
                      <div class="inline-block">标题：</div>
                      <div class="inline-block"><el-input v-model="element.label" placeholder="请输入标题" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型名称英文：</div>
                      <div class="inline-block"><el-input v-model="element.nameEn" placeholder="物模型名称英文" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型属性：</div>
                      <div class="inline-block">
                        <!--                        <el-input v-model="element.identifier" placeholder="物模型属性" />-->
                        <el-select v-model="element.identifier" @change="changeIdentifier($event, index)" placeholder="物模型属性">
                          <el-option v-for="item in thingModel.properties" :key="item.identifier" :label="item.name" :value="item.identifier" />
                        </el-select>
                      </div>
                    </div>
                    <!--                    <div class="mb-10px">
                      <div class="inline-block">显示模式：</div>
                      <div class="inline-block">
                        <el-input v-model="element.showMode" placeholder="显示模式">
                          <template #suffix>
                            <el-tooltip class="box-item" effect="dark" content="模式使用逗号(,)隔开.auto,hand,temp" placement="right-start">
                              <el-icon><QuestionFilled /></el-icon>
                            </el-tooltip>
                          </template>
                        </el-input>
                      </div>
                    </div>-->
                    <div class="mb-10px">
                      <div class="inline-block">key：</div>
                      <div class="inline-block"><el-input v-model="element.key" placeholder="请输入key" /></div>
                    </div>
                    <!--                    <div class="mb-10px">
                      <div class="inline-block">单位：</div>
                      <div class="inline-block"><el-input v-model="element.unit" placeholder="请输入单位" /></div>
                    </div>-->
                    <!--                    <div class="mb-10px">
                      <div class="inline-block">读写类型：</div>
                      <div class="inline-block">
                        <el-radio-group v-model="element.accessMode" class="ml-4">
                          <el-radio label="rw" size="large">读写</el-radio>
                          <el-radio label="r" size="large">只读</el-radio>
                        </el-radio-group>
                      </div>
                    </div>
                    <div class="mb-10px">
                      <el-form-item class="inline-block">
                        <div class="inline-block">值范围：</div>
                        <div class="w-250px flex">
                          <div class="flex-1">
                            <el-input v-model="element.min" placeholder="最小值" />
                          </div>
                          <div class="mx-2">-</div>
                          <div class="flex-1">
                            <el-input v-model="element.max" placeholder="最大值" />
                          </div>
                        </div>
                      </el-form-item>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">数据类型：</div>
                      <el-select v-model="element.type" class="w-150px" clearable placeholder="请选择数据类型">
                        <el-option v-for="item in useDict('data_type')['data_type']" :key="item.value" :label="item.label" :value="item.value" />
                      </el-select>
                    </div>-->
                    <div class="mb-10px">
                      <div class="inline-block">触发事件：</div>
                      <el-select v-model="element.event" class="w-150px" clearable placeholder="请选择触发事件">
                        <!--                        <el-option-->
                        <!--                          v-for="item in useDict('trigger_event')['trigger_event']"-->
                        <!--                          :key="item.value"-->
                        <!--                          :label="item.label"-->
                        <!--                          :value="item.value"-->
                        <!--                        />-->
                      </el-select>
                    </div>
                  </div>
                  <div class="absolute right-0 top-0">
                    <el-icon @click="delbansc(element, index)" color="#8e8e8e" size="25px"><CircleCloseFilled /></el-icon>
                  </div>
                </div>
              </template>
            </Draggable>
            <div class="mt-10px w-full">
              <el-button @click="addbansc('deviceParameter')" class="w-full" size="large" type="primary">添加版块</el-button>
            </div>
          </div>
          <!-- 其他功能 -->
          <div v-if="types?.type == 'otherfunctions'">
            <div class="mb-10px">
              <div class="inline-block">组件标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].label" placeholder="请输入组件标题" /></div>
            </div>
            <div class="mb-10px borderbottom pb-20px">
              <div class="inline-block">组件英文标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].labelEn" placeholder="请输入组件英文标题" /></div>
            </div>
            <Draggable
              :list="list[typeindex].arr"
              :animation="500"
              class="list-group bg-[#fbfcff]"
              :forceFallback="true"
              ghost-class="ghost"
              :move="getdata"
              item-key="id"
            >
              <template #item="{ element, index }">
                <div class="flex borderbottom py-20px relative">
                  <div class="w-70px">
                    <UploadImg textsub width="70px" height="70px" v-model="element.url" :limit="1" />
                    <!-- <img class="w-70px" :src="element.url" alt="" /> -->
                  </div>
                  <div class="flex-1 ml-10px color-[#333] text-16px mb-10px">
                    <div class="mb-10px">
                      <div class="inline-block">标题：</div>
                      <div class="inline-block"><el-input v-model="element.label" placeholder="请输入标题" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">物模型名称英文：</div>
                      <div class="inline-block"><el-input v-model="element.nameEn" placeholder="物模型名称英文" /></div>
                    </div>
                    <div class="mb-10px">
                      <div class="inline-block">key：</div>
                      <div class="inline-block"><el-input v-model="element.key" placeholder="请输入key" /></div>
                    </div>
                  </div>
                  <div class="absolute right-0 top-0">
                    <el-icon @click="delbansc(element, index)" color="#8e8e8e" size="25px"><CircleCloseFilled /></el-icon>
                  </div>
                </div>
              </template>
            </Draggable>
            <div class="mt-10px w-full">
              <el-button @click="addbansc('otherfunctions')" class="w-full" size="large" type="primary">添加版块</el-button>
            </div>
          </div>
          <!-- 仪表盘 -->
          <div v-if="types?.type == 'instrumentPanel'">
            <div class="mb-10px">
              <div class="inline-block">物模型属性：</div>
              <div class="inline-block">
                <!--                        <el-input v-model="element.identifier" placeholder="物模型属性" />-->
                <el-select v-model="list[typeindex].identifier" @change="changeIdentifierByChart" placeholder="物模型属性">
                  <el-option v-for="item in thingModel.properties" :key="item.identifier" :label="item.name" :value="item.identifier" />
                </el-select>
              </div>
            </div>
          </div>
          <!-- 柱形图 -->
          <div v-if="types?.type == 'histogram'">
            <div class="w-70px">
              <!-- <UploadImg
                textsub
                width="70px"
                height="70px"
                v-model="list[typeindex].color.url"
                :limit="1"
              /> -->
              <!-- <img class="w-70px" :src="element.url" alt="" /> -->
            </div>
            <div class="mb-10px">
              <div class="inline-block">标题：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].label" placeholder="请输入标题" /></div>
            </div>
            <div class="mb-10px">
              <div class="inline-block">物模型名称英文：</div>
              <div class="inline-block"><el-input v-model="list[typeindex].nameEn" placeholder="物模型英文名" /></div>
            </div>
            <div class="mb-10px">
              <div class="inline-block">柱状图顶部图片：</div>
              <div class="inline-block">
                <UploadImg textsub width="70px" height="70px" v-model="list[typeindex].bgTop" :limit="1" />
              </div>
            </div>
            <div class="mb-10px">
              <div class="inline-block">柱状图背景图片：</div>
              <div class="inline-block">
                <UploadImg textsub width="70px" height="70px" v-model="list[typeindex].bgImg" :limit="1" />
              </div>
            </div>
            <div class="mb-10px">
              <div class="inline-block">物模型属性：</div>
              <div class="inline-block">
                <!--                        <el-input v-model="element.identifier" placeholder="物模型属性" />-->
                <el-select v-model="list[typeindex].identifier" @change="changeIdentifierByChart" placeholder="物模型属性">
                  <el-option v-for="item in thingModel.properties" :key="item.identifier" :label="item.name" :value="item.identifier" />
                </el-select>
              </div>
            </div>
            <div class="mb-10px">
              <div class="inline-block">触发事件：</div>
              <el-select v-model="list[typeindex].event" class="w-150px" clearable placeholder="请选择触发事件">
                <!--                <el-option v-for="item in useDict('trigger_event')['trigger_event']" :key="item.value" :label="item.label" :value="item.value" />-->
              </el-select>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import Draggable from 'vuedraggable'
import Assembly from './components/assembly.vue'
import { useRoute } from 'vue-router'
// import { useDict } from '@/utils/dict'
import { ElMessage } from 'element-plus'
import {ProductApi} from '@/api/eiot/product'
import {ThingModelApi} from "@/api/eiot/thingmodel";

const props = defineProps({
  productInfo: {
    type: Object,
    default: () => {}
  }
})
const thingModel = ref()
const showModelType = ref(0)
const route = useRoute()
const list = ref([])
const assembly = ref([
  { label: '设备标题', type: 'parameter', color: '', color1: '' },
  {
    label: '实时参数',
    labelEn: '',
    type: 'realtime',
    arr: [
      {
        label: '实时功率',
        nameEn: '',
        url: '', //图标
        key: '',
        identifier: '', //物模型属性
        // accessMode: 'rw', //rw/读写，r只读
        description: '', //备注
        bg: '#f2fbfc',
        min: '',
        max: '',
        // event: '', //触发事件
        name: '', //物模型名称
        // property: {
        //   identifier: '', //物模型属性
        //   dataType: {
        //     type: 'int' //数据类型
        //   }
        // },
        unit: '' //单位
      }
    ]
  },
  // {
  //   label: '设备模式',
  //   labelEn: '',
  //   type: 'deviceMode',
  //   moder: true,
  //   arr: [
  //     {
  //       label: '温控',
  //       nameEn: '',
  //       url: '',
  //       identifier: '' //物模型属性
  //     }
  //   ]
  // },
  {
    label: '设置参数',
    labelEn: '',
    type: 'deviceParameter',
    arr: [
      {
        label: '温控',
        nameEn: '',
        key: '',
        accessMode: 'rw', //rw/读写，r只读
        description: '', //备注
        // showMode: '',
        // min: '',
        // max: '',
        event: '', //触发事件
        name: '', //物模型名称
        identifier: '', //物模型属性
        // unit: '' //单位
      }
    ]
  },
  // {
  //   label: '其他功能',
  //   labelEn: '',
  //   type: 'otherfunctions',
  //   arr: [
  //     {
  //       label: '温控',
  //       nameEn: '',
  //       key: '',
  //       unit: ''
  //     }
  //   ]
  // },
  // {
  //   label: '进度条',
  //   type: 'Progress',
  //   color: 'red',
  //   title: '名称',
  //   min: 1,
  //   max: 10,
  //   unit: '单位',
  //   color: '',
  //   color1: ''
  // },
  { label: '仪表盘', type: 'instrumentPanel', min: '', max: '', unit: '' },
  {
    label: '柱形图',
    nameEn: '',
    type: 'histogram',
    url: '',
    bgTop: '',
    bgImg: '',
    accessMode: 'rw',
    unit: '',
    min: '',
    max: '',
    event: '',
    types: ''
  }
])

const tags = ref(null)
const activeindex = ref(null)
const editdata = ref({})
const types = ref('')
const typeindex = ref('')

watch(showModelType, (newVal)=>{
  getDetail(newVal)
})
const onMoveCallback = (val) => {
  activeindex.value = val.moved.newIndex
  console.log('拖动前的索引 :' + val.moved.oldIndex)
  console.log('拖动后的索引 :' + val.moved.newIndex)
}

const getdata = (val) => {
  console.log(val.draggedContext.element)
}

const addbansc = (type) => {
  if (type == 'realtime') {
    list.value[typeindex.value].arr.push({
      label: '实时功率',
      url: '', //图标
      key: '',
      // accessMode: 'rw', //rw/读写，r只读
      description: '', //备注
      bg: '#f2fbfc',
      min: '',
      max: '',
      // showMode: '',
      nameEn: '',
      event: '', //触发事件
      name: '', //物模型名称
      identifier: '', //物模型属性
      // property: {
      //   identifier: '', //物模型属性
      //   dataType: {
      //     type: 'int' //数据类型
      //   }
      // },
      unit: '' //单位
    })
  } else if (type == 'deviceParameter') {
    list.value[typeindex.value].arr.push(
      {
        label: '温控',
        nameEn: '',
        key: '',
        accessMode: 'rw', //rw/读写，r只读
        description: '', //备注
        // showMode: '',
        // min: '',
        // max: '',
        event: '', //触发事件
        name: '', //物模型名称
        identifier: '', //物模型属性
        // unit: '' //单位
      })
  } else {
    list.value[typeindex.value].arr.push({
      label: '',
      url: '',
      bg: '#f2fbfc',
      identifier: '', //物模型属性
      unit: '',
      key: ''
    })
  }
}

const delbansc = (element, index) => {
  list.value[typeindex.value].arr.splice(index, 1)
}

const submit = async () => {
  const modelStr = toRaw(list.value); // 获取原始对象
  console.log("list", modelStr)
   ProductApi.editProductShowModel({
    productKey: props.productInfo.productKey,
    cnf: JSON.stringify(modelStr),
    typ: showModelType
  }).then((res)=>{
       ElMessage({
         message: '操作成功',
         type: 'success'
       })
        getDetail(showModelType)
     }
   )

}

//点击组件获取焦点
const addActiv = (index, e) => {
  activeindex.value = index
  types.value = e
  typeindex.value = index
}

const addassembly = (e) => {
  // 深拷贝
  const clonedComponent = JSON.parse(JSON.stringify(e))
  list.value = list.value.concat(clonedComponent)
}

const delClick = (element, index) => {
  if (index >= 0 && index < list.value.length) {
    list.value.splice(index, 1)
    activeindex.value = null
  }
}

const deepCopy = (obj) => JSON.parse(JSON.stringify(obj))

const addClick = (e, index) => {
  // 深拷贝要添加的元素
  const copiedElement = deepCopy(e)

  // 在指定的索引位置插入深拷贝后的元素
  list.value.splice(index + 1, 0, copiedElement)
}

const getDetail = async (idx) => {
  const { data } = await ProductApi.getShowModel(props.productInfo.productKey)
  console.log("data:", data)
  if (data) {
    list.value = JSON.parse(data[idx.value]?.cnf)
    console.log(list.value)
  }else{
    list.value = JSON.parse('[{"label":"设备标题","type":"parameter","color":"","color1":""},{"label":"仪表盘","type":"instrumentPanel","min":"1","max":"8","unit":"bar"},{"label":"实时参数","labelEn":"Online parameters","type":"realtime","arr":[{"label":"实时功率","nameEn":"Power","url":"","key":"","accessMode":"r","description":"","bg":"#f2fbfc","min":"0","max":"1000","event":"none","name":"","property":{"identifier":"","dataType":{"type":"int"}},"unit":"W","type":"int","identifier":"power"},{"label":"实时转速","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/da23811edb0a570f16a9287c0b30d6e8b02b19705b7367a9286455222cf54f78.png","key":"","accessMode":"r","description":"","bg":"#f2fbfc","min":"0","max":"5000","nameEn":"Speed","event":"none","name":"","identifier":"speed","property":{"identifier":"","dataType":{"type":"int"}},"unit":"Rpm","type":"int"},{"label":"实时电压","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/f1c1e46101bd38a40d244ed1f98e794af9d356c80f07f7c8e241f90134b0d0c2.png","key":"","accessMode":"r","description":"","bg":"#f2fbfc","min":"0","max":"300","nameEn":"Voltage","event":"none","name":"","identifier":"vdc","property":{"identifier":"","dataType":{"type":"int"}},"unit":"V","type":"int"},{"label":"实时水温","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/4dd6c70ac5600dece2fb9a8ec04f87a16dae08fe030f5ef279b0ef8bddb7fd9d.png","key":"","accessMode":"r","description":"","bg":"#f2fbfc","min":"0","max":"100","nameEn":"waterTemp","event":"none","name":"","identifier":"waterT","property":{"identifier":"","dataType":{"type":"int"}},"unit":"°C","type":"int"},{"label":"实时IPM温度","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/4dd6c70ac5600dece2fb9a8ec04f87a16dae08fe030f5ef279b0ef8bddb7fd9d.png","key":"","accessMode":"rw","description":"","bg":"#f2fbfc","min":"30","max":"100","nameEn":"Ipm Temp","event":"none","name":"","identifier":"ipmTemp","property":{"identifier":"","dataType":{"type":"int"}},"unit":"°C","type":"int"},{"label":"实时电机温度","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/4dd6c70ac5600dece2fb9a8ec04f87a16dae08fe030f5ef279b0ef8bddb7fd9d.png","key":"","accessMode":"r","description":"","bg":"#f2fbfc","min":"","max":"","showMode":"","nameEn":"Motor Temperature","event":"none","name":"","identifier":"mcTemp","property":{"identifier":"","dataType":{"type":"int"}},"unit":"°C","type":"int"}]},{"label":"设备模式","labelEn":"Device Mode","type":"deviceMode","moder":true,"arr":[{"label":"自动","nameEn":"Auto","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/560c1354e2455211dcd11513dfb67060ef30b2a1350c8a1c4fe193f83dd69394.png","identifier":"auto"},{"label":"手动","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/4118213092459b29abd6e9f60d9de6f5cfa023b2c280b2a23b9de4e4e29af7ae.png","bg":"#f2fbfc","identifier":"hand","unit":"","key":"","nameEn":"Manual"}]},{"label":"设置参数","labelEn":"Set Parameters","type":"deviceParameter","arr":[{"label":"设置压力","nameEn":"Set Pressure","key":"","accessMode":"rw","description":"","min":"1","max":"8","event":"","name":"","property":{"identifier":"","dataType":{"type":"int"}},"unit":"Bar","identifier":"pressure","type":"int"},{"label":"启动压力","url":"","bg":"#f2fbfc","identifier":"startPressure","unit":"Bar","key":"","nameEn":"Start Pressure","accessMode":"rw","min":"1","max":"8","type":"int","event":"click"},{"label":"缺水压力","url":"","bg":"#f2fbfc","identifier":"emtyRunPressure","unit":"Bar","key":"","nameEn":"Low Water Pressure","accessMode":"rw","min":"1","max":"2.5","type":"int","event":"click"},{"label":"电机温度保护","url":"","bg":"#f2fbfc","identifier":"electronicTMax","unit":"°C","key":"","nameEn":"Motor Temperature Protection ","accessMode":"rw","type":"int","event":"click"},{"label":"电机温度复位","url":"","bg":"#f2fbfc","identifier":"electronicTMaxReset","unit":"°C","key":"","nameEn":"Motor Temperature Reset","accessMode":"rw","type":"int","event":"click"},{"label":"手动挡位","url":"","bg":"#f2fbfc","identifier":"handMode","unit":"","key":"","nameEn":"Manual Gear","accessMode":"rw","type":"int","event":"click"},{"label":"童锁","url":"","bg":"#f2fbfc","identifier":"keyLock","unit":"","key":"","nameEn":"Child Lock","accessMode":"rw","type":"int","event":"click"}]},{"label":"其他功能","labelEn":"Other ","type":"otherfunctions","arr":[{"label":"共享","nameEn":"Share","key":"share","unit":"","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/c23453fbc19457524bbcc6eb4e1464651dcb0822f823b289118e59f646d84026.png"},{"label":"转移","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/f3ace24e45f0edf8f4207489bb5437ad035de1cb1ea0919159c6f863354e0bf6.png","bg":"#f2fbfc","identifier":"","unit":"","key":"transfer","nameEn":"Transfer"},{"label":"设备升级","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/e110feb76ddf4864568aafc4ef5fbe6d2d8fa58aeb8930e7f998947b16272761.png","bg":"#f2fbfc","identifier":"","unit":"","key":"deviceUpgrade","nameEn":"MCU Upgrade"},{"label":"设备信息","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/7262e07d94730baaa091e643a5782d68ce2d03e925cbe654be1e4317fdfca99b.png","bg":"#f2fbfc","identifier":"","unit":"","key":"deviceinformation","nameEn":"Device Information"},{"label":"问题反馈","url":"https://prd-factory.openiita.com/admin-api/infra/file/5/get/edf9f1e39557f87a56844683b1b7aff49c6d809ad8af92020d1cba817a1a758d.png","bg":"#f2fbfc","identifier":"","unit":"","key":"problemfeedback","nameEn":"Problem Feedback"}]}]')

  }
}

const changeIdentifier = (val, index) => {
  const data = thingModel.value.properties.find((item) => {
    return item.identifier == val
  })
  console.log(list.value[typeindex.value])
  console.log("changeIdentifier:")
  list.value[typeindex.value].arr[index].accessMode = data.accessMode
  list.value[typeindex.value].arr[index].dataType = data.dataType
  list.value[typeindex.value].arr[index].unit = data.unit
  list.value[typeindex.value].arr[index].name = data.name
}

const changeIdentifierByChart = (val) => {

  const data = thingModel.value.properties.find((item) => {
    return item.identifier == val
  })
  // console.log(list.value[typeindex.value])
  console.log("changeIdentifierByChart", data)
  list.value[typeindex.value].accessMode = data.accessMode
  list.value[typeindex.value].dataType = data.dataType
  list.value[typeindex.value].unit = data.unit
  list.value[typeindex.value].name = data.name
}
const showModelOptions = [
  {
    value: 0,
    label: '详情页',
  },
  {
    value: 1,
    label: '配置页面',
  },
]
const getThingModel=()=>{
  ThingModelApi.getThingModelByProductKey(props.productInfo.productKey).then(res => {
    const data = res || {}
    if (data.model) {
      data.model.services = data?.model?.services || []
      data.model.properties = data?.model?.properties || []
      data.model.events = data?.model?.events || []
    }
    thingModel.value = data.model || {
      services: [],
      properties: [],
      events: [],
    }
  })
}

onMounted(async () => {
  getThingModel()

  await getDetail(showModelType)
})
</script>

<style scoped>
.drag-box .items {
  width: 100%;
  padding: 15px;
  margin-bottom: 20px;

  /* background: #e3e3e3; */
  border-radius: 8px;
  box-sizing: border-box;
}

.noRepeat {
  display: inline-block;

  /* padding: 10px; */
  text-align: center;
  background: #fff;
  border-radius: 5px;
  box-sizing: border-box;
}

.items:hover {
  transform: scale(1.01);
  box-shadow: 0 0 10px 0 rgb(24 144 255 / 30%);
  transition: all 0.2s;
}

.active {
  border: 2px solid #1890ff;
}

.heightu {
  height: calc(100vh - 250px);
}

.min-heights {
  min-height: calc(100vh - 250px);
}

.heightus {
  min-height: calc(90vh - 200px);
}

.guid3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-gap: 10px;
}

.bosxe {
  box-shadow: var(--el-box-shadow-lighter);
}

::-webkit-scrollbar {
  display: none;
}

.borderbottom {
  border-bottom: 1px solid #eee;
}
</style>
