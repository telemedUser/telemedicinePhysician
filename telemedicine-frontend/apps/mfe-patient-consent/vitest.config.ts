import { createVitestConfig } from '../../vitest.shared';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const dir = dirname(fileURLToPath(import.meta.url));
export default createVitestConfig(dir);
