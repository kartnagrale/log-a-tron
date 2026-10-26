import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  afterEach(() => { localStorage.removeItem('logatron.theme'); document.documentElement.classList.remove('app-dark'); delete document.documentElement.dataset['theme']; });
  it('restores the saved theme before any route renders and keeps PrimeNG in sync', () => {
    localStorage.setItem('logatron.theme', 'light');
    const theme = TestBed.inject(ThemeService);
    expect(theme.mode()).toBe('light');
    expect(document.documentElement.classList.contains('app-dark')).toBe(false);
    theme.toggle();
    expect(document.documentElement.dataset['theme']).toBe('dark');
    expect(document.documentElement.classList.contains('app-dark')).toBe(true);
    expect(localStorage.getItem('logatron.theme')).toBe('dark');
    theme.toggle();
    expect(document.documentElement.style.colorScheme).toBe('light');
  });
});
