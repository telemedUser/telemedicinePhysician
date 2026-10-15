import { join } from 'path';
import { Tree, formatFiles, generateFiles, names } from '@nx/devkit';

interface MfeGeneratorSchema {
  name: string;
  route?: string;
  port?: number;
}

export default async function mfeGenerator(tree: Tree, options: MfeGeneratorSchema) {
  const normalized = names(options.name);
  const projectName = normalized.fileName;
  const className = normalized.className;
  const propertyName = normalized.propertyName;
  const route = options.route ?? `/${projectName}`;
  const port = options.port ?? 3002;
  const federationName = `${propertyName}Mfe`;
  const projectRoot = `apps/${projectName}`;

  if (tree.exists(projectRoot)) {
    throw new Error(`Project ${projectName} already exists at ${projectRoot}`);
  }

  generateFiles(tree, join(__dirname, 'files'), projectRoot, {
    projectName,
    className,
    propertyName,
    federationName,
    route,
    port,
    tmpl: '',
  });

  const manifestPath = 'apps/shell/public/config/mfe-manifest.json';
  if (tree.exists(manifestPath)) {
    const manifest = JSON.parse(tree.read(manifestPath, 'utf-8')!);
    manifest.remotes.push({
      name: federationName,
      url: `http://localhost:${port}/assets/remoteEntry.js`,
      route,
      enabled: true,
      module: './App',
    });
    tree.write(manifestPath, JSON.stringify(manifest, null, 2) + '\n');
  }

  await formatFiles(tree);
}
