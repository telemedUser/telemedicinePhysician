const { cpSync, existsSync, readFileSync, writeFileSync } = require('fs');
const { join } = require('path');

function toClassName(name) {
  return name.split('-').map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join('');
}

function toFederationName(name) {
  const camel = name.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
  return camel.charAt(0).toLowerCase() + camel.slice(1) + 'Mfe';
}

async function defaultExport(tree, options) {
  const projectName = options.name;
  const route = options.route ?? `/${projectName}`;
  const port = options.port ?? 3002;
  const federationName = toFederationName(projectName);
  const className = toClassName(projectName);
  const projectRoot = `apps/${projectName}`;
  const templateRoot = 'apps/_template-mfe';

  if (tree.exists(projectRoot)) {
    throw new Error(`Project ${projectName} already exists`);
  }

  const copyFile = (relativePath) => {
    const source = join(templateRoot, relativePath);
    const target = join(projectRoot, relativePath);
    let content = tree.read(source, 'utf-8');
    content = content
      .split('template-mfe').join(projectName)
      .split('templateMfe').join(federationName)
      .split('Template Microfrontend').join(`${className} Microfrontend`)
      .split('Template MFE').join(`${className} MFE`)
      .split('3001').join(String(port));
    tree.write(target, content);
  };

  for (const file of [
    'vite.config.mts',
    'project.json',
    'package.json',
    'tsconfig.json',
    'index.html',
    'nginx.conf',
    'Dockerfile',
    'src/main.tsx',
    'src/App.tsx',
  ]) {
    copyFile(file);
  }

  const manifestPath = 'apps/shell/public/config/mfe-manifest.json';
  if (tree.exists(manifestPath)) {
    const manifest = JSON.parse(tree.read(manifestPath, 'utf-8'));
    manifest.remotes.push({
      name: federationName,
      url: `http://localhost:${port}/assets/remoteEntry.js`,
      route,
      enabled: true,
      module: './App',
    });
    tree.write(manifestPath, JSON.stringify(manifest, null, 2) + '\n');
  }
}

module.exports = defaultExport;
module.exports.default = defaultExport;
