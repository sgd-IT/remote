<template>
  <iframe :src="channelUrl"></iframe>
</template>

<script setup>
const props = defineProps({
  deviceInfo: {
    deviceName: String,
    default: () => {}
  }
})


// const token = ref('eyJhbGciOiJSUzI1NiIsImtpZCI6IjNlNzk2NDZjNGRiYzQwODM4M2E5ZWVkMDlmMmI4NWFlIn0.eyJqdGkiOiJnWjVVLWFRV2RfY1dQTWFXbWJzNTVnIiwiaWF0IjoxNzEzOTUyMjEzLCJuYmYiOjE3MTM5NTIyMTMsInN1YiI6ImxvZ2luIiwiYXVkIjoiQXVkaWVuY2UiLCJ1c2VyTmFtZSI6ImFkbWluIiwiYXBpS2V5SWQiOjEyfQ.mT6pTSvJhJ0d8dAnItUwK6pKPcOjLOhGW0Ot8igZ62O1I4a4Vssgdi5Z17LpS99t-LrOUDofbtq6h77ofX4FaUsR1h4SJWRPGyo_cX3VhbgCvyJ-28RJxMiEAGkdUGYMVG8fVa4AgpGe5S2PAWIVdSLgKZ6XFFKljkBspGCrO_8jbf1OUusjmSKUqhq4n69YYB3vsDb5aRipDTKELjhwKtTPENOIzOuUOsPmZg9kyOngwTErgEjzlHMPs0pmHedMqjfJ5CrBi694Sw4njpjeJoy32SunUysg7YUOtmS0DI2Evh-43B9pg8_9khOCAyRYjJsRl9TUqp-ihD9h_xH81w')
const token = import.meta.env.VITE_WVP_TOKEN
const httpsurl = import.meta.env.VITE_WVP_API
const channelUrl = httpsurl  + '#/channelList/'+props.deviceInfo.deviceName+'/0?token='+token
</script>

<style scoped>
iframe {
  width: 100%;
  height: 800px;
}
</style>
