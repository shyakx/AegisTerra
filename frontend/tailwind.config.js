/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#0F5132',
        secondary: '#1B7F5C',
        accent: '#2F9E44',
        success: '#2E7D32',
        warning: '#F59E0B',
        danger: '#DC2626',
        info: '#2563EB',
        background: '#F8FAFC',
        surface: '#FFFFFF',
        sidebar: '#0B2E22',
        textPrimary: '#111827',
        textSecondary: '#6B7280',
        border: '#E5E7EB'
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif']
      }
    }
  },
  plugins: []
};
