#!/usr/bin/env python3
"""Scaffold telemedicine frontend apps (shells + MFEs) and update workspace config."""
import json
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
created: list[str] = []


def w(rel_path: str, content: str) -> None:
    full = ROOT / rel_path.replace("/", os.sep)
    full.parent.mkdir(parents=True, exist_ok=True)
    full.write_text(content, encoding="utf-8", newline="\n")
    created.append(rel_path.replace("\\", "/"))


def to_federation_name(dir_name: str) -> str:
    parts = dir_name.split("-")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def mfe_app(dir_name: str, port: int, title: str, display_name: str) -> None:
    fed_name = to_federation_name(dir_name)
    w(
        f"apps/{dir_name}/package.json",
        json.dumps(
            {
                "name": dir_name,
                "version": "0.1.0",
                "private": True,
                "dependencies": {
                    "@telemedicine/shared-types": "workspace:*",
                    "@telemedicine/shared-ui": "workspace:*",
                    "react": "^18.3.1",
                    "react-dom": "^18.3.1",
                },
            },
            indent=2,
        )
        + "\n",
    )
    w(
        f"apps/{dir_name}/project.json",
        json.dumps(
            {
                "name": dir_name,
                "$schema": "../../node_modules/nx/schemas/project-schema.json",
                "sourceRoot": f"apps/{dir_name}/src",
                "projectType": "application",
                "tags": ["scope:app", "type:mfe"],
                "targets": {
                    "build": {
                        "executor": "nx:run-commands",
                        "outputs": [f"{{workspaceRoot}}/dist/apps/{dir_name}"],
                        "options": {"command": "vite build", "cwd": f"apps/{dir_name}"},
                    },
                    "serve": {
                        "executor": "nx:run-commands",
                        "options": {"command": "vite", "cwd": f"apps/{dir_name}"},
                    },
                    "preview": {
                        "executor": "nx:run-commands",
                        "options": {"command": "vite preview", "cwd": f"apps/{dir_name}"},
                    },
                },
            },
            indent=2,
        )
        + "\n",
    )
    w(
        f"apps/{dir_name}/tsconfig.json",
        json.dumps(
            {
                "extends": "../../tsconfig.base.json",
                "compilerOptions": {
                    "outDir": f"../../dist/out-tsc/apps/{dir_name}",
                    "types": ["vite/client"],
                },
                "include": ["src/**/*.ts", "src/**/*.tsx"],
                "exclude": ["node_modules"],
            },
            indent=2,
        )
        + "\n",
    )
    w(
        f"apps/{dir_name}/index.html",
        f"""<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>{title}</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
""",
    )
    w(
        f"apps/{dir_name}/src/main.tsx",
        """import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
""",
    )
    w(
        f"apps/{dir_name}/src/App.tsx",
        f"""export default function App() {{
  return (
    <div>
      <h2>{display_name}</h2>
      <p>Microfrontend remote loaded via Module Federation.</p>
    </div>
  );
}}
""",
    )
    w(
        f"apps/{dir_name}/vite.config.mts",
        f"""import {{ defineConfig }} from 'vite';
import react from '@vitejs/plugin-react';
import {{ federation }} from '@module-federation/vite';
import {{ resolve, dirname }} from 'path';
import {{ fileURLToPath }} from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({{
  root: __dirname,
  cacheDir: '../../node_modules/.vite/{dir_name}',
  plugins: [
    react(),
    federation({{
      name: '{fed_name}',
      filename: 'remoteEntry.js',
      exposes: {{
        './App': './src/App.tsx',
      }},
      shared: {{
        react: {{ singleton: true }},
        'react-dom': {{ singleton: true }},
      }},
      dts: false,
    }}),
  ],
  server: {{
    port: {port},
    strictPort: true,
    origin: 'http://localhost:{port}',
    proxy: {{
      '/api': {{
        target: 'http://localhost:8080',
        changeOrigin: true,
      }},
    }},
  }},
  preview: {{
    port: {port},
    strictPort: true,
  }},
  build: {{
    outDir: '../../dist/apps/{dir_name}',
    emptyOutDir: true,
    target: 'esnext',
  }},
  resolve: {{
    alias: {{
      '@telemedicine/shared-types': resolve(__dirname, '../../packages/shared-types/src/index.ts'),
      '@telemedicine/shared-ui': resolve(__dirname, '../../packages/shared-ui/src/index.tsx'),
    }},
  }},
}});
""",
    )


