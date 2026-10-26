import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/** PrimeNG and native controls share the same blue action palette. */
export const ObservabilityPreset = definePreset(Aura, {
  semantic: {
    primary: { 50: '{blue.50}', 100: '{blue.100}', 200: '{blue.200}', 300: '{blue.300}', 400: '{blue.400}', 500: '{blue.500}', 600: '{blue.600}', 700: '{blue.700}', 800: '{blue.800}', 900: '{blue.900}', 950: '{blue.950}' },
    formField: { borderRadius: '6px', paddingX: '0.75rem', paddingY: '0.625rem' },
    colorScheme: {
      light: { primary: { color: '{blue.600}', inverseColor: '#ffffff', hoverColor: '{blue.700}', activeColor: '{blue.800}' } },
      dark: { primary: { color: '{blue.500}', inverseColor: '#ffffff', hoverColor: '{blue.400}', activeColor: '{blue.600}' } }
    }
  }
});
