/** @type {import('eslint').Linter.Config} */
module.exports = {
  root: true,
  extends: ['@telemedicine/eslint-config'],
  ignorePatterns: ['dist', 'node_modules', 'storybook-static', 'coverage'],
};
