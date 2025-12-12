// eslint.config.js — Flat Config für Angular 21 (ESLint v9)

import globals from 'globals';
import tseslint from 'typescript-eslint';
import angular from '@angular-eslint/eslint-plugin';
import angularTemplate from '@angular-eslint/eslint-plugin-template';
import templateParser from '@angular-eslint/template-parser';


export default [
  // Ignorierte Pfade
  {
    ignores: ['**/dist/**',
      '**/node_modules/**',
      '**/*.min.js',
      '**/src/app/core/modules/openapi/**',
      '**/*.spec.ts'],
  },

  // Basis + TypeScript-empfohlene Regeln (mit Type-Checking)
  // Hinweis: recommendedTypeChecked benötigt ein TS-Projekt (tsconfig.json)

  ...tseslint.configs.recommendedTypeChecked.map((cfg) => ({
    ...cfg,
    files: ['**/*.ts'],        // <— Begrenzung auf TS
    ignores: ['**/*.html'],    // <— Schutz, falls ein Preset doch greift
  })),

  // Angular-spezifische Regeln für .ts (Components, Directives, etc.)
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tseslint.parser,
      parserOptions: {
        project: [
          './tsconfig.json',
          './tsconfig.app.json'
        ],
        tsconfigRootDir: import.meta.dirname,
        ecmaVersion: 'latest',
        sourceType: 'module',
      },
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    plugins: {
      '@angular-eslint': angular,
      '@typescript-eslint': tseslint.plugin,
    },
    rules: {
      // Solide Defaults:
      '@angular-eslint/component-selector': [
        'error',
        {type: 'element', prefix: 'app', style: 'kebab-case'},
      ],
      '@angular-eslint/directive-selector': [
        'error',
        {type: 'attribute', prefix: 'app', style: 'camelCase'},
      ],

      // Ein paar empfehlenswerte TS-Regeln (Type-Checked Preset liefert schon viel):
      '@typescript-eslint/no-unused-vars': ['warn', {argsIgnorePattern: '^_', varsIgnorePattern: '^_'}],
      '@typescript-eslint/consistent-type-imports': ['warn', {
        prefer: 'type-imports',
        fixStyle: 'separate-type-imports'
      }],
    },
  },

  // Angular Template-Regeln für externe HTML-Dateien
  {
    files: ['**/*.html'],
    languageOptions: {
      parser: templateParser,
    },
    plugins: {
      '@angular-eslint/template': angularTemplate,
    },
    // Das Template-Plugin bringt eine empfohlene Rule-Collection mit:
    rules: {
      ...angularTemplate.configs.recommended.rules,
    },
  },
];

