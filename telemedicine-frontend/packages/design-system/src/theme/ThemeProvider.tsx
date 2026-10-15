import { createContext, useContext, type ReactNode } from 'react';
import './tokens.css';

export type ThemeName = 'patient' | 'clinical';

const ThemeContext = createContext<ThemeName>('patient');

export interface ThemeProviderProps {
  theme?: ThemeName;
  children: ReactNode;
}

export function ThemeProvider({ theme = 'patient', children }: ThemeProviderProps) {
  return (
    <ThemeContext.Provider value={theme}>
      <div data-theme={theme} style={{ minHeight: '100%' }}>
        {children}
      </div>
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeName {
  return useContext(ThemeContext);
}
