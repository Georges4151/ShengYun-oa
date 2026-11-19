/**
 * 前台暂时隐藏的模块。
 *
 * 仅关闭页面入口与直接访问，后端接口与权限数据保持不变；
 * 需要恢复时把对应路径从这里移除即可。
 */
const HIDDEN_MODULE_PATHS = ['/seal']

/** 判断路径是否属于当前隐藏的模块（含子路径）。 */
export function isModuleHidden(path: string): boolean {
  return HIDDEN_MODULE_PATHS.some((hidden) => path === hidden || path.startsWith(`${hidden}/`))
}
