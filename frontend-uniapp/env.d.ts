/// <reference types="@dcloudio/types" />

// UniApp 全局类型声明
declare module '*.vue' {
  import { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '*.scss' {
  const content: Record<string, string>
  export default content
}

declare module '*.css' {
  const content: Record<string, string>
  export default content
}

// Vite 环境变量类型
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly UNI_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// UniApp uni 全局对象类型（补充 @dcloudio/types 可能缺失的）
declare namespace UniApp {
  interface RequestSuccessCallbackResult {
    data: any
    statusCode: number
    header: any
    cookies?: string[]
  }
}
