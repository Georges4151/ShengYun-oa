import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import tseslint from 'typescript-eslint'
import globals from 'globals'

// CEAMS monorepo 统一 ESLint 配置（ESLint 9 flat config）
// 仅覆盖前端 workspace 目录，忽略 Java 后端与运行时产物。
export default tseslint.config(
  {
    ignores: [
      '**/node_modules/**',
      '**/dist/**',
      '**/dist-electron/**',
      '**/coverage/**',
      '**/.turbo/**',
      // Java 后端与遗留目录，不参与前端 lint
      'src/**',
      'target/**',
      'contracts/**',
      'backup/**',
      'reports/**',
      'logs/**',
      'vue/**',
      'app/**',
      'androidclient/**',
      'data/**',
      'receipts/**',
      'rephoto/**',
      'stamped_contracts/**',
      'temp_previews/**'
    ]
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs['flat/recommended'],
  {
    files: ['**/*.{ts,tsx,vue}'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      '@typescript-eslint/no-explicit-any': 'error',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }]
    }
  },
  {
    files: ['**/*.vue'],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser
      }
    }
  }
)