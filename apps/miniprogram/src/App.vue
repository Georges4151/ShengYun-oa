<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 小程序启动占位：后续可做 token 恢复、设备绑定等
  console.log('CEAMS 小程序启动')
})
</script>

<style>
/* 全局设计基础层：先令牌、后基础样式，编译后会内联进 app.wxss */
@import './styles/theme.css';
@import './styles/common.css';
</style>