<template>
  <div class="text-left">
    <!-- 参数 -->
    <div class="h-[140px] bg-[#fff]" v-if="props.elements.type == 'parameter'">
      <div class="w-full absolute top-0 left-0 h-21px relative z-1">
        <img src="@/assets/imgs/eiot/topheade.png" alt="" />
      </div>
      <div class="absolute top-0 h-100% left-0 w-full" :style="{ background: getGradientColor1(elements.color, elements.color1) }">
        <div class="px-20px">
          <div class="pt-50px text-[26px] font-500">设备</div>
          <div class="w-20px h-3px mt-5px" style="background: linear-gradient(to bottom, #c85f65, #e67e50)"></div>
          <div class="pt-10px color-[#666] text-[13px]">IITA</div>
          <div>
            <span class="color-[#1681FE] text-[12px] disabled-inline mr-10px bg-[#d2e4fe] px-5px rounded-2px">无线</span>
            <span class="color-[#029D40] text-[12px] disabled-inline mr-10px bg-[#d1e6e9] px-5px rounded-2px">在线</span>
          </div>
        </div>
      </div>
    </div>
    <!-- 实时参数 -->
    <div class="bosxe p-15px" v-if="props.elements.type == 'realtime'">
      <div class="color-[#333] text-16px font-500 titles relative pl-15px">
        {{ elements.label }}
      </div>
      <div class="goru3">
        <div class="mt-10px" v-for="(item1, index1) in props.elements.arr" :key="index1">
          <div class="p-10px rounded-5px" :style="{ background: item1.bg }">
            <img class="w-20px h-20px rounded-[50%]" :src="item1.url" alt="" />
            <div class="color-[#666] text-14px mt-8px">{{ item1.label }}</div>
            <div class="color-[#333] text-16px font-500">220{{ item1.unit }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设备模式 -->
    <div class="bosxe p-15px" v-if="props.elements.type == 'deviceMode'">
      <div class="color-[#333] text-16px font-500 titles relative flex justify-center items-center">
        {{ elements.label }} <el-icon class="ml-5px" color="#6fa7fb"><WarningFilled /></el-icon>
        <div class="ml-auto">
          <el-switch v-model="moder" inline-prompt active-text="开" inactive-text="关" active-color="#51b367" size="large" />
        </div>
      </div>
      <div class="goru3">
        <div class="mt-10px text-center" v-for="(item1, index1) in props.elements.arr" :key="index1">
          <div class="w-50px h-50px bg-[#f5f5f5] rounded-[50%] relative inline-block brores">
            <img class="w-20px h-20px rounded-[50%] absolute left-1/2 top-1/2 transform -translate-x-1/2 -translate-y-1/2" :src="item1.url" alt="" />
          </div>
          <div class="text-center color-[#666] text-14px">
            {{ item1.label }}
          </div>
        </div>
      </div>
    </div>

    <!-- 设置参数 -->
    <div class="bosxe p-15px bg-[#fff]" v-if="props.elements.type == 'deviceParameter'">
      <div class="color-[#333] text-16px font-500 titles relative flex justify-center items-center">
        {{ elements.label }}
        <div class="ml-auto color-[#666] text-14px">更多</div>
      </div>
      <div class="mt-15px grid grid-cols-2 gap-4">
        <div v-for="(item1, index1) in props.elements.arr" :key="index1" class="bg-[#F8F9FC] flex p-10px rounded-5px flex">
          <div class="color-[#666] text-15px">{{ item1.label }}</div>
          <div class="text-right text-15px ml-auto font-500">
            {{ item1.unit }}
            <div class="inline-block" style="vertical-align: middle">
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 其他功能 -->
    <div class="bosxe p-15px bg-[#fff]" v-if="props.elements.type == 'otherfunctions'">
      <div class="color-[#333] text-16px font-500 titles relative flex justify-center items-center">
        {{ elements.label }}
        <div class="ml-auto color-[#666] text-14px">更多</div>
      </div>
      <div class="mt-15px goru3">
        <div v-for="(item1, index1) in props.elements.arr" :key="index1" class="bg-[#F8F9FC] flex p-10px rounded-5px flex">
          <div v-if="item1.url"><img class="w-20px h-20px mr-5px" style="vertical-align: text-bottom" :src="item1.url" alt="" /></div>
          <div class="color-[#666] text-15px text-center w-full">{{ item1.label }}</div>
        </div>
      </div>
    </div>

    <!-- 仪表板 -->
    <div class="w-full flex justify-center items-center" v-if="props.elements.type == 'Progress'">
      <div class="flex mr-10px">{{ elements.title }}</div>
      <div class="flex-[1] mr-10px relative">
        <div class="w-[100%] h-6px rounded-5px bg-[var(--el-border-color-lighter)] absolute left-0 top-1/2 transform -translate-y-1/2"></div>
        <div class="w-[80%] h-6px rounded-5px relative" :style="{ background: getGradientColor(elements.color, elements.color1) }"></div>
      </div>
      <div class="">{{ elements.unit }}</div>
    </div>
    <!-- 柱形图 -->
    <div class="w-full flex justify-center items-center" v-if="props.elements.type == 'histogram'">
      <img class="w-full" src="@/assets/imgs/eiot/suxu.jpg" alt="" />
    </div>
    <!-- 进度条 -->
    <div v-else-if="props.elements.type == 'instrumentPanel'">
      <img class="w-full" src="@/assets/imgs/eiot/instrumentPanel.jpg" alt="" />
    </div>
  </div>
</template>
<script setup>
const props = defineProps({
  elements: {
    type: Object
  },
  indexs: {
    type: Number
  }
})

const moder = ref(true)
const getGradientColor = (color1, color2) => {
  // console.log(color1)
  // console.log(color2)
  if (color1 && color2) {
    return 'linear-gradient(to right, ' + color1 + ', ' + color2 + ')'
  } else {
    return color1
  }
}

const getGradientColor1 = (color1, color2) => {
  // console.log(color1)
  // console.log(color2)
  if (color1 && color2) {
    return 'linear-gradient(to bottom, ' + color1 + ', ' + color2 + ',' + '#fff' + ')'
  } else {
    return color1
  }
}
</script>
<style scoped>
.bosxe {
  box-shadow: 0 4px 5px 0 #ccd4e02b;
}

.titles::before {
  position: absolute;
  top: 50%;
  left: -15px;
  display: inline-block;
  width: 5px;
  height: 20px;
  background: #4e88f9;
  border-top-right-radius: 8px;
  border-bottom-right-radius: 8px;
  content: '';
  transform: translate(0, -50%);
}

.goru3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-gap: 10px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(50%, 1fr)); /* 一行两个，并自动换行 */
  gap: 10px; /* 设置子元素之间的间距 */
}

.brores {
  border: 1px solid #eee;
}
</style>