def shell_app(dir_name: str, port: int, title: str, tag: str) -> None:
    fed_name = to_federation_name(dir_name)
    w(
        f"apps/{dir_name}/package.json",
        json.dumps(
            {
                "name": dir_name,
                "version": "0.1.0",
                "private": True,
                "dependencies": {
                    "@telemedicine/mfe-registry": "workspace:*",
                    "@telemedicine/shared-types": "workspace:*",
                    "@telemedicine/shared-ui": "workspace:*",
                    "react": "^18.3.1",
                    "react-dom": "^18.3.1",
                    "react-router-dom": "^6.28.0",
                },
            },
            indent=2,
        )
        + "\n",
    )
    w(
        f"apps/{dir_name}/project.json",
        json.dumps(
            {
                "name": dir_name,
                "$schema": "../../node_modules/nx/schemas/project-schema.json",
                "sourceRoot": f"apps/{dir_name}/src",
                "projectType": "application",
                "tags": ["scope:app", tag],
                "targets": {
                    "build": {
                        "executor": "nx:run-commands",
                        "outputs": [f"{{workspaceRoot}}/dist/apps/{dir_name}"],
                        "options": {"command": "vite build", "cwd": f"apps/{dir_name}"},
                    },
                    "serve": {
                        "executor": "nx:run-commands",
                        "options": {"command": "vite", "cwd": f"apps/{dir_name}"},
                    },
                    "preview": {
                        "executor": "nx:run-commands",
                        "options": {"command": "vite preview", "cwd": f"apps/{dir_name}"},
                    },
                },
            },
            indent=2,
        )
        + "\n",
    )
    w(
        f"apps/{dir_name}/tsconfig.json",
        json.dumps(
            {
                "extends": "../../tsconfig.base.json",
                "compilerOptions": {
                    "outDir": f"../../dist/out-tsc/apps/{dir_name}",
                    "types": ["vite/client"],
                },
                "include": ["src/**/*.ts", "src/**/*.tsx"],
                "exclude": ["node_modules"],
            },
            indent=2,
        )
        + "\n",
    )
    w(
        f"apps/{dir_name}/index.html",
        f"""<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>{title}</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
""",
    )
    w(
        f"apps/{dir_name}/src/main.tsx",
        """import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>
);
""",
    )
    w(
        f"apps/{dir_name}/vite.config.mts",
        f"""import {{ defineConfig }} from 'vite';
import react from '@vitejs/plugin-react';
import {{ federation }} from '@module-federation/vite';
import {{ resolve, dirname }} from 'path';
import {{ fileURLToPath }} from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({{
  root: __dirname,
  cacheDir: '../../node_modules/.vite/{dir_name}',
  plugins: [
    react(),
    federation({{
      name: '{fed_name}',
      remotes: {{}},
    }}),
  ],
  server: {{
    port: {port},
    strictPort: true,
    proxy: {{
      '/api': {{
        target: 'http://localhost:8080',
        changeOrigin: true,
      }},
    }},
  }},
  preview: {{
    port: {port},
    strictPort: true,
  }},
  build: {{
    outDir: '../../dist/apps/{dir_name}',
    emptyOutDir: true,
    target: 'esnext',
  }},
  resolve: {{
    alias: {{
      '@telemedicine/mfe-registry': resolve(__dirname, '../../packages/mfe-registry/src/index.ts'),
      '@telemedicine/shared-types': resolve(__dirname, '../../packages/shared-types/src/index.ts'),
      '@telemedicine/shared-ui': resolve(__dirname, '../../packages/shared-ui/src/index.tsx'),
    }},
  }},
}});
""",
    )


def main() -> None:
    shell_app("physician-shell", 3010, "Physician Portal", "type:physician-shell")
    w(
        "apps/physician-shell/src/App.tsx",
        """import { Link, Route, Routes } from 'react-router-dom';
import { ShellLayout } from '@telemedicine/shared-ui';

function Home() {
  return (
    <div>
      <h2>Physician Portal</h2>
      <p>Simplified shell for physician-facing microfrontends.</p>
      <ul>
        <li><Link to="/consultation">Consultation</Link></li>
        <li><Link to="/documentation">Documentation</Link></li>
        <li><Link to="/patient-context">Patient Context</Link></li>
      </ul>
    </div>
  );
}

function Placeholder({ name }: { name: string }) {
  return <p>Load <strong>{name}</strong> remote via Module Federation.</p>;
}

export function App() {
  return (
    <ShellLayout title="Physician Portal">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/consultation" element={<Placeholder name="mfe-physician-consultation" />} />
        <Route path="/documentation" element={<Placeholder name="mfe-physician-documentation" />} />
        <Route path="/patient-context" element={<Placeholder name="mfe-physician-patient-context" />} />
      </Routes>
    </ShellLayout>
  );
}
""",
    )

    shell_app("admin-shell", 3020, "Admin Portal", "type:admin-shell")
    w(
        "apps/admin-shell/src/App.tsx",
        """import { Link, Route, Routes } from 'react-router-dom';
import { ShellLayout } from '@telemedicine/shared-ui';

function Home() {
  return (
    <div>
      <h2>Admin Portal</h2>
      <p>Simplified shell for admin-facing microfrontends.</p>
      <ul>
        <li><Link to="/config">Config</Link></li>
        <li><Link to="/dashboards">Dashboards</Link></li>
        <li><Link to="/audit">Audit</Link></li>
        <li><Link to="/access">Access</Link></li>
        <li><Link to="/monitoring">Monitoring</Link></li>
      </ul>
    </div>
  );
}

function Placeholder({ name }: { name: string }) {
  return <p>Load <strong>{name}</strong> remote via Module Federation.</p>;
}

export function App() {
  return (
    <ShellLayout title="Admin Portal">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/config" element={<Placeholder name="mfe-admin-config" />} />
        <Route path="/dashboards" element={<Placeholder name="mfe-admin-dashboards" />} />
        <Route path="/audit" element={<Placeholder name="mfe-admin-audit" />} />
        <Route path="/access" element={<Placeholder name="mfe-admin-access" />} />
        <Route path="/monitoring" element={<Placeholder name="mfe-admin-monitoring" />} />
      </Routes>
    </ShellLayout>
  );
}
""",
    )

    patient_mfes = [
        ("mfe-patient-consent", 3002, "Patient Consent", "/patient/consent"),
        ("mfe-patient-identity", 3003, "Patient Identity", "/patient/identity"),
        ("mfe-patient-waiting-room", 3004, "Patient Waiting Room", "/patient/waiting-room"),
        ("mfe-patient-consultation", 3005, "Patient Consultation", "/patient/consultation"),
        ("mfe-patient-documents", 3006, "Patient Documents", "/patient/documents"),
        ("mfe-patient-survey", 3007, "Patient Survey", "/patient/survey"),
    ]
    for dir_name, port, title, _route in patient_mfes:
        mfe_app(dir_name, port, title, title)

    physician_mfes = [
        ("mfe-physician-consultation", 3011, "Physician Consultation"),
        ("mfe-physician-documentation", 3012, "Physician Documentation"),
        ("mfe-physician-patient-context", 3013, "Physician Patient Context"),
    ]
    for dir_name, port, title in physician_mfes:
        mfe_app(dir_name, port, title, title)

    admin_mfes = [
        ("mfe-admin-config", 3021, "Admin Config"),
        ("mfe-admin-dashboards", 3022, "Admin Dashboards"),
        ("mfe-admin-audit", 3023, "Admin Audit"),
        ("mfe-admin-access", 3024, "Admin Access"),
        ("mfe-admin-monitoring", 3025, "Admin Monitoring"),
    ]
    for dir_name, port, title in admin_mfes:
        mfe_app(dir_name, port, title, title)

    manifest_remotes = [
        {
            "name": to_federation_name(dir_name),
            "url": f"http://localhost:{port}/assets/remoteEntry.js",
            "route": route,
            "enabled": True,
            "module": "./App",
        }
        for dir_name, port, _title, route in patient_mfes
    ]

    w(
        "apps/shell/public/config/mfe-manifest.json",
        json.dumps(
            {
                "remotes": [
                    {
                        "name": "templateMfe",
                        "url": "http://localhost:3001/assets/remoteEntry.js",
                        "route": "/template",
                        "enabled": True,
                        "module": "./App",
                    },
                    *manifest_remotes,
                ]
            },
            indent=2,
        )
        + "\n",
    )

    paths = {
        "@telemedicine/mfe-registry": ["packages/mfe-registry/src/index.ts"],
        "@telemedicine/shared-types": ["packages/shared-types/src/index.ts"],
        "@telemedicine/shared-ui": ["packages/shared-ui/src/index.ts"],
        "@telemedicine/api-client": ["packages/api-client/src/index.ts"],
        "@telemedicine/auth": ["packages/auth/src/index.ts"],
        "@telemedicine/session-context": ["packages/session-context/src/index.tsx"],
        "@telemedicine/video-adapter": ["packages/video-adapter/src/index.ts"],
        "@telemedicine/chat-adapter": ["packages/chat-adapter/src/index.ts"],
        "@telemedicine/telemetry-adapter": ["packages/telemetry-adapter/src/index.ts"],
        "@telemedicine/consent-kit": ["packages/consent-kit/src/index.tsx"],
        "@telemedicine/device-test": ["packages/device-test/src/index.ts"],
        "@telemedicine/i18n": ["packages/i18n/src/index.ts"],
        "@telemedicine/feature-flags": ["packages/feature-flags/src/index.ts"],
        "@telemedicine/observability-web": ["packages/observability-web/src/index.tsx"],
        "@telemedicine/design-system": ["packages/design-system/src/index.tsx"],
    }

    w(
        "tsconfig.base.json",
        json.dumps(
            {
                "compilerOptions": {
                    "composite": True,
                    "declarationMap": True,
                    "emitDeclarationOnly": True,
                    "importHelpers": True,
                    "isolatedModules": True,
                    "lib": ["ES2022", "DOM", "DOM.Iterable"],
                    "module": "ESNext",
                    "moduleResolution": "bundler",
                    "noEmitOnError": True,
                    "noFallthroughCasesInSwitch": True,
                    "noImplicitOverride": True,
                    "noImplicitReturns": True,
                    "noUnusedLocals": True,
                    "skipLibCheck": True,
                    "strict": True,
                    "target": "ES2022",
                    "jsx": "react-jsx",
                    "baseUrl": ".",
                    "paths": paths,
                }
            },
            indent=2,
        )
        + "\n",
    )

    print(json.dumps({"count": len(created), "files": sorted(created)}, indent=2))


if __name__ == "__main__":
    main()
